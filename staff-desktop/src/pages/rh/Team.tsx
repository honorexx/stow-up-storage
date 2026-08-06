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
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    role: 'SUPPORT' as StaffRole,
    additionalRoles: [] as StaffRole[],
  });
  const [newPassword, setNewPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const staff = useResource((token) => api.get<StaffView[]>('/admin/staff', token));

  function toggleFormAdditionalRole(role: StaffRole) {
    setForm((current) => ({
      ...current,
      additionalRoles: current.additionalRoles.includes(role)
        ? current.additionalRoles.filter((value) => value !== role)
        : [...current.additionalRoles, role],
    }));
  }

  async function createStaff() {
    if (!session) return;
    setBusy(true);
    setActionError(null);
    try {
      await api.post('/admin/staff', form, session.token);
      setCreating(false);
      setForm({ name: '', email: '', password: '', role: 'SUPPORT', additionalRoles: [] });
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
        {
          name: member.name,
          role: member.role,
          additionalRoles: member.additionalRoles,
          enabled: !member.enabled,
        },
        session.token,
      );
      staff.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível atualizar o funcionário.');
    }
  }

  const [editingMember, setEditingMember] = useState<StaffView | null>(null);
  const [editForm, setEditForm] = useState({
    role: 'SUPPORT' as StaffRole,
    additionalRoles: [] as StaffRole[],
  });

  function startEditingAreas(member: StaffView) {
    setEditingMember(member);
    setEditForm({ role: member.role, additionalRoles: member.additionalRoles });
  }

  function toggleEditAdditionalRole(role: StaffRole) {
    setEditForm((current) => ({
      ...current,
      additionalRoles: current.additionalRoles.includes(role)
        ? current.additionalRoles.filter((value) => value !== role)
        : [...current.additionalRoles, role],
    }));
  }

  async function saveAreas() {
    if (!session || !editingMember) return;
    setBusy(true);
    setActionError(null);
    try {
      await api.patch(
        `/admin/staff/${editingMember.id}`,
        {
          name: editingMember.name,
          role: editForm.role,
          additionalRoles: editForm.additionalRoles,
          enabled: editingMember.enabled,
        },
        session.token,
      );
      setEditingMember(null);
      staff.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível atualizar as áreas.');
    } finally {
      setBusy(false);
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
                <th>Áreas</th>
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
                  <td>
                    <div className="role-tags">
                      {[member.role, ...member.additionalRoles].map((role) => (
                        <span key={role} className="role-tag">
                          {ASSIGNABLE_ROLES.find((option) => option.value === role)?.label ?? role}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td>
                    <StatusBadge
                      status={member.enabled ? 'ATIVO' : 'INATIVO'}
                      tone={member.enabled ? 'ok' : 'bad'}
                    />
                  </td>
                  <td className="table-actions">
                    <button type="button" className="btn btn-ghost" onClick={() => startEditingAreas(member)}>
                      Editar áreas
                    </button>
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
            Função principal
            <select
              value={form.role}
              onChange={(event) =>
                setForm({
                  ...form,
                  role: event.target.value as StaffRole,
                  additionalRoles: form.additionalRoles.filter((role) => role !== event.target.value),
                })
              }
            >
              {ASSIGNABLE_ROLES.map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Áreas adicionais (opcional)
            <div className="checkbox-group">
              {ASSIGNABLE_ROLES.filter((role) => role.value !== form.role).map((role) => (
                <label key={role.value} className="checkbox-item">
                  <input
                    type="checkbox"
                    checked={form.additionalRoles.includes(role.value)}
                    onChange={() => toggleFormAdditionalRole(role.value)}
                  />
                  {role.label}
                </label>
              ))}
            </div>
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

      {editingMember && (
        <Modal title={`Áreas de ${editingMember.name}`} onClose={() => setEditingMember(null)}>
          <label>
            Função principal
            <select
              value={editForm.role}
              onChange={(event) =>
                setEditForm({
                  role: event.target.value as StaffRole,
                  additionalRoles: editForm.additionalRoles.filter((role) => role !== event.target.value),
                })
              }
            >
              {ASSIGNABLE_ROLES.map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Áreas adicionais
            <div className="checkbox-group">
              {ASSIGNABLE_ROLES.filter((role) => role.value !== editForm.role).map((role) => (
                <label key={role.value} className="checkbox-item">
                  <input
                    type="checkbox"
                    checked={editForm.additionalRoles.includes(role.value)}
                    onChange={() => toggleEditAdditionalRole(role.value)}
                  />
                  {role.label}
                </label>
              ))}
            </div>
          </label>
          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setEditingMember(null)}>
              Cancelar
            </button>
            <button type="button" className="btn btn-primary" disabled={busy} onClick={saveAreas}>
              Salvar áreas
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
