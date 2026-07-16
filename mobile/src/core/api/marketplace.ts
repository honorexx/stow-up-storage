import { API_URL } from './config';
import { authorized } from './client';

export type Booking = {
  id: string;
  spaceId: number;
  spaceTitle: string;
  start: string;
  end: string;
  months: number;
  total: number;
  status: string;
  category: 'PENDING' | 'FUTURE' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  reviewAvailable: boolean;
  contract: string;
  paymentMethod: string;
};
export type SupportCategory =
  | 'PAYMENT'
  | 'LISTING'
  | 'BOOKING'
  | 'CONTRACT'
  | 'SECURITY'
  | 'ACCOUNT'
  | 'OTHER';
export type SupportTicket = {
  id: string;
  ticketNumber: string;
  category: SupportCategory;
  subject: string;
  status: 'OPEN' | 'CLOSED';
  lastMessage: string;
  createdAt: string;
  updatedAt: string;
};
export type SupportTicketMessage = {
  id: number;
  senderType: 'CUSTOMER' | 'SUPPORT';
  senderName: string;
  body: string;
  attachmentName?: string | null;
  attachmentContentType?: string | null;
  attachmentSize?: number | null;
  createdAt: string;
};
export type Space = {
  id: number;
  title: string;
  city: string;
  district: string;
  type: string;
  size: number;
  heightMeters: number;
  lengthMeters: number;
  depthMeters: number;
  volumeCubicMeters: number;
  price: number;
  rating: number;
  reviews: number;
  distance: number;
  latitude?: number | null;
  longitude?: number | null;
  verified: boolean;
  available: boolean;
  features: string[];
  photos: string[];
  description: string;
  owner?: string;
  ownerId?: number;
  ownerVerified: boolean;
  lifecycleStatus: string;
  capacityTotal: number;
  capacityUsed: number;
  capacityAvailable: number;
};
export type Conversation = {
  id: string;
  spaceId: number;
  spaceTitle: string;
  participantName: string;
  participantPhotoUrl?: string | null;
  lastMessage: string;
  updatedAt: string;
};
export type RentalMessage = {
  id: number;
  senderName: string;
  mine: boolean;
  body: string;
  createdAt: string;
};
export type OwnerSpace = Space & {
  postalCode: string;
  addressLine: string;
  addressNumber: string;
  addressComplement?: string | null;
  state: string;
  photos: string[];
  documentStatus: 'GENERATED' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED';
  moderationStatus: string;
  moderationReason?: string | null;
  minimumPhotos: number;
  photoRequirementMet: boolean;
};
export type ListingDocument = {
  id: string;
  spaceId: number;
  documentCode: string;
  status: 'GENERATED' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED';
  originalFilename?: string | null;
  reviewNotes?: string | null;
  createdAt: string;
  signedAt?: string | null;
  reviewedAt?: string | null;
};
export type CreateSpaceInput = {
  title: string;
  city: string;
  district: string;
  postalCode: string;
  addressLine: string;
  addressNumber: string;
  addressComplement: string;
  state: string;
  type: string;
  size: number;
  price: number;
  latitude: number | null;
  longitude: number | null;
  description: string;
  features: string[];
  heightMeters: number;
  lengthMeters: number;
  depthMeters: number;
};
export type SearchFilters = {
  query?: string;
  city?: string;
  district?: string;
  postalCode?: string;
  type?: string;
  minPrice?: number;
  maxPrice?: number;
  minSize?: number;
  maxSize?: number;
  features?: string[];
  sort?: string;
  userLatitude?: number;
  userLongitude?: number;
};
export type NotificationItem = {
  id: number;
  type: string;
  title: string;
  message: string;
  referenceType?: string | null;
  referenceId?: string | null;
  read: boolean;
  createdAt: string;
};
export type NotificationPage = { unread: number; items: NotificationItem[] };
export type OwnerDashboard = {
  activeSpaces: number;
  occupiedSpaces: number;
  pendingReservations: number;
  activeContracts: number;
  monthRevenue: number;
  totalRevenue: number;
  occupancyRate: number;
  upcomingContracts: {
    bookingId: string;
    spaceTitle: string;
    customer: string;
    start: string;
    end: string;
    total: number;
  }[];
  activeRentals: {
    bookingId: string;
    spaceTitle: string;
    customer: string;
    end: string;
    customerConfirmed: boolean;
    ownerConfirmed: boolean;
  }[];
  spaces: OwnerSpace[];
};
export type FinancialReport = {
  totalRevenue: number;
  completedContracts: number;
  averageRentalValue: number;
  monthlyRevenue: Record<string, number>;
};
export type EligibleReview = {
  bookingId: string;
  spaceTitle: string;
  eligible: boolean;
  reason: string;
  deadline?: string | null;
};
export type SpaceDocument = {
  id: string;
  type: string;
  name: string;
  visibility: string;
  uploadedAt: string;
};

