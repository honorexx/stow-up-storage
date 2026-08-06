import { api } from '../../api/client';
import type { AuditEntry } from '../../api/types';
import { Card, EmptyState, ErrorBanner, LoadingBlock, PageHeader } from '../../components/ui';
import { useResource } from '../../hooks/useResource';

const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export function Audit() {
  const { data, loading, error } = useResource((token) => api.get<AuditEntry[]>('/admin/audit', token));

  return (
    <div>
      <PageHeader title="Auditoria" subtitle="Últimas 200 ações relevantes registradas na plataforma." />
      {loading && <LoadingBlock />}
      {error && <ErrorBanner message={error} />}
      {data && data.length === 0 && <EmptyState message="Nenhuma ação registrada ainda." />}
      {data && data.length > 0 && (
        <Card>
          <table className="table">
            <thead>
              <tr>
                <th>Quando</th>
                <th>Quem</th>
                <th>Ação</th>
                <th>Entidade</th>
                <th>Detalhes</th>
              </tr>
            </thead>
            <tbody>
              {data.map((entry) => (
                <tr key={entry.id}>
                  <td>{dateFormat.format(new Date(entry.createdAt))}</td>
                  <td>{entry.actor}</td>
                  <td>{entry.action}</td>
                  <td>
                    {entry.entityType} #{entry.entityId}
                  </td>
                  <td className="muted">{entry.details}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  );
}
