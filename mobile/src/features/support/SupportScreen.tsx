import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { useFocusEffect } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { MainTabParamList, RootStackParamList } from '../../app/navigation/types';
import { listSupportTickets, SupportTicket } from '../../core/api/marketplace';
import { BrandMark } from '../../shared/components/BrandMark';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

const categoryLabels: Record<SupportTicket['category'], string> = {
  PAYMENT: 'Pagamento',
  LISTING: 'Meu anúncio',
  BOOKING: 'Reserva',
  CONTRACT: 'Contrato',
  SECURITY: 'Segurança',
  ACCOUNT: 'Minha conta',
  OTHER: 'Outros',
};

export function SupportScreen({ navigation }: BottomTabScreenProps<MainTabParamList, 'Suporte'>) {
  const auth = useAuth();
  const root = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const refresh = useCallback(() => {
    if (!auth.token) return;
    setLoading(true);
    setError('');
    listSupportTickets(auth.token)
      .then(setTickets)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [auth.token]);
  useFocusEffect(refresh);
  useEffect(() => {
    if (!auth.token) return;
    const timer = setInterval(
      () =>
        listSupportTickets(auth.token!)
          .then(setTickets)
          .catch(() => undefined),
      8000,
    );
    return () => clearInterval(timer);
  }, [auth.token]);

  if (!auth.user)
    return (
      <SafeAreaView style={styles.safe}>
        <Header />
        <View style={styles.guest}>
          <Text style={styles.helpIcon}>?</Text>
          <Text style={styles.title}>Como podemos ajudar?</Text>
          <Text style={styles.subtitle}>
            Entre na sua conta para abrir e acompanhar chamados com segurança.
          </Text>
          <View style={styles.full}>
            <PrimaryButton label="Entrar para falar com o suporte" onPress={() => root?.navigate('Login')} />
          </View>
        </View>
      </SafeAreaView>
    );

  return (
    <SafeAreaView style={styles.safe}>
      <Header />
      <View style={styles.intro}>
        <View style={{ flex: 1 }}>
          <Text style={styles.titleLeft}>Central de ajuda</Text>
          <Text style={styles.subtitleLeft}>Acompanhe seus chamados ou solicite atendimento.</Text>
        </View>
        <Pressable style={styles.newButton} onPress={() => root?.navigate('NewSupportTicket')}>
          <Text style={styles.newButtonText}>+ Novo</Text>
        </Pressable>
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}
      {loading ? (
        <ActivityIndicator style={{ flex: 1 }} color={colors.orange} />
      ) : (
        <FlatList
          data={tickets}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>?</Text>
              <Text style={styles.title}>Nenhum chamado ainda</Text>
              <Text style={styles.subtitle}>
                Quando precisar, abra um chamado e escolha o assunto. Todo o histórico ficará aqui.
              </Text>
            </View>
          }
          renderItem={({ item }) => (
            <Pressable
              style={styles.ticket}
              onPress={() =>
                root?.navigate('SupportTicketChat', {
                  ticketId: item.id,
                  ticketNumber: item.ticketNumber,
                  subject: item.subject,
                  status: item.status,
                })
              }
            >
              <View style={styles.ticketTop}>
                <Text style={styles.protocol}>{item.ticketNumber}</Text>
                <View style={[styles.status, item.status === 'CLOSED' && styles.closed]}>
                  <Text style={styles.statusText}>
                    {item.status === 'OPEN' ? 'Em atendimento' : 'Encerrado'}
                  </Text>
                </View>
              </View>
              <Text style={styles.ticketTitle}>{item.subject}</Text>
              <Text style={styles.category}>{categoryLabels[item.category]}</Text>
              <Text numberOfLines={2} style={styles.preview}>
                {item.lastMessage}
              </Text>
              <Text style={styles.date}>
                Atualizado em {new Date(item.updatedAt).toLocaleDateString('pt-BR')} às{' '}
                {new Date(item.updatedAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
              </Text>
            </Pressable>
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
      <View>
        <Text style={styles.headerTitle}>Suporte StowUp</Text>
        <Text style={styles.online}>● Equipe de atendimento</Text>
      </View>
    </View>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    minHeight: 64,
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  headerTitle: { fontWeight: '800', color: colors.text, fontSize: 17 },
  online: { color: colors.success, fontSize: 11, marginTop: 2 },
  intro: { padding: 18, flexDirection: 'row', alignItems: 'center', gap: 12 },
  titleLeft: { color: colors.text, fontSize: 23, fontWeight: '900' },
  subtitleLeft: { color: colors.muted, marginTop: 3, lineHeight: 18 },
  newButton: { backgroundColor: colors.orange, borderRadius: 14, paddingHorizontal: 16, paddingVertical: 12 },
  newButtonText: { color: '#fff', fontWeight: '900' },
  list: { paddingHorizontal: 16, paddingBottom: 30, gap: 12, flexGrow: 1 },
  ticket: {
    backgroundColor: '#fff',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.line,
  },
  ticketTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  protocol: { color: colors.orange, fontWeight: '900', fontSize: 12 },
  status: { backgroundColor: '#E7F7EE', paddingHorizontal: 9, paddingVertical: 5, borderRadius: 20 },
  closed: { backgroundColor: '#EEF1F5' },
  statusText: { color: colors.text, fontSize: 10, fontWeight: '800' },
  ticketTitle: { color: colors.text, fontWeight: '900', fontSize: 17, marginTop: 10 },
  category: { color: colors.orange, fontSize: 12, fontWeight: '700', marginTop: 3 },
  preview: { color: colors.muted, lineHeight: 19, marginTop: 9 },
  date: { color: colors.muted, fontSize: 10, marginTop: 12 },
  guest: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 28 },
  empty: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 28 },
  emptyIcon: {
    width: 62,
    height: 62,
    textAlign: 'center',
    textAlignVertical: 'center',
    borderRadius: 31,
    overflow: 'hidden',
    backgroundColor: '#FFF0E8',
    color: colors.orange,
    fontSize: 36,
    fontWeight: '900',
    marginBottom: 15,
  },
  helpIcon: { fontSize: 55, color: colors.orange, marginBottom: 16 },
  title: { color: colors.text, fontSize: 23, fontWeight: '800', textAlign: 'center' },
  subtitle: {
    color: colors.muted,
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
    marginTop: 8,
    maxWidth: 330,
  },
  full: { width: '100%', marginTop: 24 },
  error: { color: '#B42318', marginHorizontal: 18, marginBottom: 10 },
});
