import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { complianceApi, requestApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import {
  Alert, AsyncView, Badge, Button, Card, ConfirmDialog, DataTable, DescriptionList, LinkButton, PageHeader, Progress,
  Stepper, StatusBadge, Tabs, Timeline,
} from '../../components';
import { refreshCounts } from '../../layouts/AppLayout';
import { MovementTypeLabels, RequestStatusLabels, TransportModeLabels, TransportPurposeLabels } from '../../types/enums';
import type { RequestDetail } from '../../types/api';
import { countryName, formatDate, formatDateTime, formatMoney } from '../../utils/format';
import { ChecklistPanel } from '../compliance/ChecklistPanel';
import { DocumentsTable } from '../documents/DocumentsTable';
import { Money, RefLink } from '../common/bits';
import { requestSteps } from './requestStages';

type DialogKey = 'cancel' | 'reject' | 'info' | 'approve' | 'docReview' | 'compliance' | 'submit' | null;

function Actions({ r, reload }: { r: RequestDetail; reload: () => void }) {
  const [dlg, setDlg] = useState<DialogKey>(null);
  const { run } = useAction();
  const navigate = useNavigate();
  const has = (a: string) => r.allowedActions.includes(a);
  const done = (msg: string, fn: () => Promise<unknown>) => async (reason?: string) => {
    void reason;
    await run(fn, msg);
    refreshCounts();
    reload();
  };

  return (
    <>
      {has('EDIT') && <LinkButton to={`/requests/${r.id}/edit`} variant="outline" icon="bi-pencil">Chỉnh sửa</LinkButton>}
      {has('SUBMIT') && <Button icon="bi-send" onClick={() => setDlg('submit')}>Gửi yêu cầu</Button>}
      {has('START_REVIEW') && <Button icon="bi-play-circle" onClick={async () => { await run(() => requestApi.startReview(r.id), 'Đã bắt đầu rà soát hồ sơ'); refreshCounts(); reload(); }}>Bắt đầu rà soát</Button>}
      {has('COMPLETE_DOCUMENT_REVIEW') && <Button icon="bi-check2-square" onClick={() => setDlg('docReview')}>Hoàn tất rà soát tài liệu</Button>}
      {has('COMPLETE_COMPLIANCE') && <Button icon="bi-shield-check" onClick={() => setDlg('compliance')}>Hoàn tất tuân thủ</Button>}
      {has('REQUEST_INFORMATION') && <Button variant="outline" icon="bi-question-circle" onClick={() => setDlg('info')}>Yêu cầu bổ sung</Button>}
      {has('APPROVE') && <Button variant="success" icon="bi-check-circle" onClick={() => setDlg('approve')}>Phê duyệt</Button>}
      {has('REJECT') && <Button variant="danger-outline" icon="bi-x-circle" onClick={() => setDlg('reject')}>Từ chối</Button>}
      {has('BUILD_QUOTE') && <LinkButton to={`/quotes?requestId=${r.id}`} icon="bi-receipt">{r.latestQuote ? 'Quản lý báo giá' : 'Lập báo giá'}</LinkButton>}
      {has('PLAN_TRIP') && <LinkButton to={`/planning?requestId=${r.id}`} icon="bi-calendar2-plus">Lập chuyến</LinkButton>}
      {has('RESPOND_QUOTE') && r.latestQuote && <LinkButton to={`/quotes/${r.latestQuote.id}`} icon="bi-receipt">Xem & phản hồi báo giá</LinkButton>}
      {has('CREATE_CLAIM') && <LinkButton to={`/claims/new?requestId=${r.id}`} variant="outline" icon="bi-shield-exclamation">Tạo hồ sơ bồi thường</LinkButton>}
      {has('CANCEL') && <Button variant="danger-outline" icon="bi-slash-circle" onClick={() => setDlg('cancel')}>Hủy yêu cầu</Button>}

      <ConfirmDialog open={dlg === 'submit'} onClose={() => setDlg(null)} title="Gửi yêu cầu vận chuyển?" confirmLabel="Gửi yêu cầu"
        message="Sau khi gửi, hệ thống tạo checklist hồ sơ và chi phí ước tính. Bạn vẫn có thể tải thêm tài liệu."
        onConfirm={done('Đã gửi yêu cầu', () => requestApi.submit(r.id))} />
      <ConfirmDialog open={dlg === 'docReview'} onClose={() => setDlg(null)} title="Hoàn tất rà soát tài liệu?" allowNote reasonLabel="Ghi chú (không bắt buộc)"
        message="Yêu cầu chuyển sang bước kiểm tra tuân thủ." confirmLabel="Hoàn tất"
        onConfirm={(n) => done('Đã hoàn tất rà soát tài liệu', () => requestApi.completeDocumentReview(r.id, n || undefined))()} />
      <ConfirmDialog open={dlg === 'compliance'} onClose={() => setDlg(null)} title="Hoàn tất kiểm tra tuân thủ?" allowNote reasonLabel="Ghi chú cho Quản lý (không bắt buộc)"
        message="Yêu cầu sẽ chuyển cho Quản lý Logistics phê duyệt." confirmLabel="Chuyển phê duyệt"
        onConfirm={(n) => done('Đã chuyển yêu cầu chờ phê duyệt', () => requestApi.completeCompliance(r.id, n || undefined))()} />
      <ConfirmDialog open={dlg === 'info'} onClose={() => setDlg(null)} title="Yêu cầu khách hàng bổ sung thông tin" requireReason reasonLabel="Nội dung cần bổ sung"
        message="Khách hàng nhận thông báo và có thể chỉnh sửa yêu cầu." confirmLabel="Gửi yêu cầu bổ sung"
        onConfirm={(n) => done('Đã gửi yêu cầu bổ sung', () => requestApi.requestInformation(r.id, n))()} />
      <ConfirmDialog open={dlg === 'approve'} onClose={() => setDlg(null)} title="Phê duyệt yêu cầu?" tone="success" allowNote reasonLabel="Ghi chú phê duyệt (không bắt buộc)"
        message="Sau khi duyệt, bạn có thể lập báo giá cuối cho khách hàng." confirmLabel="Phê duyệt"
        onConfirm={(n) => done('Đã phê duyệt yêu cầu', () => requestApi.approve(r.id, n || undefined))()} />
      <ConfirmDialog open={dlg === 'reject'} onClose={() => setDlg(null)} title="Từ chối yêu cầu?" tone="error" requireReason reasonLabel="Lý do từ chối"
        message="Khách hàng sẽ nhận được lý do từ chối." confirmLabel="Từ chối"
        onConfirm={(n) => done('Đã từ chối yêu cầu', () => requestApi.reject(r.id, n))()} />
      <ConfirmDialog open={dlg === 'cancel'} onClose={() => setDlg(null)} title="Hủy yêu cầu?" tone="error" allowNote reasonLabel="Lý do hủy (không bắt buộc)"
        message="Yêu cầu đã hủy không thể khôi phục." confirmLabel="Hủy yêu cầu"
        onConfirm={async (n) => { await run(() => requestApi.cancel(r.id, n || undefined), 'Đã hủy yêu cầu'); refreshCounts(); navigate('/requests'); }} />
    </>
  );
}

type TabKey = 'overview' | 'compliance' | 'documents' | 'history';

export function RequestDetailPage() {
  const id = Number(useParams().id);
  const user = useCurrentUser();
  const state = useAsync(() => requestApi.get(id), [id]);
  const [tab, setTab] = useState<TabKey>('overview');
  const checklist = useAsync(() => complianceApi.checklist(id), [id, state.data?.status], tab === 'compliance');
  const docs = useAsync(() => requestApi.documents(id), [id], tab === 'documents');

  return (
    <AsyncView state={state}>
      {(r) => (
        <>
          <PageHeader
            title={r.requestNumber}
            back={{ to: '/requests', label: 'Danh sách yêu cầu' }}
            meta={<StatusBadge status={r.status} labels={RequestStatusLabels} size="md" />}
            subtitle={`${countryName(r.originCountry)} → ${countryName(r.destinationCountry)} · khởi hành ${formatDate(r.requestedDepartureDate)}`}
            actions={<Actions r={r} reload={state.reload} />}
          />
          <Card><Stepper steps={requestSteps(r.status)} /></Card>
          {r.status === 'INFORMATION_REQUIRED' && r.lastDecisionNote && (
            <Alert tone="warning" title="Cần bổ sung thông tin">{r.lastDecisionNote}</Alert>
          )}
          {(r.status === 'REJECTED' || r.status === 'QUOTE_REJECTED') && r.lastDecisionNote && (
            <Alert tone="error" title={r.status === 'REJECTED' ? 'Yêu cầu bị từ chối' : 'Báo giá bị từ chối'}>{r.lastDecisionNote}</Alert>
          )}
          <Tabs<TabKey>
            tabs={[
              { key: 'overview', label: 'Tổng quan' },
              { key: 'compliance', label: 'Tuân thủ', count: r.compliance.total ? r.compliance.total - r.compliance.approved : undefined, alert: r.compliance.problems > 0 },
              { key: 'documents', label: 'Tài liệu' },
              { key: 'history', label: 'Lịch sử trạng thái', count: r.history.length },
            ]}
            active={tab}
            onChange={setTab}
          />

          {tab === 'overview' && (
            <div className="app-grid app-grid-main">
              <div className="ir-stack">
                <Card title="Thông tin vận chuyển" icon="bi-geo-alt">
                  <DescriptionList items={[
                    { label: 'Điểm đi', value: `${r.originAddress}, ${countryName(r.originCountry)}` },
                    { label: 'Điểm đến', value: `${r.destinationAddress}, ${countryName(r.destinationCountry)}` },
                    { label: 'Ngày khởi hành mong muốn', value: formatDate(r.requestedDepartureDate) },
                    { label: 'Ngày quay về', value: r.movementType === 'TEMPORARY' ? formatDate(r.returnDate) : 'Không áp dụng (chuyển vĩnh viễn)' },
                    { label: 'Loại di chuyển', value: MovementTypeLabels[r.movementType] },
                    { label: 'Mục đích', value: TransportPurposeLabels[r.purpose] },
                    { label: 'Phương thức', value: TransportModeLabels[r.transportMode] },
                    { label: 'Giá trị khai báo (tham chiếu)', value: <Money value={r.declaredValueTotal} currency={r.currency} /> },
                    { label: 'Yêu cầu đặc biệt', value: r.specialRequirements, span: true },
                    { label: 'Ghi chú', value: r.notes, span: true },
                  ]} />
                </Card>
                <Card title={`Ngựa (${r.horses.length})`} icon="bi-postcard-heart" flush>
                  <DataTable flush dense rows={r.horses} rowKey={(h) => h.id}
                    columns={[
                      { key: 'n', header: 'Tên ngựa', render: (h) => <RefLink to={`/horses/${h.horseId}`}>{h.horseName}</RefLink> },
                      { key: 'c', header: 'Microchip', render: (h) => <span className="ir-mono">{h.microchipNumber}</span> },
                      { key: 's', header: 'Chuồng', render: (h) => `${h.stallTypeName} (${h.stallWidthCm} cm)` },
                      { key: 'v', header: 'Giá trị khai báo', align: 'right', render: (h) => <Money value={h.declaredValue} currency={r.currency} /> },
                    ]} />
                </Card>
              </div>
              <div className="ir-stack">
                <Card title="Khách hàng" icon="bi-person">
                  <DescriptionList columns={1} items={[
                    { label: 'Tên', value: r.customerName },
                    { label: 'Email', value: r.customerEmail },
                    { label: 'Điện thoại', value: r.customerPhone },
                  ]} />
                </Card>
                <Card title="Tuân thủ hồ sơ" icon="bi-shield-check">
                  <Progress value={r.compliance.approved} max={Math.max(1, r.compliance.total)} tone={r.compliance.complete ? 'success' : 'primary'} label={`${r.compliance.approved}/${r.compliance.total} mục đã duyệt`} />
                  <div className="app-row" style={{ marginTop: 12 }}>
                    {r.compliance.missing > 0 && <Badge tone="warning">Thiếu {r.compliance.missing}</Badge>}
                    {r.compliance.pending > 0 && <Badge tone="info">Chờ duyệt {r.compliance.pending}</Badge>}
                    {r.compliance.problems > 0 && <Badge tone="error">Có vấn đề {r.compliance.problems}</Badge>}
                    <Button size="sm" variant="ghost" onClick={() => setTab('compliance')}>Mở checklist</Button>
                  </div>
                </Card>
                <Card title="Chi phí" icon="bi-receipt">
                  <div className="ir-stack" style={{ gap: 12 }}>
                    {r.estimate && (
                      <div>
                        <div className="app-text-xs ir-muted">Ước tính (tham khảo)</div>
                        <div className="ir-strong"><Money value={r.estimate.totalAmount} currency={r.estimate.currency} /></div>
                        <RefLink to={`/quotes/${r.estimate.id}`}>{r.estimate.quoteNumber}</RefLink>
                      </div>
                    )}
                    {r.latestQuote && (
                      <div>
                        <div className="app-text-xs ir-muted">Báo giá cuối (phiên bản {r.latestQuote.versionNo})</div>
                        <div className="ir-strong"><Money value={r.latestQuote.totalAmount} currency={r.latestQuote.currency} /></div>
                        <div className="app-row"><RefLink to={`/quotes/${r.latestQuote.id}`}>{r.latestQuote.quoteNumber}</RefLink><StatusBadge status={r.latestQuote.status} /></div>
                      </div>
                    )}
                    {r.invoiceId && <LinkButton to={`/invoices/${r.invoiceId}`} variant="outline" size="sm" icon="bi-credit-card">Hoá đơn & thanh toán</LinkButton>}
                    {!r.estimate && !r.latestQuote && <span className="ir-muted app-text-sm">Chi phí ước tính sẽ có sau khi gửi yêu cầu.</span>}
                  </div>
                </Card>
                {r.trips.length > 0 && (
                  <Card title="Chuyến đi" icon="bi-truck">
                    <ul style={{ listStyle: 'none', padding: 0, margin: 0 }} className="ir-stack">
                      {r.trips.map((t) => (
                        <li key={t.id} className="app-row">
                          <RefLink to={`/trips/${t.id}`}>{t.tripNumber}</RefLink>
                          <StatusBadge status={t.status} />
                          <span className="ir-muted app-text-xs">{formatDateTime(t.plannedDeparture)}</span>
                        </li>
                      ))}
                    </ul>
                  </Card>
                )}
              </div>
            </div>
          )}

          {tab === 'compliance' && (
            <Card>
              <AsyncView state={checklist}>
                {(c) => <ChecklistPanel checklist={c} canUpload={user.role === 'CUSTOMER' || user.role === 'TRANSPORT_SPECIALIST'} onChanged={() => { checklist.reload(); state.reload(); }} />}
              </AsyncView>
            </Card>
          )}

          {tab === 'documents' && (
            <DocumentsTable state={docs} showRequest={false} />
          )}

          {tab === 'history' && (
            <Card>
              <Timeline items={[...r.history].reverse().map((h, i) => ({
                key: i,
                title: RequestStatusLabels[h.toStatus],
                time: h.changedAt,
                description: h.note,
                meta: `${h.changedBy}`,
                state: h.toStatus === 'REJECTED' || h.toStatus === 'CANCELLED' || h.toStatus === 'QUOTE_REJECTED' ? 'error' : i === 0 ? 'current' : 'done',
              }))} />
            </Card>
          )}
          <p className="app-text-xs ir-muted" style={{ margin: 0 }}>Tổng cộng giá trị khai báo chỉ để tham chiếu bảo hiểm/hải quan: {formatMoney(r.declaredValueTotal, r.currency)}. Giá vận chuyển do báo giá quyết định.</p>
        </>
      )}
    </AsyncView>
  );
}
