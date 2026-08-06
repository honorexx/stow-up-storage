import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import { API_ORIGIN } from '../../core/api/auth';
import { checkoutBooking, getOwnerDashboard, OwnerDashboard } from '../../core/api/marketplace';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

type ActiveRental = OwnerDashboard['activeRentals'][number];

export function OwnerRentalScreen({
  navigation,
  route,
}: NativeStackScreenProps<RootStackParamList, 'OwnerRental'>) {
  const { token } = useAuth();
  const [rental, setRental] = useState<ActiveRental | null>(null);
  const [loading, setLoading] = useState(true);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');

  const refresh = useCallback(async () => {
    if (!token) {
      setError('Entre na sua conta para acompanhar esta locação.');
      setLoading(false);
      return;
    }
    try {
      const dashboard = await getOwnerDashboard(token);
      const current = dashboard.activeRentals.find((item) => item.bookingId === route.params.bookingId);
      setRental(current ?? null);
      setError(current ? '' : 'Esta locação não está mais ativa.');
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : 'Não foi possível atualizar a locação.',
      );
    } finally {
      setLoading(false);
    }
  }, [route.params.bookingId, token]);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 5000);
    return () => clearInterval(timer);
  }, [refresh]);

  function requestCheckoutConfirmation() {
    if (!rental || rental.ownerConfirmed) return;
    Alert.alert(
      'Confirmar retirada dos itens?',
      rental.customerConfirmed
        ? 'O cliente já informou a retirada. Sua confirmação encerrará a locação e liberará o espaço.'
        : 'Sua confirmação será registrada. A locação será encerrada quando o cliente também confirmar a retirada.',
      [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Confirmar retirada', onPress: confirmCheckout },
      ],
    );
  }

  async function confirmCheckout() {
    if (!token || !rental) return;
    setConfirming(true);
    try {
      const result = await checkoutBooking(token, rental.bookingId);
      if (result.category === 'COMPLETED') {
        Alert.alert(
          'Locação encerrada',
          'O espaço foi liberado e as avaliações ficarão disponíveis por 14 dias.',
          [{ text: 'OK', onPress: () => navigation.goBack() }],
        );
      } else {
        Alert.alert('Confirmação registrada', 'Agora falta o cliente confirmar a retirada no aplicativo.');
        await refresh();
      }
    } catch (requestError) {
      Alert.alert(
        'Não foi possível confirmar',
        requestError instanceof Error ? requestError.message : 'Tente novamente.',
      );
    } finally {
      setConfirming(false);
    }
  }

  const photoUri = rental?.customerPhotoUrl
    ? rental.customerPhotoUrl.startsWith('http')
      ? rental.customerPhotoUrl
      : `${API_ORIGIN}${rental.customerPhotoUrl}`
    : null;

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Pressable onPress={navigation.goBack}>
          <Text style={styles.back}>‹ Voltar</Text>
        </Pressable>
        <Text style={styles.headerTitle}>Locação do espaço</Text>
        <View style={{ width: 52 }} />
      </View>

      {loading && !rental ? (
        <View style={styles.center}>
          <ActivityIndicator color={colors.orange} size="large" />
          <Text style={styles.muted}>Carregando dados da locação...</Text>
        </View>
      ) : !rental ? (
        <View style={styles.center}>
          <Text style={styles.emptyTitle}>Locação indisponível</Text>
          <Text style={styles.muted}>{error}</Text>
          <Pressable style={styles.secondaryButton} onPress={refresh}>
            <Text style={styles.secondaryButtonText}>Atualizar</Text>
          </Pressable>
        </View>
      ) : (
        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.hero}>
            <View style={styles.occupiedBadge}>
              <Text style={styles.occupiedText}>ESPAÇO OCUPADO</Text>
            </View>
            <Text style={styles.spaceTitle}>{rental.spaceTitle}</Text>
            <Text style={styles.bookingCode}>Reserva {rental.bookingId}</Text>
          </View>

          <Text style={styles.sectionTitle}>Quem está alugando</Text>
          <View style={styles.card}>
            <View style={styles.personRow}>
              {photoUri ? (
                <Image source={{ uri: photoUri }} style={styles.avatar} />
              ) : (
                <View style={styles.avatarFallback}>
                  <Text style={styles.avatarText}>{initials(rental.customer)}</Text>
                </View>
              )}
              <View style={{ flex: 1 }}>
                <Text style={styles.customerName}>{rental.customer}</Text>
                <Text style={styles.detail}>{rental.customerEmail}</Text>
                <Text style={styles.detail}>{rental.customerPhone || 'Telefone não informado'}</Text>
              </View>
            </View>
          </View>

          <Text style={styles.sectionTitle}>Resumo da locação</Text>
          <View style={styles.card}>
            <Detail label="Início" value={formatDate(rental.start)} />
            <Detail label="Término previsto" value={formatDate(rental.end)} />
            <Detail label="Valor contratado" value={formatMoney(rental.total)} />
            <Detail label="Situação do espaço" value="Ocupado" />
          </View>

          <Text style={styles.sectionTitle}>Confirmação da retirada</Text>
          <View style={styles.card}>
            <ConfirmationRow
              label="Cliente"
              confirmed={rental.customerConfirmed}
              pendingText="Ainda não confirmou a retirada"
            />
            <View style={styles.divider} />
            <ConfirmationRow
              label="Proprietário"
              confirmed={rental.ownerConfirmed}
              pendingText="Aguardando sua confirmação"
            />
            <Text style={styles.checkoutHelp}>
              Em uma retirada antecipada, cliente e proprietário precisam confirmar. Quando as duas
              confirmações forem registradas, o espaço será liberado automaticamente.
            </Text>
            {!rental.ownerConfirmed && (
              <Pressable
                disabled={confirming}
                style={[styles.primaryButton, confirming && { opacity: 0.6 }]}
                onPress={requestCheckoutConfirmation}
              >
                {confirming ? (
                  <ActivityIndicator color="#fff" />
                ) : (
                  <Text style={styles.primaryButtonText}>Confirmar retirada dos itens</Text>
                )}
              </Pressable>
            )}
            {rental.ownerConfirmed && (
              <View style={styles.confirmedBox}>
                <Text style={styles.confirmedBoxText}>
                  Sua confirmação já foi registrada. A tela será atualizada automaticamente.
                </Text>
              </View>
            )}
          </View>

          {!!error && <Text style={styles.warning}>{error}</Text>}
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.detailRow}>
      <Text style={styles.detailLabel}>{label}</Text>
      <Text style={styles.detailValue}>{value}</Text>
    </View>
  );
}

