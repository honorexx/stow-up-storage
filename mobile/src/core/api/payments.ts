import { authorized } from './client';
import { API_URL } from './config';

export type RentalContract = {
  id: string;
  bookingId: string;
  contractCode: string;
  status: 'GENERATED' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED';
  paymentApproved: boolean;
  originalFilename?: string | null;
  validatorReportReference?: string | null;
  reviewNotes?: string | null;
  createdAt: string;
  signedAt?: string | null;
  reviewedAt?: string | null;
};

export const createPaymentCheckout = (token: string, bookingId: string) =>
  authorized<{ preferenceId: string; checkoutUrl: string }>('/payments/checkout', token, {
    method: 'POST',
    body: JSON.stringify({ bookingId }),
  });

export const reconcilePayment = (token: string, bookingId: string) =>
  authorized<{ bookingId: string; approved: boolean }>('/payments/reconcile', token, {
    method: 'POST',
    body: JSON.stringify({ bookingId }),
  });

export const getRentalContract = (token: string, bookingId: string) =>
  authorized<RentalContract>(`/contracts/${bookingId}`, token);

export async function uploadSignedContract(
  token: string,
  bookingId: string,
  file: { uri: string; name: string; mimeType?: string | null },
): Promise<RentalContract> {
  const form = new FormData();
  form.append('file', {
    uri: file.uri,
    name: file.name,
    type: file.mimeType || 'application/pdf',
  } as any);
  const response = await fetch(`${API_URL}/contracts/${bookingId}/signed`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message ?? 'Não foi possível enviar o contrato assinado.');
  return body as RentalContract;
}

export const contractPdfUrl = (bookingId: string) => `${API_URL}/contracts/${bookingId}/pdf`;
