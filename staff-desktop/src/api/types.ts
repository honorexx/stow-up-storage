export type StaffRole =
  | 'ADMIN'
  | 'SUPPORT'
  | 'MODERATOR'
  | 'LISTING_ANALYST'
  | 'DOCUMENT_ANALYST'
  | 'CONTRACT_ANALYST';

export type AppRole = 'CUSTOMER' | 'OWNER';

export interface StaffUserInfo {
  name: string;
  email: string;
  role: StaffRole;
  additionalRoles: StaffRole[];
}

export interface AuthResponse {
  token: string;
  user: StaffUserInfo;
}

export interface DashboardData {
  totalUsers: number;
  customers: number;
  owners: number;
  verifiedOwners: number;
  activeSpaces: number;
  occupiedSpaces: number;
  pendingSpaces: number;
  ongoingBookings: number;
  activeContracts: number;
  pendingReports: number;
  platformRevenue: number;
}

export interface AuditEntry {
  id: number;
  actor: string;
  action: string;
  entityType: string;
  entityId: string;
  details: string;
  createdAt: string;
}

export interface ConversationView {
  id: string;
  ticketNumber: string;
  category: string;
  subject: string;
  customerName: string;
  customerEmail: string;
  status: 'OPEN' | 'CLOSED';
  lastMessage: string;
  updatedAt: string;
}

export interface MessageView {
  id: number;
  senderType: string;
  senderName: string;
  body: string;
  attachmentName: string | null;
  attachmentContentType: string | null;
  attachmentSize: number | null;
  createdAt: string;
}

export interface ModerationSpaceView {
  id: number;
  title: string;
  ownerName: string;
  ownerEmail: string;
  city: string;
  district: string;
  type: string;
  size: number;
  price: number;
  description: string;
  photos: string[];
  moderationStatus: string;
  moderationReason: string | null;
  moderatedAt: string | null;
}

export interface StaffContractView {
  id: string;
  bookingId: string;
  contractCode: string;
  status: string;
  customerName: string;
  customerEmail: string;
  ownerName: string;
  spaceTitle: string;
  originalFilename: string;
  validatorUrl: string;
  signedAt: string;
}

export interface StaffDocumentView {
  id: string;
  spaceId: number;
  documentCode: string;
  status: string;
  ownerName: string;
  ownerEmail: string;
  spaceTitle: string;
  originalFilename: string;
  signedAt: string;
}

export interface PaymentView {
  id: string;
  bookingId: string;
  providerPreferenceId: string | null;
  providerPaymentId: string | null;
  status: string;
  amount: number;
  customerName: string;
  customerEmail: string;
  spaceTitle: string;
  createdAt: string;
  updatedAt: string;
  receiptAvailable: boolean;
}

export interface StaffView {
  id: number;
  staffCode: string;
  name: string;
  email: string;
  role: StaffRole;
  additionalRoles: StaffRole[];
  enabled: boolean;
}

export interface AppUserView {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  city: string | null;
  state: string | null;
  role: AppRole;
  enabled: boolean;
  ownerVerified: boolean;
  photoUrl: string | null;
  listings: number;
  createdAt: string;
}
