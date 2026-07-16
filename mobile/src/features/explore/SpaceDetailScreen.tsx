import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Image,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import { API_ORIGIN } from '../../core/api/auth';
import {
  addFavorite,
  favoriteStatus,
  getSpace,
  removeFavorite,
  Space,
  spaceAvailability,
  startConversation,
} from '../../core/api/marketplace';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

export function SpaceDetailScreen({
  navigation,
  route,
}: NativeStackScreenProps<RootStackParamList, 'SpaceDetail'>) {
  const auth = useAuth();
  const [space, setSpace] = useState<Space | null>(null);
  const [error, setError] = useState('');
  const [starting, setStarting] = useState(false);
  const [favorite, setFavorite] = useState(false);
  const [periods, setPeriods] = useState<{ start: string; end: string }[]>([]);
  const { width } = useWindowDimensions();
  useEffect(() => {
    getSpace(route.params.spaceId)
      .then(setSpace)
      .catch((e) => setError(e.message));
    spaceAvailability(route.params.spaceId)
      .then(setPeriods)
      .catch(() => undefined);
    if (auth.token)
      favoriteStatus(auth.token, route.params.spaceId)
        .then((v) => setFavorite(v.favorite))
        .catch(() => undefined);
  }, [route.params.spaceId, auth.token]);
  async function toggleFavorite() {
    if (!auth.token) return navigation.navigate('Login');
    if (favorite) await removeFavorite(auth.token, route.params.spaceId);
    else await addFavorite(auth.token, route.params.spaceId);
    setFavorite(!favorite);
  }
  async function talk() {
    if (!auth.token) return navigation.navigate('Login');
    setStarting(true);
    setError('');
    try {
      const conversation = await startConversation(auth.token, route.params.spaceId);
      navigation.navigate('Conversation', {
        conversationId: conversation.id,
        title: conversation.participantName,
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível iniciar a conversa.');
    } finally {
      setStarting(false);
    }
  }
  function reserve() {
    if (!auth.token) return navigation.navigate('Login');
    if (space)
      navigation.navigate('ReserveSpace', {
        spaceId: space.id,
        title: space.title,
        price: Number(space.price),
      });
  }
  if (!space)
    return (
      <SafeAreaView style={styles.safe}>
        <Text onPress={navigation.goBack} style={styles.back}>
          ‹ Voltar
        </Text>
        {error ? (
          <Text style={styles.error}>{error}</Text>
        ) : (
          <ActivityIndicator style={{ flex: 1 }} color={colors.orange} />
        )}
      </SafeAreaView>
    );
  const rating = space.reviews
    ? `★ ${space.rating.toFixed(1)} · ${space.reviews} avaliações`
    : 'Novo · ainda sem avaliações';
  const mine = auth.user?.id === space.ownerId;
  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.top}>
          <Text onPress={navigation.goBack} style={styles.back}>
            ‹ Voltar
          </Text>
          {!mine && (
            <Text onPress={toggleFavorite} style={styles.heart}>
              {favorite ? '♥' : '♡'}
            </Text>
          )}
        </View>
        <ScrollView horizontal pagingEnabled showsHorizontalScrollIndicator={false} style={{ flexGrow: 0 }}>
          {space.photos.length ? (
            space.photos.map((photo) => (
              <Image
                key={photo}
                source={{ uri: `${API_ORIGIN}${photo}` }}
                style={[styles.photo, { width }]}
              />
            ))
          ) : (
            <View style={[styles.photo, styles.placeholder, { width }]}>
              <Text style={styles.placeholderText}>StowUp</Text>
            </View>
          )}
        </ScrollView>
        <View style={styles.body}>
          {mine && (
            <View style={styles.mine}>
              <Text style={styles.mineText}>Este anúncio é seu</Text>
            </View>
          )}
          <Text style={styles.type}>
            {space.type} · {space.size} m²
          </Text>
          <Text style={styles.title}>{space.title}</Text>
          <Text style={styles.location}>
            {space.district}, {space.city}
            {space.distance > 0 ? ` · ${space.distance.toFixed(1)} km de você` : ''}
          </Text>
          <Text style={styles.rating}>{rating}</Text>
          {space.ownerVerified && <Text style={styles.verified}>✓ Proprietário Verificado</Text>}
          <View style={styles.divider} />
          <Text style={styles.section}>Sobre o espaço</Text>
          <Text style={styles.description}>{space.description}</Text>
          <Text style={styles.capacity}>
            {space.heightMeters} m de altura × {space.lengthMeters} m de comprimento × {space.depthMeters} m
            de profundidade · {Number(space.volumeCubicMeters).toFixed(2)} m³
          </Text>
          <View style={styles.features}>
            {space.features.map((item) => (
              <View key={item} style={styles.feature}>
                <Text style={styles.featureText}>✓ {item}</Text>
              </View>
            ))}
          </View>
          <View style={styles.divider} />
          <Text style={styles.section}>Calendário de disponibilidade</Text>
          {periods.length ? (
            periods.map((p, i) => (
              <Text key={`${p.start}-${i}`} style={styles.period}>
                Ocupado: {new Date(`${p.start}T12:00:00`).toLocaleDateString('pt-BR')} até{' '}
                {new Date(`${p.end}T12:00:00`).toLocaleDateString('pt-BR')}
              </Text>
            ))
          ) : (
            <Text style={styles.safeText}>Nenhum período ocupado no momento.</Text>
          )}
          <View style={styles.divider} />
          <Text style={styles.section}>Anunciante</Text>
          <Text style={styles.owner}>{mine ? 'Você' : space.owner || 'Proprietário StowUp'}</Text>
          <Text style={styles.safeText}>
            {mine
              ? 'Clientes verão este anúncio depois que o documento e o espaço forem aprovados.'
              : 'As características acima foram informadas pelo proprietário. Converse pelo aplicativo antes de combinar qualquer detalhe.'}
          </Text>
          {!!error && <Text style={styles.error}>{error}</Text>}
          <View style={styles.priceRow}>
            <View>
              <Text style={styles.price}>R$ {Number(space.price).toFixed(0)}</Text>
              <Text style={styles.month}>por mês</Text>
            </View>
            <View style={styles.actions}>
              {mine ? (
                <PrimaryButton
                  label="Gerenciar meu anúncio"
                  onPress={() => navigation.navigate('MyListings')}
                />
              ) : (
                <>
                  <PrimaryButton label="Reservar espaço" onPress={reserve} />
                  <PrimaryButton label="Falar com o anunciante" onPress={talk} loading={starting} secondary />
                </>
              )}
            </View>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#fff' },
  content: { paddingBottom: 35 },
  top: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  back: { color: colors.navy, fontWeight: '800', fontSize: 16, paddingHorizontal: 18, paddingVertical: 13 },
  heart: { fontSize: 30, color: colors.orange, paddingHorizontal: 20 },
  photo: { height: 260, backgroundColor: colors.orangeSoft },
  placeholder: { alignItems: 'center', justifyContent: 'center' },
  placeholderText: { color: colors.orange, fontSize: 30, fontWeight: '900' },
  body: { padding: 21 },
  mine: {
    alignSelf: 'flex-start',
    backgroundColor: colors.orange,
    paddingHorizontal: 11,
    paddingVertical: 6,
    borderRadius: 12,
    marginBottom: 10,
  },
  mineText: { color: '#fff', fontWeight: '900', fontSize: 12 },
  type: { color: colors.orange, fontWeight: '800' },
  title: { color: colors.text, fontSize: 25, fontWeight: '900', marginTop: 5 },
  location: { color: colors.muted, marginTop: 7, fontSize: 15 },
  rating: { color: colors.text, marginTop: 8, fontWeight: '700' },
  verified: { color: colors.success, fontWeight: '900', marginTop: 8 },
  divider: { height: 1, backgroundColor: colors.line, marginVertical: 22 },
  section: { color: colors.text, fontSize: 19, fontWeight: '900' },
  description: { color: colors.muted, lineHeight: 22, marginTop: 10 },
  capacity: { color: colors.navy, fontWeight: '800', marginTop: 10 },
  features: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 15 },
  feature: { backgroundColor: '#F2F5F8', paddingHorizontal: 11, paddingVertical: 8, borderRadius: 12 },
  featureText: { color: colors.navy, fontWeight: '700', fontSize: 12 },
  period: { backgroundColor: '#FFF4EB', color: colors.text, padding: 10, borderRadius: 10, marginTop: 7 },
  owner: { color: colors.text, fontSize: 17, fontWeight: '800', marginTop: 8 },
  safeText: { color: colors.muted, lineHeight: 19, marginTop: 5 },
  priceRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 20, marginTop: 24 },
  actions: { flex: 1, gap: 10 },
  price: { color: colors.navy, fontSize: 22, fontWeight: '900' },
  month: { color: colors.muted, fontSize: 11 },
  error: { color: colors.error, padding: 15, textAlign: 'center' },
});
