import { useState } from 'react';
import { api, ApiError } from '../../api/client';
import type { StaffRole, StaffView } from '../../api/types';
import { Modal } from '../../components/Modal';
import { Card, EmptyState, ErrorBanner, LoadingBlock, PageHeader, StatusBadge } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { useAuth } from '../../state/AuthContext';

const ASSIGNABLE_ROLES: { value: StaffRole; label: string }[] = [
  { value: 'SUPPORT', label: 'Atendimento' },
  { value: 'MODERATOR', label: 'Operações · Moderador' },
  { value: 'LISTING_ANALYST', label: 'Operações · Analista de anúncios' },
  { value: 'DOCUMENT_ANALYST', label: 'Jurídico · Analista de documentos' },
  { value: 'CONTRACT_ANALYST', label: 'Jurídico · Analista de contratos' },
];

export function Team() {
  const { session } = useAuth();
  const [creating, setCreating] = useState(false);
  const [resettingId, setResettingId] = useState<number | null>(null);
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'SUPPORT' as StaffRole });
  const [newPassword, setNewPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const staff = useResource((token) => api.get<StaffView[]>('/admin/staff', token));

  async function createStaff() {
    if (!session) return;
    setBusy(true);
    setActionError(null);
    try {
      await api.post('/admin/staff', form, session.token);
      setCreating(false);
      setForm({ name: '', email: '', password: '', role: 'SUPPORT' });
      staff.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível cadastrar o funcionário.');
    } finally {
      setBusy(false);
    }
  }

  async function toggleEnabled(member: StaffView) {
    if (!session) return;
    try {
      await api.patch(
        `/admin/staff/${member.id}`,
        { name: member.name, role: member.role, enabled: !member.enabled },
        session.token,
      );
      staff.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível atualizar o funcionário.');
    }
  }

  async function resetPassword() {
    if (!session || resettingId === null) return;
    setBusy(true);
    setActionError(null);
    try {
      await api.post(`/admin/staff/${resettingId}/reset-password`, { password: newPassword }, session.token);
      setResettingId(null);
      setNewPassword('');
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível redefinir a senha.');
    } finally {
      setBusy(false);
    }
  }

  async function removeStaff(member: StaffView) {
    if (!session) return;
    if (!window.confirm(`Remover o acesso de ${member.name}?`)) return;
    try {
      await api.delete(`/admin/staff/${member.id}`, session.token);
      staff.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível remover o funcionário.');
    }
  }

  return (
    <div>
      <PageHeader
        title="Equipe StowUp"
        subtitle="Funcionários internos, cada um com um código SU exclusivo e permissões por função."
        actions={
          <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>
            Novo funcionário
          </button>
        }
      />
      {staff.error && <ErrorBanner message={staff.error} />}
      {actionError && <ErrorBanner message={actionError} />}
      {staff.loading && <LoadingBlock />}
      {staff.data && staff.data.length === 0 && <EmptyState message="Nenhum funcionário cadastrado ainda." />}
      {staff.data && staff.data.length > 0 && (
        <Card>
          <table className="table">
            <thead>
              <tr>
                <th>Código</th>
                <th>Nome</th>
                <th>E-mail</th>
                <th>Função</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {staff.data.map((member) => (
                <tr key={member.id}>
                  <td>{member.staffCode}</td>
                  <td>{member.name}</td>
                  <td>{member.email}</td>
                  <td>{ASSIGNABLE_ROLES.find((role) => role.value === member.role)?.label ?? member.role}</td>
                  <td>
                    <StatusBadge
                      status={member.enabled ? 'ATIVO' : 'INATIVO'}
                      tone={member.enabled ? 'ok' : 'bad'}
                    />
                  </td>
                  <td className="table-actions">
                    <button type="button" className="btn btn-ghost" onClick={() => toggleEnabled(member)}>
                      {member.enabled ? 'Desativar' : 'Ativar'}
                    </button>
                    <button type="button" className="btn btn-ghost" onClick={() => setResettingId(member.id)}>
                      Redefinir senha
                    </button>
                    <button type="button" className="btn btn-danger" onClick={() => removeStaff(member)}>
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}

      {creating && (
        <Modal title="Novo funcionário" onClose={() => setCreating(false)}>
          <label>
            Nome completo
            <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          </label>
          <label>
            E-mail corporativo
            <input
              type="email"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
            />
          </label>
          <label>
            Senha temporária
            <input
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              minLength={10}
            />
          </label>
          <label>
            Função
            <select
              value={form.role}
              onChange={(event) => setForm({ ...form, role: event.target.value as StaffRole })}
            >
              {ASSIGNABLE_ROLES.map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </select>
          </label>
          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setCreating(false)}>
              Cancelar
            </button>
            <button
              type="button"
              className="btn btn-primary"
              disabled={busy || !form.name || !form.email || form.password.length < 10}
              onClick={createStaff}
            >
              Cadastrar
            </button>
          </div>
        </Modal>
      )}

      {resettingId !== null && (
        <Modal title="Redefinir senha" onClose={() => setResettingId(null)}>
          <label>
            Nova senha temporária
            <input
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              minLength={10}
            />
          </label>
          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setResettingId(null)}>
              Cancelar
            </button>
            <button
              type="button"
              className="btn btn-primary"
              disabled={busy || newPassword.length < 10}
              onClick={resetPassword}
            >
              Confirmar
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
