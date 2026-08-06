import { useState } from 'react';
import { api, ApiError, downloadFile, openBlob } from '../../api/client';
import type { StaffContractView } from '../../api/types';
import { Modal } from '../../components/Modal';
import { Card, EmptyState, ErrorBanner, LoadingBlock, PageHeader, StatusBadge } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { useAuth } from '../../state/AuthContext';

const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export function Contracts() {
  const { session } = useAuth();
  const [status, setStatus] = useState('PENDING_REVIEW');
  const [reviewing, setReviewing] = useState<StaffContractView | null>(null);
  const [signerNameMatches, setSignerNameMatches] = useState(true);
  const [validatorReference, setValidatorReference] = useState('');
  const [notes, setNotes] = useState('');
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const contracts = useResource(
    (token) => api.get<StaffContractView[]>(`/staff/contracts?status=${status}`, token),
    [status],
  );

  async function download(contract: StaffContractView) {
    if (!session) return;
    try {
      const blob = await downloadFile(`/staff/contracts/${contract.id}/signed`, session.token);
      openBlob(blob, contract.originalFilename || `${contract.contractCode}.pdf`);
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível baixar o contrato.');
    }
  }

  async function submitReview(result: 'VALID' | 'INVALID') {
    if (!session || !reviewing) return;
    setBusy(true);
    setActionError(null);
    try {
      await api.post(
        `/staff/contracts/${reviewing.id}/review`,
        { result, signerNameMatches, validatorReportReference: validatorReference, notes },
        session.token,
      );
      setReviewing(null);
      setValidatorReference('');
      setNotes('');
      contracts.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível registrar a análise.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Contratos de locação"
        subtitle="Confira a assinatura digital em validar.iti.gov.br antes de aprovar."
        actions={
          <select value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="PENDING_REVIEW">Aguardando análise</option>
            <option value="VALID">Aprovados</option>
            <option value="INVALID">Reprovados</option>
          </select>
        }
      />
      {contracts.error && <ErrorBanner message={contracts.error} />}
      {actionError && <ErrorBanner message={actionError} />}
      {contracts.loading && <LoadingBlock />}
      {contracts.data && contracts.data.length === 0 && <EmptyState message="Nenhum contrato nesta fila." />}
      <div className="card-grid">
        {contracts.data?.map((contract) => (
          <Card key={contract.id}>
            <div className="card-title-row">
              <h3>{contract.contractCode}</h3>
              <StatusBadge status={contract.status} />
            </div>
            <small className="muted">{contract.spaceTitle}</small>
            <p>
              Cliente: {contract.customerName} ({contract.customerEmail})
              <br />
              Anunciante: {contract.ownerName}
            </p>
            <small className="muted">Assinado em {dateFormat.format(new Date(contract.signedAt))}</small>
            <div className="card-actions">
              <button type="button" className="btn btn-ghost" onClick={() => download(contract)}>
                Baixar PDF assinado
              </button>
              {status === 'PENDING_REVIEW' && (
                <button type="button" className="btn btn-primary" onClick={() => setReviewing(contract)}>
                  Analisar
                </button>
              )}
            </div>
          </Card>
        ))}
      </div>
      {reviewing && (
        <Modal title={`Analisar contrato ${reviewing.contractCode}`} onClose={() => setReviewing(null)}>
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={signerNameMatches}
              onChange={(event) => setSignerNameMatches(event.target.checked)}
            />
            O nome do assinante confere com o cliente
          </label>
          <label>
            Referência da validação (validar.iti.gov.br)
            <input
              value={validatorReference}
              onChange={(event) => setValidatorReference(event.target.value)}
              placeholder="Código ou observação do validador"
              maxLength={500}
            />
          </label>
          <label>
            Notas da análise
            <textarea
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              minLength={5}
              maxLength={1000}
              placeholder="Descreva o que foi verificado"
            />
          </label>
          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setReviewing(null)}>
              Cancelar
            </button>
            <button
              type="button"
              className="btn btn-danger"
              disabled={busy || !validatorReference.trim() || notes.trim().length < 5}
              onClick={() => submitReview('INVALID')}
            >
              Reprovar
            </button>
            <button
              type="button"
              className="btn btn-primary"
              disabled={busy || !validatorReference.trim() || notes.trim().length < 5}
              onClick={() => submitReview('VALID')}
            >
              Aprovar
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
