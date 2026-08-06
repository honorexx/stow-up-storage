import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import {
  checkoutBooking,
  FinancialReport,
  getFinancialReport,
  getOwnerDashboard,
  OwnerDashboard,
} from '../../core/api/marketplace';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

export function OwnerDashboardScreen({
  navigation,
}: NativeStackScreenProps<RootStackParamList, 'OwnerDashboard'>) {
  const { token } = useAuth();
  const [data, setData] = useState<OwnerDashboard | null>(null);
  const [report, setReport] = useState<FinancialReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reportError, setReportError] = useState(false);

  const refresh = useCallback(async () => {
    if (!token) {
      setError('Entre na sua conta para abrir o painel do proprietário.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError('');
    setReportError(false);
    try {
      const dashboard = await getOwnerDashboard(token);
      setData(dashboard);

      try {
        setReport(await getFinancialReport(token));
      } catch {
        setReport(null);
        setReportError(true);
      }
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'Não foi possível carregar o painel do proprietário.',
      );
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 8000);
    return () => clearInterval(timer);
  }, [refresh]);

  async function confirmCheckout(bookingId: string) {
    if (!token) return;
    try {
      const result = await checkoutBooking(token, bookingId);
      Alert.alert(
        result.category === 'COMPLETED' ? 'Locação encerrada' : 'Confirmação registrada',
        result.category === 'COMPLETED'
          ? 'As avaliações foram liberadas por 14 dias.'
          : 'Aguardando a confirmação do cliente.',
      );
      await refresh();
    } catch (error: any) {
      Alert.alert('Check-out', error.message);
    }
  }

  if (!data)
    return (
      <SafeAreaView style={s.safe}>
        <DashboardHeader onBack={navigation.goBack} />
        <View style={s.statusContainer}>
          {loading ? (
            <>
              <ActivityIndicator color={colors.orange} size="large" />
              <Text style={s.statusText}>Carregando seu painel...</Text>
            </>
          ) : (
            <>
              <Text style={s.errorTitle}>Não foi possível abrir o painel</Text>
              <Text style={s.statusText}>{error}</Text>
              <Pressable style={s.retry} onPress={refresh}>
                <Text style={s.retryText}>Tentar novamente</Text>
              </Pressable>
            </>
          )}
        </View>
      </SafeAreaView>
    );
  return (
    <SafeAreaView style={s.safe}>
      <DashboardHeader onBack={navigation.goBack} />
      <ScrollView contentContainerStyle={s.content}>
        {!!error && <Text style={s.reportWarning}>{error}</Text>}
        {reportError && (
          <Text style={s.reportWarning}>O relatório financeiro não pôde ser atualizado agora.</Text>
        )}
        <View style={s.grid}>
          <Metric label="Espaços ativos" value={data.activeSpaces} />
          <Metric label="Ocupados" value={data.occupiedSpaces} />
          <Metric label="Reservas pendentes" value={data.pendingReservations} />
          <Metric label="Contratos ativos" value={data.activeContracts} />
          <Metric label="Receita do mês" value={`R$ ${Number(data.monthRevenue).toFixed(2)}`} />
          <Metric label="Receita total" value={`R$ ${Number(data.totalRevenue).toFixed(2)}`} />
          <Metric label="Taxa de ocupação" value={`${data.occupancyRate}%`} />
          <Metric label="Contratos concluídos" value={report?.completedContracts ?? 0} />
        </View>

        <Text style={s.section}>Locações confirmadas dos meus espaços</Text>
        {data.activeRentals.length ? (
          data.activeRentals.map((item) => (
            <View key={item.bookingId} style={s.row}>
              <Text style={s.rowTitle}>{item.spaceTitle}</Text>
              <Text style={s.muted}>
                {item.customer} · início {new Date(`${item.start}T12:00:00`).toLocaleDateString('pt-BR')} ·
                término {new Date(`${item.end}T12:00:00`).toLocaleDateString('pt-BR')}
              </Text>
              <Text style={s.muted}>
                {item.customerConfirmed
                  ? 'Cliente confirmou a retirada'
                  : 'Retirada ainda não confirmada pelo cliente'}
              </Text>
              <Pressable
                style={s.details}
                onPress={() => navigation.navigate('OwnerRental', { bookingId: item.bookingId })}
              >
                <Text style={s.detailsText}>Ver locatário e acompanhar locação</Text>
              </Pressable>
              {!item.ownerConfirmed && (
                <Pressable style={s.checkout} onPress={() => confirmCheckout(item.bookingId)}>
                  <Text style={s.checkoutText}>Confirmar retirada dos itens</Text>
                </Pressable>
              )}
            </View>
          ))
        ) : (
          <Text style={s.empty}>Nenhuma locação confirmada para seus espaços.</Text>
        )}

        <Text style={s.section}>Próximos contratos</Text>
        {data.upcomingContracts.length ? (
          data.upcomingContracts.map((item) => (
            <View key={item.bookingId} style={s.row}>
              <Text style={s.rowTitle}>{item.spaceTitle}</Text>
              <Text style={s.muted}>
                {new Date(`${item.start}T12:00:00`).toLocaleDateString('pt-BR')} · {item.customer}
              </Text>
              <Text style={s.money}>R$ {Number(item.total).toFixed(2)}</Text>
            </View>
          ))
        ) : (
          <Text style={s.empty}>Nenhum contrato futuro confirmado.</Text>
        )}

        <Text style={s.section}>Histórico financeiro mensal</Text>
        {Object.entries(report?.monthlyRevenue ?? {}).map(([month, value]) => (
          <View key={month} style={s.month}>
            <Text style={s.rowTitle}>{month}</Text>
            <Text style={s.money}>R$ {Number(value).toFixed(2)}</Text>
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

function DashboardHeader({ onBack }: { onBack: () => void }) {
  return (
    <View style={s.header}>
      <Text onPress={onBack} style={s.back}>
        ‹ Voltar
      </Text>
      <Text style={s.title}>Painel do proprietário</Text>
      <View style={{ width: 50 }} />
    </View>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <View style={s.metric}>
      <Text style={s.metricValue}>{value}</Text>
      <Text style={s.metricLabel}>{label}</Text>
    </View>
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
  title: { fontSize: 18, fontWeight: '900', color: colors.text },
  statusContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 30,
    gap: 14,
  },
  statusText: { color: colors.muted, textAlign: 'center', lineHeight: 21 },
  errorTitle: { color: colors.text, fontSize: 20, fontWeight: '900', textAlign: 'center' },
  retry: {
    marginTop: 6,
    borderRadius: 14,
    backgroundColor: colors.navy,
    paddingHorizontal: 24,
    paddingVertical: 13,
  },
  retryText: { color: '#fff', fontWeight: '900' },
  reportWarning: {
    color: colors.muted,
    backgroundColor: colors.orangeSoft,
    borderRadius: 12,
    padding: 12,
    marginBottom: 12,
  },
  content: { padding: 16, paddingBottom: 40 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  metric: {
    width: '48%',
    backgroundColor: '#fff',
    borderRadius: 17,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.line,
  },
  metricValue: { fontSize: 20, fontWeight: '900', color: colors.navy },
  metricLabel: { fontSize: 11, color: colors.muted, marginTop: 5 },
  section: { fontSize: 19, fontWeight: '900', color: colors.text, marginTop: 25, marginBottom: 10 },
  row: {
    backgroundColor: '#fff',
    padding: 15,
    borderRadius: 15,
    borderWidth: 1,
    borderColor: colors.line,
    marginBottom: 9,
  },
  rowTitle: { fontWeight: '900', color: colors.text },
  muted: { color: colors.muted, marginTop: 4 },
  money: { color: colors.orange, fontWeight: '900', marginTop: 7 },
  details: {
    marginTop: 12,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.navy,
    padding: 12,
    alignItems: 'center',
  },
  detailsText: { color: colors.navy, fontWeight: '900', textAlign: 'center' },
  checkout: {
    marginTop: 12,
    borderRadius: 12,
    backgroundColor: colors.navy,
    padding: 12,
    alignItems: 'center',
  },
  checkoutText: { color: '#fff', fontWeight: '900' },
  empty: { color: colors.muted, padding: 16, textAlign: 'center' },
  month: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    backgroundColor: '#fff',
    padding: 14,
    borderRadius: 14,
    marginBottom: 8,
  },
});
