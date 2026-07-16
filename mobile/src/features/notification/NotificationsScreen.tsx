import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { FlatList, Pressable, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import {
  listNotifications,
  NotificationItem,
  readAllNotifications,
  readNotification,
} from '../../core/api/marketplace';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';
export function NotificationsScreen({
  navigation,
}: NativeStackScreenProps<RootStackParamList, 'Notifications'>) {
  const { token } = useAuth();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unread, setUnread] = useState(0);
  function refresh() {
    if (token)
      listNotifications(token)
        .then((p) => {
          setItems(p.items);
          setUnread(p.unread);
        })
        .catch(() => undefined);
  }
  useEffect(() => {
    refresh();
    const t = setInterval(refresh, 8000);
    return () => clearInterval(t);
  }, [token]);
  async function open(i: NotificationItem) {
    if (token && !i.read) {
      await readNotification(token, i.id);
      refresh();
    }
    if (i.referenceType === 'BOOKING') navigation.navigate('Main', { screen: 'Reservas' });
  }
  async function all() {
    if (token) {
      await readAllNotifications(token);
      refresh();
    }
  }
  return (
    <SafeAreaView style={s.safe}>
      <View style={s.header}>
        <Text onPress={navigation.goBack} style={s.back}>
          ‹ Voltar
        </Text>
        <Text style={s.title}>Notificações</Text>
        <Text onPress={all} style={s.all}>
          Ler todas
        </Text>
      </View>
      <FlatList
        data={items}
        keyExtractor={(i) => String(i.id)}
        contentContainerStyle={items.length ? s.list : s.empty}
        ListEmptyComponent={
          <View style={s.center}>
            <Text style={s.emptyTitle}>Tudo tranquilo por aqui</Text>
            <Text style={s.muted}>Novas reservas, contratos e avaliações aparecerão nesta área.</Text>
          </View>
        }
        renderItem={({ item }) => (
          <Pressable onPress={() => open(item)} style={[s.card, !item.read && s.unread]}>
            <View style={[s.dot, item.read && s.dotRead]} />
            <View style={{ flex: 1 }}>
              <Text style={s.cardTitle}>{item.title}</Text>
              <Text style={s.message}>{item.message}</Text>
              <Text style={s.date}>{new Date(item.createdAt).toLocaleString('pt-BR')}</Text>
            </View>
          </Pressable>
        )}
      />
      {unread > 0 && (
        <View style={s.badge}>
          <Text style={s.badgeText}>{unread} não lidas</Text>
        </View>
      )}
    </SafeAreaView>
  );
}
const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    height: 62,
    backgroundColor: '#fff',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 18,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  back: { color: colors.navy, fontWeight: '800' },
  title: { fontSize: 19, fontWeight: '900', color: colors.text },
  all: { color: colors.orange, fontWeight: '800', fontSize: 12 },
  list: { padding: 16, gap: 10 },
  empty: { flexGrow: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 30 },
  emptyTitle: { fontSize: 21, fontWeight: '900', color: colors.text },
  muted: { color: colors.muted, textAlign: 'center', marginTop: 7, lineHeight: 20 },
  card: {
    flexDirection: 'row',
    gap: 11,
    padding: 15,
    borderRadius: 16,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: colors.line,
  },
  unread: { backgroundColor: '#FFF7F0', borderColor: '#FFD9C4' },
  dot: { width: 9, height: 9, borderRadius: 5, backgroundColor: colors.orange, marginTop: 5 },
  dotRead: { backgroundColor: colors.line },
  cardTitle: { fontWeight: '900', color: colors.text },
  message: { color: colors.muted, lineHeight: 19, marginTop: 4 },
  date: { fontSize: 10, color: colors.muted, marginTop: 7 },
  badge: {
    position: 'absolute',
    bottom: 18,
    alignSelf: 'center',
    backgroundColor: colors.navy,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 15,
  },
  badgeText: { color: '#fff', fontWeight: '800', fontSize: 11 },
});
