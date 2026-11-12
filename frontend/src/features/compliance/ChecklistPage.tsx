import { useParams } from 'react-router-dom';
import { complianceApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { AsyncView, LinkButton, PageHeader, StatusBadge, Card } from '../../components';
import { RequestStatusLabels } from '../../types/enums';
import { formatDate } from '../../utils/format';
import { ChecklistPanel } from './ChecklistPanel';

export function ChecklistPage() {
  const id = Number(useParams().id);
  const user = useCurrentUser();
  const state = useAsync(() => complianceApi.checklist(id), [id]);
  return (
    <AsyncView state={state}>
      {(c) => (
        <>
          <PageHeader
            title={`Checklist tuân thủ – ${c.requestNumber}`}
            back={{ to: `/requests/${id}`, label: 'Chi tiết yêu cầu' }}
            subtitle={`${c.route} · khởi hành ${formatDate(c.requestedDepartureDate)}`}
            meta={<StatusBadge status={c.requestStatus} labels={RequestStatusLabels} />}
            actions={<LinkButton to={`/requests/${id}`} variant="outline" icon="bi-clipboard-check">Chi tiết yêu cầu</LinkButton>}
          />
          <Card>
            <ChecklistPanel checklist={c} canUpload={user.role === 'CUSTOMER' || user.role === 'TRANSPORT_SPECIALIST'} onChanged={state.reload} />
          </Card>
        </>
      )}
    </AsyncView>
  );
}
