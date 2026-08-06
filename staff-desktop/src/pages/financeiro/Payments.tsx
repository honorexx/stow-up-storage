import { api, ApiError, downloadFile, openBlob } from '../../api/client';
import type { PaymentView } from '../../api/types';
import { Card, EmptyState, ErrorBanner, LoadingBlock, PageHeader, StatusBadge } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { useAuth } from '../../state/AuthContext';
import { useState } from 'react';

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export function Payments() {
  const { session } = useAuth();
  const [actionError, setActionError] = useState<string | null>(null);

  const payments = useResource((token) => api.get<PaymentView[]>('/admin/payments', token));

  async function downloadReceipt(payment: PaymentView) {
    if (!session) return;
    try {
      const blob = await downloadFile(`/admin/payments/${payment.id}/receipt`, session.token);
      openBlob(blob, `comprovante-stowup-${payment.id}.pdf`);
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Não foi possível baixar o comprovante.');
    }
  }

  const total = payments.data
    ?.filter((payment) => payment.status === 'APPROVED' || payment.status === 'PAID')
    .reduce((sum, payment) => sum + payment.amount, 0);

  return (
    <div>
      <PageHeader
        title="Pagamentos"
        subtitle="Transações processadas via Mercado Pago, sem exibir dados sensíveis de cartão."
        actions={total !== undefined && <strong>{currency.format(total)} aprovados</strong>}
      />
      {payments.error && <ErrorBanner message={payments.error} />}
      {actionError && <ErrorBanner message={actionError} />}
      {payments.loading && <LoadingBlock />}
      {payments.data && payments.data.length === 0 && <EmptyState message="Nenhuma transação registrada." />}
      {payments.data && payments.data.length > 0 && (
        <Card>
          <table className="table">
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Espaço</th>
                <th>Valor</th>
                <th>Status</th>
                <th>Data</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {payments.data.map((payment) => (
                <tr key={payment.id}>
                  <td>
                    {payment.customerName}
                    <br />
                    <small className="muted">{payment.customerEmail}</small>
                  </td>
                  <td>{payment.spaceTitle}</td>
                  <td>{currency.format(payment.amount)}</td>
                  <td>
                    <StatusBadge status={payment.status} />
                  </td>
                  <td>{dateFormat.format(new Date(payment.createdAt))}</td>
                  <td>
                    {payment.receiptAvailable && (
                      <button
                        type="button"
                        className="btn btn-ghost"
                        onClick={() => downloadReceipt(payment)}
                      >
                        Comprovante
                      </button>
                    )}
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
