import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, Pressable, SafeAreaView, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import { createReview, EligibleReview, eligibleReviews } from '../../core/api/marketplace';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';
export function ReviewsScreen({ navigation }: NativeStackScreenProps<RootStackParamList, 'Reviews'>) {
  const { token } = useAuth();
  const [items, setItems] = useState<EligibleReview[]>([]);
  const [rating, setRating] = useState<Record<string, number>>({});
  const [comment, setComment] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState('');
  function refresh() {
    if (token) eligibleReviews(token).then(setItems);
  }
  useEffect(refresh, [token]);
  async function send(id: string) {
    if (!token || !rating[id]) return Alert.alert('Escolha uma nota', 'Selecione de 1 a 5 estrelas.');
    setBusy(id);
    try {
      await createReview(token, id, rating[id], comment[id] || '');
      Alert.alert('Avaliação enviada', 'Obrigado por compartilhar a experiência.');
      refresh();
    } catch (e: any) {
      Alert.alert('Avaliação', e.message);
    } finally {
      setBusy('');
    }
  }
  return (
    <SafeAreaView style={s.safe}>
      <View style={s.header}>
        <Text onPress={navigation.goBack} style={s.back}>
          ‹ Voltar
        </Text>
        <Text style={s.title}>Avaliações</Text>
        <View style={{ width: 50 }} />
      </View>
      <ScrollView contentContainerStyle={s.content}>
        {items.length ? (
          items.map((i) => (
            <View key={i.bookingId} style={s.card}>
              <Text style={s.cardTitle}>{i.spaceTitle}</Text>
              <Text style={s.muted}>
                Disponível até {i.deadline ? new Date(i.deadline).toLocaleDateString('pt-BR') : '—'}
              </Text>
              <View style={s.stars}>
                {[1, 2, 3, 4, 5].map((n) => (
                  <Pressable key={n} onPress={() => setRating((v) => ({ ...v, [i.bookingId]: n }))}>
                    <Text style={[s.star, (rating[i.bookingId] || 0) >= n && s.starOn]}>★</Text>
                  </Pressable>
                ))}
              </View>
              <TextInput
                multiline
                value={comment[i.bookingId] || ''}
                onChangeText={(v) => setComment((c) => ({ ...c, [i.bookingId]: v }))}
                placeholder="Conte como foi toda a experiência"
                style={s.input}
              />
              <PrimaryButton
                label="Enviar avaliação"
                onPress={() => send(i.bookingId)}
                loading={busy === i.bookingId}
              />
            </View>
          ))
        ) : (
          <View style={s.empty}>
            <Text style={s.emptyTitle}>Nenhuma avaliação disponível</Text>
            <Text style={s.muted}>
              As avaliações são liberadas somente após o check-out e ficam disponíveis por 14 dias.
            </Text>
          </View>
        )}
      </ScrollView>
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
  },
  back: { color: colors.navy, fontWeight: '800' },
  title: { fontSize: 19, fontWeight: '900', color: colors.text },
  content: { padding: 16, flexGrow: 1 },
  card: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 18,
    padding: 17,
    marginBottom: 12,
  },
  cardTitle: { fontSize: 18, fontWeight: '900', color: colors.text },
  muted: { color: colors.muted, lineHeight: 20, marginTop: 5 },
  stars: { flexDirection: 'row', gap: 5, marginVertical: 14 },
  star: { fontSize: 31, color: '#DCE2E8' },
  starOn: { color: '#FF8A3D' },
  input: {
    minHeight: 90,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    padding: 12,
    textAlignVertical: 'top',
    marginBottom: 12,
    color: colors.text,
  },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 30 },
  emptyTitle: { fontSize: 21, fontWeight: '900', color: colors.text },
});
