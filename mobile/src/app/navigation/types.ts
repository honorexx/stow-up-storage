import type { NavigatorScreenParams } from '@react-navigation/native';

export type RootStackParamList = {
  Welcome: undefined;
  Login: undefined;
  Register: undefined;
  PasswordRecovery: { email?: string } | undefined;
  Main: NavigatorScreenParams<MainTabParamList> | undefined;
  EditProfile: undefined;
  DeleteAccount: undefined;
  MyListings: undefined;
  CreateListing: { space?: import('../../core/api/marketplace').OwnerSpace } | undefined;
  ListingDocument: { spaceId: number; title: string };
  SpaceDetail: { spaceId: number };
  Conversation: { conversationId: string; title: string; participantPhotoUrl?: string | null };
  NewSupportTicket: undefined;
  SupportTicketChat: { ticketId: string; ticketNumber: string; subject: string; status: 'OPEN' | 'CLOSED' };
  TermsOfUse: undefined;
  ReserveSpace: { spaceId: number; title: string; price: number };
  BookingDocuments: { booking: import('../../core/api/marketplace').Booking };
  Favorites: undefined;
  Notifications: undefined;
  OwnerDashboard: undefined;
  OwnerRental: { bookingId: string };
  Reviews: undefined;
};

export type MainTabParamList = {
  Explorar: undefined;
  Reservas: undefined;
  Suporte: undefined;
  Conversas: undefined;
  Perfil: undefined;
};
