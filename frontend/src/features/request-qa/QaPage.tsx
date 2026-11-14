import { useState } from 'react';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { useCurrentUser } from '../../app/AuthContext';
import { Alert, Button, Card, EmptyState, Modal, NumberField, PageHeader, TextAreaField, AsyncView } from '../../components';
import { formatDateTime } from '../../utils/format';
import { qaApi, type Question } from './api';

function AnswerModal({ q, onClose, onDone }: { q?: Question; onClose: () => void; onDone: () => void }) {
  const [text, setText] = useState('');
  const [err, setErr] = useState<string | undefined>();
  const { run, busy } = useAction();
  const submit = async () => {
    if (!q) return;
    if (!text.trim()) { setErr('Nhập nội dung trả lời'); return; }
    try { await run(() => qaApi.answer(q.id, text.trim()), 'Đã gửi trả lời'); setText(''); setErr(undefined); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={!!q} title={`Trả lời · ${q?.requestNumber ?? ''}`} icon="bi-chat-left-text" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Gửi trả lời</Button></>}>
      <div className="ir-stack">
        <Alert tone="info" title={q?.askedByName}>{q?.question}</Alert>
        <TextAreaField label="Trả lời" required value={text} onChange={setText} error={err} maxLength={2000} rows={4} />
      </div>
    </Modal>
  );
}

function AskModal({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: () => void }) {
  const [requestId, setRequestId] = useState<number | undefined>();
  const [text, setText] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  const submit = async () => {
    const e = { id: requestId ? undefined : 'Nhập ID yêu cầu', text: text.trim() ? undefined : 'Nhập câu hỏi' };
    setErr(e);
    if (e.id || e.text) return;
    try { await run(() => qaApi.ask(requestId!, text.trim()), 'Đã gửi câu hỏi'); setText(''); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Đặt câu hỏi" icon="bi-question-circle" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Gửi câu hỏi</Button></>}>
      <div className="ir-stack">
        <NumberField label="ID yêu cầu" required value={requestId} onChange={setRequestId} error={err.id} help="Số ID trong đường dẫn /requests/ID" />
        <TextAreaField label="Câu hỏi" required value={text} onChange={setText} error={err.text} maxLength={1000} rows={4} />
      </div>
    </Modal>
  );
}

/** Hộp thư hỏi đáp trên yêu cầu: câu hỏi đang chờ người dùng trả lời. */
export function QaPage() {
  const user = useCurrentUser();
  const state = useAsync(() => qaApi.inbox(), []);
  const [answering, setAnswering] = useState<Question | undefined>();
  const [asking, setAsking] = useState(false);
  return (
    <>
      <PageHeader title="Hỏi đáp yêu cầu" subtitle="Câu hỏi đang chờ bạn trả lời."
        actions={<Button icon="bi-plus-lg" onClick={() => setAsking(true)}>{user.role === 'CUSTOMER' ? 'Hỏi nhân viên' : 'Hỏi khách hàng'}</Button>} />
      <AsyncView state={state} isEmpty={(d) => d.length === 0} empty={<EmptyState title="Không có câu hỏi nào chờ trả lời" icon="bi-chat-left-text" />}>
        {(rows) => (
          <div className="ir-stack">
            {rows.map((q) => (
              <Card key={q.id} title={q.requestNumber} subtitle={`${q.askedByName} · ${formatDateTime(q.createdAt)}`}
                actions={q.canAnswer ? <Button size="sm" onClick={() => setAnswering(q)}>Trả lời</Button> : undefined}>
                {q.question}
              </Card>
            ))}
          </div>
        )}
      </AsyncView>
      <AnswerModal q={answering} onClose={() => setAnswering(undefined)} onDone={state.reload} />
      <AskModal open={asking} onClose={() => setAsking(false)} onDone={state.reload} />
    </>
  );
}
