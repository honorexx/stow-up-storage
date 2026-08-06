import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Platform, Text, useWindowDimensions } from 'react-native';
import { ExploreScreen } from '../../features/explore/ExploreScreen';
import { colors } from '../../shared/theme';
import { MainTabParamList } from './types';
import { ProfileScreen } from '../../features/profile/ProfileScreen';
import { SupportScreen } from '../../features/support/SupportScreen';
import { ReservationsScreen } from '../../features/booking/ReservationsScreen';
import { ConversationsScreen } from '../../features/conversation/ConversationsScreen';

const Tab = createBottomTabNavigator<MainTabParamList>();
const icons: Record<keyof MainTabParamList, string> = {
  Explorar: '⌖',
  Reservas: '▣',
  Conversas: '◉',
  Suporte: '◌',
  Perfil: '●',
};

export function MainTabs() {
  const { width } = useWindowDimensions();
  const desktop = Platform.OS === 'web' && width >= 960;
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarPosition: desktop ? 'left' : 'bottom',
        tabBarActiveTintColor: colors.orange,
        tabBarActiveBackgroundColor: desktop ? colors.orangeSoft : undefined,
        tabBarInactiveTintColor: colors.muted,
        tabBarStyle: desktop
          ? {
              width: 210,
              paddingTop: 22,
              paddingHorizontal: 12,
              borderRightColor: colors.line,
              backgroundColor: '#fff',
            }
          : { height: 72, paddingTop: 8, paddingBottom: 9, borderTopColor: colors.line },
        tabBarItemStyle: desktop ? { maxHeight: 58, borderRadius: 14 } : undefined,
        tabBarLabelPosition: desktop ? 'beside-icon' : 'below-icon',
        tabBarLabelStyle: { fontSize: desktop ? 13 : 11, fontWeight: '700' },
        tabBarIcon: ({ color }) => (
          <Text style={{ color, fontSize: desktop ? 19 : 23, fontWeight: '700' }}>{icons[route.name]}</Text>
        ),
      })}
    >
      <Tab.Screen name="Explorar" component={ExploreScreen} />
      <Tab.Screen name="Reservas" component={ReservationsScreen} />
      <Tab.Screen name="Conversas" component={ConversationsScreen} />
      <Tab.Screen name="Suporte" component={SupportScreen} />
      <Tab.Screen name="Perfil" component={ProfileScreen} />
    </Tab.Navigator>
  );
}
