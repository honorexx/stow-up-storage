import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useRef, useState } from 'react';
import { Alert, Pressable, SafeAreaView, StyleSheet, Text, TextInput, View } from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import { createBooking } from '../../core/api/marketplace';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

function suggestedDate() {
  const date = new Date();
  date.setDate(date.getDate() + 2);
  return date.toISOString().slice(0, 10);
}

export function ReserveSpaceScreen({
  navigation,
  route,
}: NativeStackScreenProps<RootStackParamList, 'ReserveSpace'>) {
  const { token } = useAuth();
  const [start, setStart] = useState(suggestedDate());
  const [months, setMonths] = useState(1);
  const [loading, setLoading] = useState(false);
  const submitting = useRef(false);
  const subtotal = route.params.price * months;
  const total = subtotal * 1.08;
  async function continueFlow() {
    if (submitting.current) return;
    if (!token) return navigation.navigate('Login');
    if (!/^\d{4}-\d{2}-\d{2}$/.test(start)) return Alert.alert('Data inválida', 'Use o formato AAAA-MM-DD.');
    submitting.current = true;
    setLoading(true);
    try {
      const booking = await createBooking(token, { spaceId: route.params.spaceId, start, months });
      navigation.replace('BookingDocuments', { booking });
    } catch (e: any) {
      Alert.alert('Não foi possível reservar', e.message);
    } finally {
      submitting.current = false;
      setLoading(false);
    }
  }
  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()}>
          <Text style={styles.back}>‹</Text>
        </Pressable>
        <Text style={styles.headerTitle}>Solicitar reserva</Text>
      </View>
      <View style={styles.content}>
        <Text style={styles.title}>{route.params.title}</Text>
        <Text style={styles.subtitle}>
          Confira o período. O espaço só será liberado depois do pagamento e da validação do contrato
          assinado.
        </Text>
        <View style={styles.card}>
          <Text style={styles.label}>Data de início</Text>
          <TextInput value={start} onChangeText={setStart} placeholder="AAAA-MM-DD" style={styles.input} />
          <Text style={styles.hint}>Informe no formato ano-mês-dia.</Text>
          <Text style={styles.label}>Período</Text>
          <View style={styles.counter}>
            <Pressable
              onPress={() => setMonths((value) => Math.max(1, value - 1))}
              style={styles.counterButton}
            >
              <Text style={styles.counterText}>−</Text>
            </Pressable>
            <Text style={styles.months}>
              {months} {months === 1 ? 'mês' : 'meses'}
            </Text>
            <Pressable
              onPress={() => setMonths((value) => Math.min(24, value + 1))}
              style={styles.counterButton}
            >
              <Text style={styles.counterText}>+</Text>
            </Pressable>
          </View>
        </View>
        <View style={styles.summary}>
          <View style={styles.line}>
            <Text style={styles.muted}>Locação</Text>
            <Text>R$ {subtotal.toFixed(2).replace('.', ',')}</Text>
          </View>
          <View style={styles.line}>
            <Text style={styles.muted}>Taxa StowUp (8%)</Text>
            <Text>R$ {(subtotal * 0.08).toFixed(2).replace('.', ',')}</Text>
          </View>
          <View style={[styles.line, styles.total]}>
            <Text style={styles.totalText}>Total</Text>
            <Text style={styles.totalText}>R$ {total.toFixed(2).replace('.', ',')}</Text>
          </View>
        </View>
        <PrimaryButton label="Gerar reserva e contrato" onPress={continueFlow} loading={loading} />
      </View>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    minHeight: 66,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingHorizontal: 18,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  back: { fontSize: 38, lineHeight: 42, color: colors.text },
  headerTitle: { color: colors.text, fontWeight: '900', fontSize: 19 },
  content: { padding: 20, gap: 18 },
  title: { color: colors.text, fontWeight: '900', fontSize: 23 },
  subtitle: { color: colors.muted, lineHeight: 21 },
  card: {
    backgroundColor: '#fff',
    borderRadius: 18,
    padding: 17,
    gap: 10,
    borderWidth: 1,
    borderColor: colors.line,
  },
  label: { color: colors.text, fontWeight: '800', marginTop: 4 },
  input: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 13,
    height: 48,
    paddingHorizontal: 14,
    color: colors.text,
  },
  hint: { color: colors.muted, fontSize: 11 },
  counter: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  counterButton: {
    width: 42,
    height: 42,
    borderRadius: 13,
    borderWidth: 1,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
  },
  counterText: { color: colors.navy, fontWeight: '900', fontSize: 23 },
  months: { color: colors.text, fontWeight: '900', fontSize: 17 },
  summary: { gap: 10 },
  line: { flexDirection: 'row', justifyContent: 'space-between' },
  muted: { color: colors.muted },
  total: { borderTopWidth: 1, borderTopColor: colors.line, paddingTop: 12 },
  totalText: { color: colors.navy, fontWeight: '900', fontSize: 18 },
});
