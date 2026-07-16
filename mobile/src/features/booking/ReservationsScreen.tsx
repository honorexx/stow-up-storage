import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { useFocusEffect } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { MainTabParamList, RootStackParamList } from '../../app/navigation/types';
import { Booking, checkoutBooking, listBookings } from '../../core/api/marketplace';
import { BrandMark } from '../../shared/components/BrandMark';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

export function ReservationsScreen({ navigation }: BottomTabScreenProps<MainTabParamList, 'Reservas'>) {
  const auth = useAuth();
  const root = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();
  const [items, setItems] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [category, setCategory] = useState('PENDING');

  useFocusEffect(
    useCallback(() => {
      if (!auth.token) {
        setItems([]);
        return;
      }
      setLoading(true);
      setError('');
      listBookings(auth.token, category)
        .then(setItems)
        .catch((e) => setError(e.message))
        .finally(() => setLoading(false));
    }, [auth.token, category]),
  );
  useEffect(() => {
    if (!auth.token) return;
    const timer = setInterval(
      () =>
        listBookings(auth.token!, category)
          .then(setItems)
          .catch(() => undefined),
      8000,
    );
    return () => clearInterval(timer);
  }, [auth.token, category]);
  async function checkout(item: Booking) {
    if (!auth.token) return;
    try {
      const result = await checkoutBooking(auth.token, item.id);
      Alert.alert(
        result.category === 'COMPLETED' ? 'Locação encerrada' : 'Confirmação registrada',
        result.category === 'COMPLETED'
          ? 'As avaliações foram liberadas por 14 dias.'
          : 'Como a retirada foi antecipada, o proprietário também precisa confirmar.',
      );
      setItems(await listBookings(auth.token, category));
    } catch (e: any) {
      Alert.alert('Check-out', e.message);
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <Header />
      {!!auth.user && (
        <View style={styles.categories}>
          {[
            ['ALL', 'Todos'],
            ['PENDING', 'Pendentes'],
            ['FUTURE', 'Futuros'],
            ['ACTIVE', 'Ativos'],
            ['COMPLETED', 'Encerrados'],
            ['CANCELLED', 'Cancelados'],
          ].map(([v, l]) => (
            <Pressable
              key={v}
              onPress={() => setCategory(v)}
              style={[styles.category, category === v && styles.categoryOn]}
            >
              <Text style={[styles.categoryText, category === v && styles.categoryTextOn]}>{l}</Text>
            </Pressable>
          ))}
        </View>
      )}
      {!auth.user ? (
        <Guest onLogin={() => root?.navigate('Login')} />
      ) : loading ? (
        <ActivityIndicator style={styles.loader} color={colors.orange} size="large" />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.id}
          contentContainerStyle={items.length ? styles.list : styles.emptyList}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>▣</Text>
              <Text style={styles.title}>Nenhuma reserva ainda</Text>
              <Text style={styles.subtitle}>
                {error || 'Quando você reservar um espaço, os detalhes e o contrato aparecerão aqui.'}
              </Text>
              <View style={styles.button}>
                <PrimaryButton label="Encontrar um espaço" onPress={() => navigation.navigate('Explorar')} />
              </View>
            </View>
          }
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View style={styles.row}>
                <Text style={styles.cardTitle}>{item.spaceTitle}</Text>
                <Text style={styles.status}>{item.status}</Text>
              </View>
              <Text style={styles.meta}>
                {new Date(`${item.start}T12:00:00`).toLocaleDateString('pt-BR')} até{' '}
                {new Date(`${item.end}T12:00:00`).toLocaleDateString('pt-BR')}
              </Text>
              <Text style={styles.price}>R$ {Number(item.total).toFixed(2).replace('.', ',')}</Text>
              <Text style={styles.contract}>
                Contrato {item.contract} · {item.category}
              </Text>
              <View style={styles.manage}>
                <PrimaryButton
                  label="Contrato e pagamento"
                  onPress={() => root?.navigate('BookingDocuments', { booking: item })}
                  secondary
                />
                {item.category === 'ACTIVE' && (
                  <PrimaryButton label="Confirmar retirada (check-out)" onPress={() => checkout(item)} />
                )}
                {item.reviewAvailable && (
                  <PrimaryButton label="Avaliar experiência" onPress={() => root?.navigate('Reviews')} />
                )}
              </View>
            </View>
          )}
        />
      )}
    </SafeAreaView>
  );
}

function Header() {
  return (
    <View style={styles.header}>
      <BrandMark compact />
      <Text style={styles.headerTitle}>Reservas</Text>
    </View>
  );
}
function Guest({ onLogin }: { onLogin: () => void }) {
  return (
    <View style={styles.empty}>
      <Text style={styles.emptyIcon}>▣</Text>
      <Text style={styles.title}>Acesse suas reservas</Text>
      <Text style={styles.subtitle}>Entre para acompanhar datas, pagamentos e contratos.</Text>
      <View style={styles.button}>
        <PrimaryButton label="Entrar na conta" onPress={onLogin} />
      </View>
    </View>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    height: 62,
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  headerTitle: { fontSize: 20, fontWeight: '800', color: colors.text },
  categories: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, padding: 10, backgroundColor: '#fff' },
  category: {
    paddingHorizontal: 10,
    paddingVertical: 7,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.line,
  },
  categoryOn: { backgroundColor: colors.navy },
  categoryText: { fontSize: 10, fontWeight: '800', color: colors.text },
  categoryTextOn: { color: '#fff' },
  loader: { flex: 1 },
  list: { padding: 18, gap: 12 },
  emptyList: { flexGrow: 1 },
  empty: { flex: 1, padding: 28, alignItems: 'center', justifyContent: 'center' },
  emptyIcon: { fontSize: 44, color: colors.orange, marginBottom: 18 },
  title: { fontSize: 24, fontWeight: '800', color: colors.text, textAlign: 'center' },
  subtitle: {
    fontSize: 15,
    lineHeight: 22,
    color: colors.muted,
    textAlign: 'center',
    maxWidth: 320,
    marginTop: 8,
  },
  button: { width: '100%', marginTop: 24 },
  card: { backgroundColor: '#fff', borderRadius: 18, padding: 18, borderWidth: 1, borderColor: colors.line },
  row: { flexDirection: 'row', gap: 8 },
  cardTitle: { flex: 1, fontSize: 17, fontWeight: '800', color: colors.text },
  status: { color: colors.success, fontWeight: '800', fontSize: 12 },
  meta: { color: colors.muted, marginTop: 10 },
  price: { color: colors.navy, fontWeight: '900', fontSize: 18, marginTop: 12 },
  contract: { color: colors.muted, fontSize: 12, marginTop: 4 },
  manage: { marginTop: 14 },
});