export async function listSpaces(filters: SearchFilters = {}): Promise<Space[]> {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value == null || value === '' || (Array.isArray(value) && !value.length)) return;
    if (Array.isArray(value)) value.forEach((item) => params.append(key, String(item)));
    else params.set(key, String(value));
  });
  const response = await fetch(`${API_URL}/spaces${params.size ? `?${params}` : ''}`);
  if (!response.ok) throw new Error('Não foi possível carregar os espaços.');
  return response.json();
}
export async function getSpace(id: number): Promise<Space> {
  const response = await fetch(`${API_URL}/spaces/${id}`);
  if (!response.ok) throw new Error('Espaço não encontrado.');
  return response.json();
}
export const listBookings = (token: string, category = 'ALL') =>
  authorized<Booking[]>(`/bookings?category=${encodeURIComponent(category)}`, token);
export const createBooking = (token: string, input: { spaceId: number; start: string; months: number }) =>
  authorized<Booking>('/bookings', token, {
    method: 'POST',
    body: JSON.stringify({ ...input, paymentMethod: 'MERCADO_PAGO' }),
  });
export const listSupportTickets = (token: string) => authorized<SupportTicket[]>('/support/tickets', token);
export const createSupportTicket = (
  token: string,
  input: { category: SupportCategory; subject: string; description: string },
) => authorized<SupportTicket>('/support/tickets', token, { method: 'POST', body: JSON.stringify(input) });
export const listSupportTicketMessages = (token: string, id: string) =>
  authorized<SupportTicketMessage[]>(`/support/tickets/${id}/messages`, token);
export const sendSupportTicketMessage = (token: string, id: string, body: string) =>
  authorized<SupportTicketMessage>(`/support/tickets/${id}/messages`, token, {
    method: 'POST',
    body: JSON.stringify({ body }),
  });
export async function uploadSupportAttachment(
  token: string,
  id: string,
  file: { uri: string; name: string; mimeType?: string | null },
) {
  const form = new FormData();
  form.append('file', {
    uri: file.uri,
    name: file.name,
    type: file.mimeType || 'application/octet-stream',
  } as any);
  const response = await fetch(`${API_URL}/support/tickets/${id}/attachments`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message ?? 'Não foi possível enviar o anexo.');
  return body as SupportTicketMessage;
}
export const listConversations = (token: string) => authorized<Conversation[]>('/conversations', token);
export const startConversation = (token: string, spaceId: number) =>
  authorized<Conversation>('/conversations', token, { method: 'POST', body: JSON.stringify({ spaceId }) });
export const listRentalMessages = (token: string, id: string) =>
  authorized<RentalMessage[]>(`/conversations/${id}/messages`, token);
export const sendRentalMessage = (token: string, id: string, body: string) =>
  authorized<RentalMessage>(`/conversations/${id}/messages`, token, {
    method: 'POST',
    body: JSON.stringify({ body }),
  });
export const listMySpaces = (token: string) => authorized<OwnerSpace[]>('/owner/spaces', token);
export const getOwnerDashboard = (token: string) => authorized<OwnerDashboard>('/owner/dashboard', token);
export const getFinancialReport = (token: string) =>
  authorized<FinancialReport>('/owner/financial-report', token);
export const setSpaceAvailability = (token: string, id: number, available: boolean) =>
  authorized<OwnerSpace>(`/owner/spaces/${id}/availability`, token, {
    method: 'PATCH',
    body: JSON.stringify({ available }),
  });
export const createSpace = (token: string, input: CreateSpaceInput) =>
  authorized<OwnerSpace>('/owner/spaces', token, { method: 'POST', body: JSON.stringify(input) });
