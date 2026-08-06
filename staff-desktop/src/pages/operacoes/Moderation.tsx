import { useState } from 'react';
import { api, ApiError } from '../../api/client';
import type { ModerationSpaceView } from '../../api/types';
import { Modal } from '../../components/Modal';
import { Card, EmptyState, ErrorBanner, LoadingBlock, PageHeader } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { useAuth } from '../../state/AuthContext';

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function Moderation() {
  const { session } = useAuth();
  const [rejecting, setRejecting] = useState<ModerationSpaceView | null>(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const pending = useResource((token) =>
    api.get<ModerationSpaceView[]>('/staff/moderation/spaces/pending', token),
  );

  async function approve(space: ModerationSpaceView) {
    if (!session) return;
    setBusy(true);
    setActionError(null);
    try {
      await api.post(`/staff/moderation/spaces/${space.id}/approve`, undefined, session.token);
      pending.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível aprovar o anúncio.');
    } finally {
      setBusy(false);
    }
  }

  async function reject() {
    if (!session || !rejecting) return;
    setBusy(true);
    setActionError(null);
    try {
      await api.post(`/staff/moderation/spaces/${rejecting.id}/reject`, { reason }, session.token);
      setRejecting(null);
      setReason('');
      pending.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível rejeitar o anúncio.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Moderação de anúncios"
        subtitle="Espaços cadastrados pelos proprietários, aguardando revisão antes de ficarem públicos."
      />
      {pending.error && <ErrorBanner message={pending.error} />}
      {actionError && <ErrorBanner message={actionError} />}
      {pending.loading && <LoadingBlock />}
      {pending.data && pending.data.length === 0 && (
        <EmptyState message="Nenhum anúncio aguardando moderação." />
      )}
      <div className="card-grid">
        {pending.data?.map((space) => (
          <Card key={space.id}>
            <h3>{space.title}</h3>
            <small className="muted">
              {space.city} · {space.district} · {space.type}
            </small>
            <p>{space.description}</p>
            <strong>{currency.format(space.price)}/mês</strong>
            <small className="muted">
              Anunciante: {space.ownerName} ({space.ownerEmail})
            </small>
            <div className="card-actions">
              <button
                type="button"
                className="btn btn-primary"
                disabled={busy}
                onClick={() => approve(space)}
              >
                Aprovar
              </button>
              <button
                type="button"
                className="btn btn-danger"
                disabled={busy}
                onClick={() => setRejecting(space)}
              >
                Rejeitar
              </button>
            </div>
          </Card>
        ))}
      </div>
      {rejecting && (
        <Modal title={`Rejeitar "${rejecting.title}"`} onClose={() => setRejecting(null)}>
          <label>
            Motivo da rejeição
            <textarea
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              minLength={10}
              maxLength={500}
              placeholder="Explique objetivamente o que precisa ser corrigido"
            />
          </label>
          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setRejecting(null)}>
              Cancelar
            </button>
            <button
              type="button"
              className="btn btn-danger"
              disabled={busy || reason.trim().length < 10}
              onClick={reject}
            >
              Confirmar rejeição
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
