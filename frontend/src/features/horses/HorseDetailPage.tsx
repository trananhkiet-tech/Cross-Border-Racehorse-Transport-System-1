import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { openFile } from '../../api/client';
import { documentApi, horseApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, AsyncView, Button, Card, ConfirmDialog, DataTable, DescriptionList, LinkButton, PageHeader, StatusBadge, Tabs } from '../../components';
import { DocumentTypeLabels, HorseConditionLabels, HorseSexLabels, HorseStatusLabels, IntakeLevelLabels, StressLevelLabels } from '../../types/enums';
import type { DocumentItem, Horse } from '../../types/api';
import { countryName, formatDate, formatDateTime } from '../../utils/format';

function HealthHistory({ horseId }: { horseId: number }) {
  const state = useAsync(() => horseApi.healthHistory(horseId), [horseId]);
  return (
    <AsyncView state={state}>
      {(h) => (
        <div className="ir-stack">
          <Alert tone="info">{h.disclaimer}</Alert>
          <Card title="Kiểm tra trước vận chuyển" flush>
            <DataTable dense flush rows={h.preTransportChecks} rowKey={(x) => x.id} emptyTitle="Chưa có lần kiểm tra"
              columns={[
                { key: 't', header: 'Thời gian', render: (x) => formatDateTime(x.checkedAt) },
                { key: 'c', header: 'Tình trạng chung', render: (x) => HorseConditionLabels[x.generalCondition] },
                { key: 'tmp', header: 'Nhiệt độ', render: (x) => (x.temperatureC ? `${x.temperatureC} °C` : '—') },
                { key: 'f', header: 'Đủ điều kiện (quan sát)', render: (x) => (x.fitForTravelObserved ? 'Có' : 'Không') },
                { key: 'b', header: 'Người kiểm tra', render: (x) => x.checkedBy },
              ]} />
          </Card>
          <Card title="Quan sát trong hành trình" flush>
            <DataTable dense flush rows={h.observations} rowKey={(x) => x.id} emptyTitle="Chưa có quan sát"
              columns={[
                { key: 't', header: 'Thời gian', render: (x) => formatDateTime(x.observedAt) },
                { key: 'trip', header: 'Chuyến', render: (x) => <span className="ir-mono">{x.tripNumber}</span> },
                { key: 'a', header: 'Ăn / uống', render: (x) => `${IntakeLevelLabels[x.appetite]} / ${IntakeLevelLabels[x.waterIntake]}` },
                { key: 's', header: 'Căng thẳng', render: (x) => StressLevelLabels[x.stressLevel] },
                { key: 'sev', header: 'Mức độ', render: (x) => <StatusBadge status={x.severity} /> },
              ]} />
          </Card>
          <Card title="Kiểm tra khi đến nơi" flush>
            <DataTable dense flush rows={h.arrivalChecks} rowKey={(x) => x.id} emptyTitle="Chưa có"
              columns={[
                { key: 't', header: 'Thời gian', render: (x) => formatDateTime(x.checkedAt) },
                { key: 'c', header: 'Tình trạng', render: (x) => HorseConditionLabels[x.condition] },
                { key: 'i', header: 'Thương tích', render: (x) => (x.visibleInjury ? 'Có' : 'Không') },
                { key: 'n', header: 'Ghi chú', render: (x) => x.notes ?? '—' },
              ]} />
          </Card>
          <Card title="Tư vấn thú y" flush>
            <DataTable dense flush rows={h.consultations} rowKey={(x) => x.id} emptyTitle="Chưa có"
              columns={[
                { key: 't', header: 'Thời gian', render: (x) => formatDateTime(x.consultedAt) },
                { key: 'v', header: 'Bác sĩ thú y', render: (x) => `${x.veterinarianName}${x.clinic ? ` – ${x.clinic}` : ''}` },
                { key: 'a', header: 'Khuyến nghị', render: (x) => x.advice },
              ]} />
          </Card>
        </div>
      )}
    </AsyncView>
  );
}

