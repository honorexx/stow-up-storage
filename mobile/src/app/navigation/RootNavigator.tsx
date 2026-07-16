import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { LoginScreen } from '../../features/auth/LoginScreen';
import { RegisterScreen } from '../../features/auth/RegisterScreen';
import { WelcomeScreen } from '../../features/auth/WelcomeScreen';
import { PasswordRecoveryScreen } from '../../features/auth/PasswordRecoveryScreen';
import { MainTabs } from './MainTabs';
import { RootStackParamList } from './types';
import { EditProfileScreen } from '../../features/profile/EditProfileScreen';
import { useAuth } from '../../features/auth/AuthContext';
import { ActivityIndicator, View } from 'react-native';
import { colors } from '../../shared/theme';
import { MyListingsScreen } from '../../features/listing/MyListingsScreen';
import { CreateListingScreen } from '../../features/listing/CreateListingScreen';
import { SpaceDetailScreen } from '../../features/explore/SpaceDetailScreen';
import { ConversationScreen } from '../../features/conversation/ConversationScreen';
import { NewSupportTicketScreen } from '../../features/support/NewSupportTicketScreen';
import { SupportTicketChatScreen } from '../../features/support/SupportTicketChatScreen';
import { TermsOfUseScreen } from '../../features/legal/TermsOfUseScreen';
import { ReserveSpaceScreen } from '../../features/booking/ReserveSpaceScreen';
import { BookingDocumentsScreen } from '../../features/booking/BookingDocumentsScreen';
import { ListingDocumentScreen } from '../../features/listing/ListingDocumentScreen';
import { DeleteAccountScreen } from '../../features/profile/DeleteAccountScreen';
import { FavoritesScreen } from '../../features/favorites/FavoritesScreen';
import { NotificationsScreen } from '../../features/notification/NotificationsScreen';
import { OwnerDashboardScreen } from '../../features/owner/OwnerDashboardScreen';
import { OwnerRentalScreen } from '../../features/owner/OwnerRentalScreen';
import { ReviewsScreen } from '../../features/review/ReviewsScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  const { user, restoring } = useAuth();
  if (restoring) {
    return (
      <View
        style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.surface }}
      >
        <ActivityIndicator color={colors.orange} size="large" />
      </View>
    );
  }
  return (
    <Stack.Navigator
      initialRouteName={user ? 'Main' : 'Welcome'}
      screenOptions={{ headerShown: false, animation: 'slide_from_right' }}
    >
      <Stack.Screen name="Welcome" component={WelcomeScreen} />
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="Register" component={RegisterScreen} />
      <Stack.Screen name="PasswordRecovery" component={PasswordRecoveryScreen} />
      <Stack.Screen name="Main" component={MainTabs} />
      <Stack.Screen name="EditProfile" component={EditProfileScreen} />
      <Stack.Screen name="DeleteAccount" component={DeleteAccountScreen} />
      <Stack.Screen name="MyListings" component={MyListingsScreen} />
      <Stack.Screen name="CreateListing" component={CreateListingScreen} />
      <Stack.Screen name="ListingDocument" component={ListingDocumentScreen} />
      <Stack.Screen name="SpaceDetail" component={SpaceDetailScreen} />
      <Stack.Screen name="Conversation" component={ConversationScreen} />
      <Stack.Screen name="NewSupportTicket" component={NewSupportTicketScreen} />
      <Stack.Screen name="SupportTicketChat" component={SupportTicketChatScreen} />
      <Stack.Screen name="TermsOfUse" component={TermsOfUseScreen} />
      <Stack.Screen name="ReserveSpace" component={ReserveSpaceScreen} />
      <Stack.Screen name="BookingDocuments" component={BookingDocumentsScreen} />
      <Stack.Screen name="Favorites" component={FavoritesScreen} />
      <Stack.Screen name="Notifications" component={NotificationsScreen} />
      <Stack.Screen name="OwnerDashboard" component={OwnerDashboardScreen} />
      <Stack.Screen name="OwnerRental" component={OwnerRentalScreen} />
      <Stack.Screen name="Reviews" component={ReviewsScreen} />
    </Stack.Navigator>
  );
}
