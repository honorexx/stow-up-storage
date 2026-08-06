import { useState } from 'react';
import { api, ApiError } from '../../api/client';
import type { AppUserView } from '../../api/types';
import { Card, EmptyState, ErrorBanner, LoadingBlock, PageHeader, StatusBadge } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { useAuth } from '../../state/AuthContext';

export function AppUsers() {
  const { session } = useAuth();
  const [query, setQuery] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);

  const users = useResource((token) => api.get<AppUserView[]>('/admin/users', token));

  async function toggleEnabled(user: AppUserView) {
    if (!session) return;
    try {
      await api.patch(
        `/admin/users/${user.id}`,
        {
          name: user.name,
          phone: user.phone ?? '',
          city: user.city ?? '',
          state: user.state ?? '',
          role: user.role,
          enabled: !user.enabled,
          ownerVerified: user.ownerVerified,
        },
        session.token,
      );
      users.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível atualizar o usuário.');
    }
  }

  const filtered = users.data?.filter(
    (user) =>
      !query.trim() ||
      user.name.toLowerCase().includes(query.toLowerCase()) ||
      user.email.toLowerCase().includes(query.toLowerCase()),
  );

  return (
    <div>
      <PageHeader
        title="Usuários do aplicativo"
        subtitle="Clientes e proprietários cadastrados no app público StowUp."
        actions={
          <input
            placeholder="Buscar por nome ou e-mail"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        }
      />
      {users.error && <ErrorBanner message={users.error} />}
      {actionError && <ErrorBanner message={actionError} />}
      {users.loading && <LoadingBlock />}
      {filtered && filtered.length === 0 && <EmptyState message="Nenhum usuário encontrado." />}
      {filtered && filtered.length > 0 && (
        <Card>
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>E-mail</th>
                <th>Perfil</th>
                <th>Anúncios</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((user) => (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td>
                    {user.role === 'OWNER'
                      ? `Proprietário${user.ownerVerified ? ' · verificado' : ''}`
                      : 'Cliente'}
                  </td>
                  <td>{user.listings}</td>
                  <td>
                    <StatusBadge
                      status={user.enabled ? 'ATIVO' : 'INATIVO'}
                      tone={user.enabled ? 'ok' : 'bad'}
                    />
                  </td>
                  <td className="table-actions">
                    <button type="button" className="btn btn-ghost" onClick={() => toggleEnabled(user)}>
                      {user.enabled ? 'Desativar' : 'Ativar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  );
}