function HorseDocuments({ horseId }: { horseId: number }) {
  const navigate = useNavigate();
  const state = useAsync(() => horseApi.documents(horseId), [horseId]);
  return (
    <DataTable<DocumentItem>
      rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(d) => d.id}
      onRowClick={(d) => navigate(`/documents/${d.id}`)} emptyTitle="Chưa có tài liệu gắn với ngựa này" emptyIcon="bi-file-earmark"
      columns={[
        { key: 'type', header: 'Loại tài liệu', render: (d) => DocumentTypeLabels[d.documentType] },
        { key: 'file', header: 'Tệp', render: (d) => <button type="button" className="ir-link" style={{ border: 0, background: 'none', padding: 0 }} onClick={(e) => { e.stopPropagation(); void openFile(documentApi.filePath(d.id)); }}>{d.fileName}</button> },
        { key: 'req', header: 'Yêu cầu', render: (d) => d.requestNumber ?? '—' },
        { key: 'exp', header: 'Hết hạn', render: (d) => formatDate(d.expiryDate) },
        { key: 'st', header: 'Trạng thái', render: (d) => <StatusBadge status={d.reviewStatus} /> },
      ]}
    />
  );
}

export function HorseDetailPage() {
  const id = Number(useParams().id);
  const user = useCurrentUser();
  const navigate = useNavigate();
  const state = useAsync(() => horseApi.get(id), [id]);
  const [tab, setTab] = useState<'info' | 'docs' | 'health'>('info');
  const [archiveOpen, setArchiveOpen] = useState(false);
  const { run } = useAction();
  const isOwner = (h: Horse) => user.role === 'CUSTOMER' && h.ownerId === user.id;

  return (
    <AsyncView state={state}>
      {(h) => (
        <>
          <PageHeader
            title={h.registeredName}
            back={{ to: '/horses', label: 'Danh sách ngựa' }}
            meta={<StatusBadge status={h.status} labels={HorseStatusLabels} />}
            subtitle={`Microchip ${h.microchipNumber} · Hộ chiếu ${h.passportNumber}`}
            actions={isOwner(h) && h.status !== 'ARCHIVED' && (
              <>
                <Button variant="danger-outline" icon="bi-archive" onClick={() => setArchiveOpen(true)}>Lưu trữ</Button>
                <LinkButton to={`/horses/${h.id}/edit`} variant="outline" icon="bi-pencil">Chỉnh sửa</LinkButton>
                <LinkButton to={`/requests/new?horseId=${h.id}`} icon="bi-plus-circle">Tạo yêu cầu</LinkButton>
              </>
            )}
          />
          <Tabs tabs={[{ key: 'info', label: 'Thông tin' }, { key: 'docs', label: 'Tài liệu' }, { key: 'health', label: 'Lịch sử sức khỏe vận chuyển' }]} active={tab} onChange={setTab} />
          {tab === 'info' && (
            <div className="app-grid app-grid-main">
              <Card title="Hồ sơ ngựa" icon="bi-postcard-heart">
                <DescriptionList items={[
                  { label: 'Tên đăng ký', value: h.registeredName },
                  { label: 'Giống', value: h.breed },
                  { label: 'Ngày sinh', value: h.dateOfBirth ? `${formatDate(h.dateOfBirth)} (${h.ageYears ?? '?'} tuổi)` : undefined },
                  { label: 'Giới tính', value: h.sex && HorseSexLabels[h.sex] },
                  { label: 'Màu lông', value: h.color },
                  { label: 'Chiều cao', value: h.heightCm ? `${h.heightCm} cm` : undefined },
                  { label: 'Microchip', value: <span className="ir-mono">{h.microchipNumber}</span> },
                  { label: 'Hộ chiếu', value: `${h.passportNumber} (${countryName(h.passportCountry)})` },
                  { label: 'Yêu cầu đặc biệt', value: h.specialRequirements, span: true },
                  { label: 'Ghi chú', value: h.notes, span: true },
                ]} />
              </Card>
              <Card title="Chủ sở hữu" icon="bi-person">
                <DescriptionList columns={1} items={[
                  { label: 'Khách hàng', value: h.ownerName },
                  { label: 'Tạo lúc', value: formatDateTime(h.createdAt) },
                  { label: 'Cập nhật', value: formatDateTime(h.updatedAt) },
                ]} />
              </Card>
            </div>
          )}
          {tab === 'docs' && <HorseDocuments horseId={h.id} />}
          {tab === 'health' && <HealthHistory horseId={h.id} />}
          <ConfirmDialog
            open={archiveOpen}
            title="Lưu trữ hồ sơ ngựa?"
            message="Ngựa đã lưu trữ sẽ không thể chọn cho yêu cầu mới. Lịch sử vận chuyển vẫn được giữ nguyên."
            tone="error"
            confirmLabel="Lưu trữ"
            onClose={() => setArchiveOpen(false)}
            onConfirm={async () => {
              await run(() => horseApi.archive(h.id), 'Đã lưu trữ hồ sơ ngựa');
              navigate('/horses');
            }}
          />
        </>
      )}
    </AsyncView>
  );
}
