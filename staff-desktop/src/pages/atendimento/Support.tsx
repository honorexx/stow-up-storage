import { useState } from 'react';
import { api, ApiError } from '../../api/client';
import type { ConversationView, MessageView } from '../../api/types';
import { Card, EmptyState, ErrorBanner, LoadingBlock, PageHeader, StatusBadge } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { useAuth } from '../../state/AuthContext';

const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export function Support() {
  const { session } = useAuth();
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'OPEN' | 'CLOSED'>('ALL');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [reply, setReply] = useState('');
  const [sending, setSending] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const queue = useResource(
    (token) => api.get<ConversationView[]>(`/staff/support/conversations?status=${statusFilter}`, token),
    [statusFilter],
  );

  const thread = useResource(
    (token) =>
      selectedId
        ? api.get<MessageView[]>(`/staff/support/conversations/${selectedId}/messages`, token)
        : Promise.resolve<MessageView[]>([]),
    [selectedId],
  );

  const selected = queue.data?.find((item) => item.id === selectedId) ?? null;

  async function sendReply() {
    if (!session || !selectedId || !reply.trim()) return;
    setSending(true);
    setActionError(null);
    try {
      await api.post(
        `/staff/support/conversations/${selectedId}/messages`,
        { body: reply.trim() },
        session.token,
      );
      setReply('');
      thread.reload();
      queue.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível enviar a resposta.');
    } finally {
      setSending(false);
    }
  }

  async function toggleStatus() {
    if (!session || !selected) return;
    try {
      await api.patch(
        `/staff/support/conversations/${selected.id}/status`,
        { status: selected.status === 'OPEN' ? 'CLOSED' : 'OPEN' },
        session.token,
      );
      queue.reload();
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível alterar o status.');
    }
  }

  return (
    <div>
      <PageHeader
        title="Fila de atendimento"
        subtitle="Chamados dos clientes StowUp, com histórico completo por conversa."
        actions={
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as typeof statusFilter)}
          >
            <option value="ALL">Todos</option>
            <option value="OPEN">Em aberto</option>
            <option value="CLOSED">Encerrados</option>
          </select>
        }
      />
      {queue.error && <ErrorBanner message={queue.error} />}
      <div className="split-layout">
        <Card className="queue-list">
          {queue.loading && <LoadingBlock />}
          {queue.data && queue.data.length === 0 && <EmptyState message="Nenhum atendimento nesta fila." />}
          {queue.data?.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`queue-item ${selectedId === item.id ? 'active' : ''}`}
              onClick={() => setSelectedId(item.id)}
            >
              <div className="queue-item-top">
                <strong>{item.customerName}</strong>
                <StatusBadge status={item.status} tone={item.status === 'OPEN' ? 'warn' : 'ok'} />
              </div>
              <small className="muted">{item.customerEmail}</small>
              <p className="queue-item-preview">{item.lastMessage}</p>
              <small className="muted">{dateFormat.format(new Date(item.updatedAt))}</small>
            </button>
          ))}
        </Card>
        <Card className="chat-panel">
          {!selected && <EmptyState message="Selecione um atendimento para ver a conversa." />}
          {selected && (
            <>
              <div className="chat-header">
                <div>
                  <h2>{selected.customerName}</h2>
                  <small className="muted">{selected.customerEmail}</small>
                </div>
                <button type="button" className="btn btn-ghost" onClick={toggleStatus}>
                  {selected.status === 'OPEN' ? 'Encerrar atendimento' : 'Reabrir atendimento'}
                </button>
              </div>
              {actionError && <ErrorBanner message={actionError} />}
              <div className="chat-messages">
                {thread.loading && <LoadingBlock />}
                {thread.data?.map((message) => (
                  <div
                    key={message.id}
                    className={`bubble ${message.senderType === 'SUPPORT' ? 'staff' : ''}`}
                  >
                    <p>{message.body}</p>
                    <small className="muted">
                      {message.senderName} · {dateFormat.format(new Date(message.createdAt))}
                    </small>
                  </div>
                ))}
              </div>
              <div className="chat-composer">
                <textarea
                  value={reply}
                  onChange={(event) => setReply(event.target.value)}
                  placeholder="Escreva uma resposta clara e objetiva"
                  maxLength={2000}
                />
                <button type="button" className="btn btn-primary" onClick={sendReply} disabled={sending}>
                  {sending ? 'Enviando…' : 'Enviar resposta'}
                </button>
              </div>
            </>
          )}
        </Card>
      </div>
    </div>
  );
}