export const listFavorites = (token: string) => authorized<Space[]>('/favorites', token);
export const favoriteStatus = (token: string, spaceId: number) =>
  authorized<{ favorite: boolean }>(`/favorites/${spaceId}`, token);
export const addFavorite = (token: string, spaceId: number) =>
  authorized<void>(`/favorites/${spaceId}`, token, { method: 'POST' });
export const removeFavorite = (token: string, spaceId: number) =>
  authorized<void>(`/favorites/${spaceId}`, token, { method: 'DELETE' });
export const listNotifications = (token: string) => authorized<NotificationPage>('/notifications', token);
export const readNotification = (token: string, id: number) =>
  authorized<NotificationItem>(`/notifications/${id}/read`, token, { method: 'POST' });
export const readAllNotifications = (token: string) =>
  authorized<void>('/notifications/read-all', token, { method: 'POST' });
export const deleteOwnedSpace = (token: string, id: number, password: string) =>
  authorized<void>(`/owner/spaces/${id}`, token, {
    method: 'DELETE',
    body: JSON.stringify({ password }),
  });
export const spaceAvailability = (spaceId: number) =>
  fetch(`${API_URL}/bookings/availability/${spaceId}`).then(async (r) => {
    if (!r.ok) throw new Error('Não foi possível carregar a disponibilidade.');
    return r.json() as Promise<{ start: string; end: string }[]>;
  });
export const checkoutBooking = (token: string, bookingId: string) =>
  authorized<Booking>(`/bookings/${bookingId}/checkout`, token, { method: 'POST' });
export const eligibleReviews = (token: string) => authorized<EligibleReview[]>('/reviews/eligible', token);
export const createReview = (token: string, bookingId: string, rating: number, comment: string) =>
  authorized('/reviews', token, { method: 'POST', body: JSON.stringify({ bookingId, rating, comment }) });
export const listSpaceDocuments = (token: string, spaceId: number) =>
  authorized<SpaceDocument[]>(`/owner/spaces/${spaceId}/documents`, token);
export async function uploadSpaceDocument(
  token: string,
  spaceId: number,
  file: { uri: string; name: string; mimeType?: string | null },
  type: string,
  visibility = 'ADMIN_ONLY',
) {
  const form = new FormData();
  form.append('file', { uri: file.uri, name: file.name, type: file.mimeType || 'application/pdf' } as any);
  const response = await fetch(
    `${API_URL}/owner/spaces/${spaceId}/documents?type=${encodeURIComponent(type)}&visibility=${encodeURIComponent(visibility)}`,
    { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: form },
  );
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message || 'Não foi possível enviar o documento.');
  return body as SpaceDocument;
}
export const getListingDocument = (token: string, spaceId: number) =>
  authorized<ListingDocument>(`/owner/spaces/${spaceId}/document`, token);
export const listingDocumentPdfUrl = (spaceId: number) => `${API_URL}/owner/spaces/${spaceId}/document/pdf`;
export async function uploadSignedListingDocument(
  token: string,
  spaceId: number,
  file: { uri: string; name: string; mimeType?: string | null },
): Promise<ListingDocument> {
  const form = new FormData();
  form.append('file', { uri: file.uri, name: file.name, type: file.mimeType || 'application/pdf' } as any);
  const response = await fetch(`${API_URL}/owner/spaces/${spaceId}/document/signed`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message ?? 'Não foi possível enviar o termo assinado.');
  return body as ListingDocument;
}
export type LocalImage = { uri: string; mimeType?: string | null; fileName?: string | null };
export async function uploadSpacePhoto(
  token: string,
  spaceId: number,
  image: LocalImage,
): Promise<OwnerSpace> {
  const form = new FormData();
  const extension =
    image.fileName?.split('.').pop()?.toLowerCase() ||
    image.uri.split('.').pop()?.split('?')[0].toLowerCase() ||
    'jpg';
  const type =
    image.mimeType ||
    (extension === 'png'
      ? 'image/png'
      : extension === 'webp'
        ? 'image/webp'
        : extension === 'heic' || extension === 'heif'
          ? 'image/heic'
          : 'image/jpeg');
  form.append('file', { uri: image.uri, name: image.fileName || `space.${extension}`, type } as any);
  const response = await fetch(`${API_URL}/owner/spaces/${spaceId}/photos`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message ?? 'Não foi possível enviar a foto.');
  return body as OwnerSpace;
}