function ConfirmationRow({
  label,
  confirmed,
  pendingText,
}: {
  label: string;
  confirmed: boolean;
  pendingText: string;
}) {
  return (
    <View style={styles.confirmationRow}>
      <View style={[styles.check, confirmed ? styles.checkDone : styles.checkPending]}>
        <Text style={styles.checkText}>{confirmed ? '✓' : '…'}</Text>
      </View>
      <View style={{ flex: 1 }}>
        <Text style={styles.confirmationTitle}>{label}</Text>
        <Text style={styles.detail}>{confirmed ? 'Retirada confirmada' : pendingText}</Text>
      </View>
    </View>
  );
}

function formatDate(value: string) {
  return new Date(`${value}T12:00:00`).toLocaleDateString('pt-BR');
}

function formatMoney(value: number) {
  return Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function initials(name: string) {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    height: 62,
    paddingHorizontal: 18,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
    backgroundColor: '#fff',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  back: { color: colors.navy, fontWeight: '800' },
  headerTitle: { color: colors.text, fontWeight: '900', fontSize: 17 },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 30,
    gap: 14,
  },
  content: { padding: 18, paddingBottom: 45 },
  hero: {
    borderRadius: 20,
    padding: 20,
    backgroundColor: colors.navy,
  },
  occupiedBadge: {
    alignSelf: 'flex-start',
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 5,
    backgroundColor: colors.orange,
  },
  occupiedText: { color: '#fff', fontSize: 10, fontWeight: '900', letterSpacing: 0.5 },
  spaceTitle: { color: '#fff', fontSize: 23, fontWeight: '900', marginTop: 14 },
  bookingCode: { color: '#D5E2F0', marginTop: 5 },
  sectionTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '900',
    marginTop: 23,
    marginBottom: 9,
  },
  card: {
    borderRadius: 18,
    padding: 17,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: colors.line,
  },
  personRow: { flexDirection: 'row', alignItems: 'center', gap: 13 },
  avatar: { width: 58, height: 58, borderRadius: 29, backgroundColor: colors.orangeSoft },
  avatarFallback: {
    width: 58,
    height: 58,
    borderRadius: 29,
    backgroundColor: colors.orangeSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: { color: colors.orange, fontWeight: '900', fontSize: 18 },
  customerName: { color: colors.text, fontWeight: '900', fontSize: 17 },
  detail: { color: colors.muted, marginTop: 3, lineHeight: 19 },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 9,
    gap: 16,
  },
  detailLabel: { color: colors.muted, flex: 1 },
  detailValue: { color: colors.text, fontWeight: '800', textAlign: 'right', flex: 1 },
  confirmationRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  check: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkDone: { backgroundColor: '#E4F7EE' },
  checkPending: { backgroundColor: '#FFF3E8' },
  checkText: { color: colors.navy, fontWeight: '900', fontSize: 18 },
  confirmationTitle: { color: colors.text, fontWeight: '900' },
  divider: { height: 1, backgroundColor: colors.line, marginVertical: 15 },
  checkoutHelp: { color: colors.muted, lineHeight: 20, marginTop: 17 },
  primaryButton: {
    minHeight: 50,
    borderRadius: 14,
    backgroundColor: colors.orange,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
    marginTop: 16,
  },
  primaryButtonText: { color: '#fff', fontWeight: '900', textAlign: 'center' },
  confirmedBox: {
    borderRadius: 13,
    backgroundColor: '#EAF7F0',
    padding: 13,
    marginTop: 16,
  },
  confirmedBoxText: { color: colors.success, fontWeight: '800', lineHeight: 19 },
  muted: { color: colors.muted, textAlign: 'center', lineHeight: 21 },
  emptyTitle: { color: colors.text, fontWeight: '900', fontSize: 21 },
  secondaryButton: {
    borderWidth: 1,
    borderColor: colors.navy,
    borderRadius: 13,
    paddingHorizontal: 22,
    paddingVertical: 11,
  },
  secondaryButtonText: { color: colors.navy, fontWeight: '900' },
  warning: { color: colors.error, textAlign: 'center', marginTop: 15 },
});
