import { useState } from 'react';
import { api, ApiError, downloadFile, openBlob } from '../../api/client';
import type { StaffDocumentView } from '../../api/types';
import { Modal } from '../../components/Modal';
import { Card, EmptyState, ErrorBanner, LoadingBlock, PageHeader, StatusBadge } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { useAuth } from '../../state/AuthContext';

export function ListingDocuments() {
  const { session } = useAuth();
  const [reviewing, setReviewing] = useState<StaffDocumentView | null>(null);
  const [signerNameMatches, setSignerNameMatches] = useState(true);
  const [validatorReference, setValidatorReference] = useState('');
  const [notes, setNotes] = useState('');
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const documents = useResource((token) => api.get<StaffDocumentView[]>('/staff/listing-documents', token));

  async function download(document: StaffDocumentView) {
    if (!session) return;
    try {
      const blob = await downloadFile(`/staff/listing-documents/${document.id}/signed`, session.token);
      openBlob(blob, document.originalFilename || `${document.documentCode}.pdf`);
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível baixar o documento.');
    }
  }

  async function submitReview(approved: boolean) {
    if (!session || !reviewing) return;
    setBusy(true);
    setActionError(null);
    try {
      await api.post(
        `/staff/listing-documents/${reviewing.id}/review`,
        { approved, signerNameMatches, validatorReference, notes },
        session.token,
      );
      setReviewing(null);
      setValidatorReference('');
      setNotes('');
      documents.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível registrar a análise.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Documentos de anúncio"
        subtitle="Termo assinado pelo anunciante confirmando a titularidade do espaço."
      />
      {documents.error && <ErrorBanner message={documents.error} />}
      {actionError && <ErrorBanner message={actionError} />}
      {documents.loading && <LoadingBlock />}
      {documents.data && documents.data.length === 0 && (
        <EmptyState message="Nenhum documento aguardando análise." />
      )}
      <div className="card-grid">
        {documents.data?.map((document) => (
          <Card key={document.id}>
            <div className="card-title-row">
              <h3>{document.documentCode}</h3>
              <StatusBadge status={document.status} />
            </div>
            <small className="muted">{document.spaceTitle}</small>
            <p>
              Anunciante: {document.ownerName} ({document.ownerEmail})
            </p>
            <div className="card-actions">
              <button type="button" className="btn btn-ghost" onClick={() => download(document)}>
                Baixar PDF assinado
              </button>
              <button type="button" className="btn btn-primary" onClick={() => setReviewing(document)}>
                Analisar
              </button>
            </div>
          </Card>
        ))}
      </div>
      {reviewing && (
        <Modal title={`Analisar documento ${reviewing.documentCode}`} onClose={() => setReviewing(null)}>
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={signerNameMatches}
              onChange={(event) => setSignerNameMatches(event.target.checked)}
            />
            O nome do assinante confere com o anunciante
          </label>
          <label>
            Referência da validação
            <input
              value={validatorReference}
              onChange={(event) => setValidatorReference(event.target.value)}
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
              onClick={() => submitReview(false)}
            >
              Reprovar
            </button>
            <button
              type="button"
              className="btn btn-primary"
              disabled={busy || !validatorReference.trim() || notes.trim().length < 5}
              onClick={() => submitReview(true)}
            >
              Aprovar
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
