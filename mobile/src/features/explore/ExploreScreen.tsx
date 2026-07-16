import * as Location from 'expo-location';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import MapView, { Marker, Region } from 'react-native-maps';
import { useFocusEffect } from '@react-navigation/native';
import { listNotifications, listSpaces, Space } from '../../core/api/marketplace';
import { BrandMark } from '../../shared/components/BrandMark';
import { colors, shadow } from '../../shared/theme';
import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { MainTabParamList, RootStackParamList } from '../../app/navigation/types';
import { API_ORIGIN } from '../../core/api/auth';
import { useAuth } from '../auth/AuthContext';

const SAO_PAULO: Region = {
  latitude: -23.5505,
  longitude: -46.6333,
  latitudeDelta: 0.13,
  longitudeDelta: 0.13,
};
const types = ['Todos', 'Box', 'Armário', 'Quarto', 'Garagem', 'Depósito', 'Galpão', 'Container'];
const featureOptions = [
  'Climatização',
  'Acesso 24 horas',
  'Câmeras',
  'Segurança',
  'Área interna',
  'Área externa',
  'Estacionamento',
  'Acesso para caminhões',
];

export function ExploreScreen({ navigation }: BottomTabScreenProps<MainTabParamList, 'Explorar'>) {
  const root = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();
  const auth = useAuth();
  const map = useRef<MapView>(null);
  const fallbackPositioned = useRef(false);
  const [region, setRegion] = useState(SAO_PAULO);
  const [searchRegion, setSearchRegion] = useState<Region | null>(null);
  const [locationAllowed, setLocationAllowed] = useState(false);
  const [locationResolved, setLocationResolved] = useState(false);
  const [spaces, setSpaces] = useState<Space[]>([]);
  const [query, setQuery] = useState('');
  const [type, setType] = useState('Todos');
  const [mode, setMode] = useState<'map' | 'list'>('map');
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [maxPrice, setMaxPrice] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [minSize, setMinSize] = useState('');
  const [maxSize, setMaxSize] = useState('');
  const [city, setCity] = useState('');
  const [district, setDistrict] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [sort, setSort] = useState('relevance');
  const [selectedFeatures, setSelectedFeatures] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [unreadNotifications, setUnreadNotifications] = useState(0);

  async function refresh(background = false) {
    if (!background) setLoading(true);
    setError('');
    try {
      setSpaces(
        await listSpaces({
          query,
          type,
          city,
          district,
          postalCode,
          minPrice: Number(minPrice.replace(',', '.')) || undefined,
          maxPrice: Number(maxPrice.replace(',', '.')) || undefined,
          minSize: Number(minSize) || undefined,
          maxSize: Number(maxSize) || undefined,
          features: selectedFeatures,
          sort,
          userLatitude: searchRegion?.latitude,
          userLongitude: searchRegion?.longitude,
        }),
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível carregar os espaços.');
    } finally {
      setLoading(false);
    }
  }

  const refreshRef = useRef(refresh);
  refreshRef.current = refresh;

  useEffect(() => {
    refreshRef.current();
    Location.requestForegroundPermissionsAsync()
      .then(async ({ status }) => {
        if (status !== 'granted') return;
        const current = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
        setLocationAllowed(true);
        const next = {
          latitude: current.coords.latitude,
          longitude: current.coords.longitude,
          latitudeDelta: 0.25,
          longitudeDelta: 0.25,
        };
        setRegion(next);
        setSearchRegion(next);
        map.current?.animateToRegion(next, 500);
        setSpaces(await listSpaces({ userLatitude: next.latitude, userLongitude: next.longitude }));
      })
      .catch(() => undefined)
      .finally(() => setLocationResolved(true));
  }, []);

  useEffect(() => {
    const timer = setInterval(() => refreshRef.current(true), 12000);
    return () => clearInterval(timer);
  }, []);

  useFocusEffect(
    useCallback(() => {
      refreshRef.current(true);
    }, []),
  );

  useEffect(() => {
    if (!locationResolved || locationAllowed || fallbackPositioned.current || !spaces.length) return;
    const target =
      spaces.find(
        (space) => space.ownerId === auth.user?.id && space.latitude != null && space.longitude != null,
      ) ?? spaces.find((space) => space.latitude != null && space.longitude != null);
    if (!target) return;
    fallbackPositioned.current = true;
    const next: Region = {
      latitude: target.latitude!,
      longitude: target.longitude!,
      latitudeDelta: 0.25,
      longitudeDelta: 0.25,
    };
    setRegion(next);
    setSearchRegion(next);
    map.current?.animateToRegion(next, 500);
  }, [auth.user?.id, locationAllowed, locationResolved, spaces]);

  useEffect(() => {
    if (!auth.token) {
      setUnreadNotifications(0);
      return;
    }
    const loadUnread = () =>
      listNotifications(auth.token!)
        .then((page) => setUnreadNotifications(page.unread))
        .catch(() => undefined);
    loadUnread();
    const timer = setInterval(loadUnread, 8000);
    return () => clearInterval(timer);
  }, [auth.token]);

  const filtered = useMemo(() => {
    const term = query.trim().toLocaleLowerCase('pt-BR');
    const priceLimit = Number(maxPrice.replace(',', '.'));
    const priceMinimum = Number(minPrice.replace(',', '.'));
    const sizeLimit = Number(minSize.replace(',', '.'));
    const sizeMaximum = Number(maxSize.replace(',', '.'));
    const result = spaces.filter(
      (space) =>
        (!searchRegion ||
          space.latitude == null ||
          space.longitude == null ||
          (Math.abs(space.latitude - searchRegion.latitude) <= searchRegion.latitudeDelta / 2 &&
            Math.abs(space.longitude - searchRegion.longitude) <= searchRegion.longitudeDelta / 2)) &&
        (type === 'Todos' || space.type.toLocaleLowerCase('pt-BR') === type.toLocaleLowerCase('pt-BR')) &&
        (!term ||
          `${space.title} ${space.city} ${space.district}`.toLocaleLowerCase('pt-BR').includes(term)) &&
        (!city || space.city.toLowerCase().includes(city.toLowerCase())) &&
        (!district || space.district.toLowerCase().includes(district.toLowerCase())) &&
        (!priceMinimum || Number(space.price) >= priceMinimum) &&
        (!priceLimit || Number(space.price) <= priceLimit) &&
        (!sizeLimit || Number(space.size) >= sizeLimit) &&
        (!sizeMaximum || Number(space.size) <= sizeMaximum) &&
        selectedFeatures.every((f) =>
          space.features.some((sf) => sf.toLowerCase().includes(f.toLowerCase())),
        ),
    );
    return [...result].sort((a, b) =>
      sort === 'priceAsc'
        ? Number(a.price) - Number(b.price)
        : sort === 'priceDesc'
          ? Number(b.price) - Number(a.price)
          : sort === 'rating'
            ? b.rating - a.rating
            : sort === 'distance'
              ? a.distance - b.distance
              : sort === 'size'
                ? b.size - a.size
                : sort === 'recent'
                  ? b.id - a.id
                  : 0,
    );
  }, [
    spaces,
    query,
    type,
    minPrice,
    maxPrice,
    minSize,
    maxSize,
    city,
    district,
    postalCode,
    selectedFeatures,
    sort,
    searchRegion,
  ]);

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <BrandMark compact />
        <Text style={styles.brand}>
          Stow<Text style={styles.up}>Up</Text>
        </Text>
        {!!auth.user && (
          <Pressable
            accessibilityLabel="Abrir meus favoritos"
            onPress={() => root?.navigate('Favorites')}
            style={styles.notificationButton}
          >
            <Text style={styles.favoriteIcon}>♡</Text>
          </Pressable>
        )}
        {!!auth.user && (
          <Pressable
            accessibilityLabel="Abrir notificações"
            onPress={() => root?.navigate('Notifications')}
            style={styles.notificationButton}
          >
            <Text style={styles.notificationIcon}>🔔</Text>
            {unreadNotifications > 0 && (
              <View style={styles.notificationBadge}>
                <Text style={styles.notificationBadgeText}>
                  {unreadNotifications > 99 ? '99+' : unreadNotifications}
                </Text>
              </View>
            )}
          </Pressable>
        )}
        <Pressable onPress={() => navigation.navigate('Perfil')} style={styles.avatar}>
          {auth.user?.photoUrl ? (
            <Image source={{ uri: `${API_ORIGIN}${auth.user.photoUrl}` }} style={styles.avatarImage} />
          ) : (
            <Text style={styles.avatarText}>{auth.user?.name?.slice(0, 1).toUpperCase() || 'U'}</Text>
          )}
        </Pressable>
      </View>
      <View style={styles.search}>
        <Text style={styles.searchIcon}>⌕</Text>
        <TextInput
          value={query}
          onChangeText={setQuery}
          placeholder="Onde você precisa de espaço?"
          placeholderTextColor={colors.muted}
          style={styles.searchInput}
        />
        <Pressable
          onPress={() => setFiltersOpen((value) => !value)}
          style={[styles.filter, filtersOpen && styles.filterActive]}
        >
          <Text style={styles.filterText}>☷</Text>
        </Pressable>
      </View>
      <ScrollView
        horizontal
        style={styles.chipScroll}
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.chips}
      >
        {types.map((item) => (
          <Pressable
            key={item}
            onPress={() => setType(item)}
            style={[styles.chip, type === item && styles.chipActive]}
          >
            <Text style={[styles.chipText, type === item && styles.chipTextActive]}>{item}</Text>
          </Pressable>
        ))}
      </ScrollView>
      {filtersOpen && (
        <ScrollView style={styles.filterPanel} contentContainerStyle={styles.advancedFilters}>
          <View style={styles.filterRow}>
            <Filter label="Cidade" value={city} set={setCity} />
            <Filter label="Bairro" value={district} set={setDistrict} />
            <Filter label="CEP" value={postalCode} set={setPostalCode} keyboard="numeric" />
          </View>
          <View style={styles.filterRow}>
            <Filter label="Preço mínimo" value={minPrice} set={setMinPrice} keyboard="decimal-pad" />
            <Filter label="Preço máximo" value={maxPrice} set={setMaxPrice} keyboard="decimal-pad" />
          </View>
          <View style={styles.filterRow}>
            <Filter label="Tamanho mínimo" value={minSize} set={setMinSize} keyboard="numeric" />
            <Filter label="Tamanho máximo" value={maxSize} set={setMaxSize} keyboard="numeric" />
          </View>
          <Text style={styles.filterLabel}>Características informadas pelo proprietário</Text>
          <View style={styles.featureChips}>
            {featureOptions.map((f) => (
              <Pressable
                key={f}
                onPress={() =>
                  setSelectedFeatures((v) => (v.includes(f) ? v.filter((i) => i !== f) : [...v, f]))
                }
                style={[styles.smallChip, selectedFeatures.includes(f) && styles.smallChipOn]}
              >
                <Text style={[styles.smallChipText, selectedFeatures.includes(f) && styles.smallChipTextOn]}>
                  {f}
                </Text>
              </Pressable>
            ))}
          </View>
          <Text style={styles.filterLabel}>Ordenar por</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            {[
              ['relevance', 'Relevância'],
              ['priceAsc', 'Menor preço'],
              ['priceDesc', 'Maior preço'],
              ['rating', 'Melhor avaliação'],
              ['distance', 'Menor distância'],
              ['size', 'Maior tamanho'],
              ['recent', 'Mais recentes'],
            ].map(([v, l]) => (
              <Pressable
                key={v}
                onPress={() => setSort(v)}
                style={[styles.smallChip, sort === v && styles.smallChipOn]}
              >
                <Text style={[styles.smallChipText, sort === v && styles.smallChipTextOn]}>{l}</Text>
              </Pressable>
            ))}
          </ScrollView>
          <Pressable onPress={() => refresh()} style={styles.applyFilters}>
            <Text style={styles.applyFiltersText}>Aplicar filtros</Text>
          </Pressable>
          <Pressable
            onPress={() => {
              setMinPrice('');
              setMaxPrice('');
              setMinSize('');
              setMaxSize('');
              setCity('');
              setDistrict('');
              setPostalCode('');
              setSelectedFeatures([]);
              setSort('relevance');
              setType('Todos');
            }}
          >
            <Text style={styles.resetFilters}>Limpar todos os filtros</Text>
          </Pressable>
        </ScrollView>
      )}
      <View style={styles.viewBar}>
        <Text style={styles.results}>
          {filtered.length} {filtered.length === 1 ? 'espaço' : 'espaços'}
        </Text>
        <View style={styles.viewSwitch}>
          <Pressable
            onPress={() => setMode('map')}
            style={[styles.viewOption, mode === 'map' && styles.viewOptionActive]}
          >
            <Text style={[styles.viewOptionText, mode === 'map' && styles.viewOptionTextActive]}>Mapa</Text>
          </Pressable>
          <Pressable
            onPress={() => setMode('list')}
            style={[styles.viewOption, mode === 'list' && styles.viewOptionActive]}
          >
            <Text style={[styles.viewOptionText, mode === 'list' && styles.viewOptionTextActive]}>Lista</Text>
          </Pressable>
        </View>
      </View>
      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={colors.orange} size="large" />
          <Text style={styles.loadingText}>Buscando espaços...</Text>
        </View>
      ) : mode === 'map' ? (
        <View style={styles.mapWrap}>
          <MapView
            ref={map}
            style={StyleSheet.absoluteFill}
            initialRegion={region}
            onRegionChangeComplete={setRegion}
            showsUserLocation={locationAllowed}
            showsMyLocationButton
          >
            {filtered
              .filter((item) => item.latitude != null && item.longitude != null)
              .map((item) => {
                const mine = auth.user?.id === item.ownerId;
                return (
                  <Marker
                    key={item.id}
                    onPress={() => root?.navigate('SpaceDetail', { spaceId: item.id })}
                    coordinate={{ latitude: item.latitude!, longitude: item.longitude! }}
                    title={mine ? `Seu anúncio · ${item.title}` : item.title}
                    description={
                      mine ? 'Este espaço pertence a você' : `R$ ${Number(item.price).toFixed(2)}/mês`
                    }
                  >
                    <View style={[styles.priceMarker, mine && styles.myMarker]}>
                      <Text style={[styles.priceMarkerText, mine && styles.myMarkerText]}>
                        {mine ? 'Seu espaço' : `R$ ${Math.round(Number(item.price))}`}
                      </Text>
                    </View>
                  </Marker>
                );
              })}
          </MapView>
          <Pressable
            onPress={() => {
              setSearchRegion({ ...region });
              refresh(true);
            }}
            style={styles.areaButton}
          >
            <Text style={styles.areaButtonText}>⌖ Buscar nesta área</Text>
          </Pressable>
          <View style={styles.mapBottom}>
            {filtered.length ? (
              <ScrollView
                horizontal
                showsHorizontalScrollIndicator={false}
                contentContainerStyle={{ gap: 10 }}
              >
                {filtered.map((item) => (
                  <SpaceCard
                    key={item.id}
                    item={item}
                    compact
                    onPress={() => root?.navigate('SpaceDetail', { spaceId: item.id })}
                  />
                ))}
              </ScrollView>
            ) : (
              <View style={styles.emptyBar}>
                <Text style={styles.emptyTitle}>Nenhum espaço nesta área</Text>
                <Text style={styles.emptyText}>
                  {error || 'Amplie o mapa ou altere os filtros. Novos anúncios aparecerão após aprovação.'}
                </Text>
              </View>
            )}
          </View>
        </View>
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={filtered.length ? styles.list : styles.emptyList}
          renderItem={({ item }) => (
            <SpaceCard item={item} onPress={() => root?.navigate('SpaceDetail', { spaceId: item.id })} />
          )}
          ListEmptyComponent={
            <View style={styles.center}>
              <Text style={styles.emptyTitle}>Nenhum espaço encontrado</Text>
              <Text style={styles.emptyText}>{error || 'Tente outro bairro ou remova algum filtro.'}</Text>
              <Pressable
                onPress={() => {
                  setQuery('');
                  setType('Todos');
                  refresh();
                }}
              >
                <Text style={styles.clear}>Limpar filtros</Text>
              </Pressable>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
}

function SpaceCard({
  item,
  compact = false,
  onPress,
}: {
  item: Space;
  compact?: boolean;
  onPress?: () => void;
}) {
  const { user } = useAuth();
  const mine = user?.id === item.ownerId;
  const rating = item.reviews > 0 ? `★ ${item.rating.toFixed(1)} (${item.reviews})` : 'Novo · sem avaliações';
  return (
    <Pressable onPress={onPress} style={[styles.card, compact && styles.cardCompact]}>
      <View style={styles.cardImage}>
        {item.photos?.[0] ? (
          <Image source={{ uri: `${API_ORIGIN}${item.photos[0]}` }} style={StyleSheet.absoluteFill} />
        ) : (
          <BrandMark compact />
        )}
        {mine && (
          <View style={styles.myBadge}>
            <Text style={styles.myBadgeText}>Seu anúncio</Text>
          </View>
        )}
      </View>
      <View style={styles.cardInfo}>
        <Text numberOfLines={1} style={styles.cardTitle}>
          {item.title}
        </Text>
        <Text style={styles.cardMeta}>
          {item.district}, {item.city} · {item.size} m²
          {item.distance > 0 ? ` · ${item.distance.toFixed(1)} km` : ''}
        </Text>
        {item.ownerVerified && <Text style={styles.verified}>✓ Proprietário verificado</Text>}
        <View style={styles.cardFooter}>
          <Text style={styles.cardPrice}>
            R$ {Number(item.price).toFixed(0)}
            <Text style={styles.perMonth}>/mês</Text>
          </Text>
          <Text style={styles.rating}>{rating}</Text>
        </View>
      </View>
    </Pressable>
  );
}
function Filter({
  label,
  value,
  set,
  keyboard,
}: {
  label: string;
  value: string;
  set: (v: string) => void;
  keyboard?: 'numeric' | 'decimal-pad';
}) {
  return (
    <View style={styles.filterField}>
      <Text style={styles.filterLabel}>{label}</Text>
      <TextInput
        value={value}
        onChangeText={set}
        keyboardType={keyboard}
        placeholder="Qualquer"
        placeholderTextColor={colors.muted}
        style={styles.filterInput}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surface },
  header: { height: 58, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 18, gap: 9 },
  brand: { flex: 1, color: colors.navy, fontSize: 23, fontWeight: '800', letterSpacing: -0.7 },
  up: { color: colors.orange },
  notificationButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#fff',
  },
  favoriteIcon: { color: colors.navy, fontSize: 25, lineHeight: 28, fontWeight: '600' },
  notificationIcon: { fontSize: 17, lineHeight: 22 },
  notificationBadge: {
    position: 'absolute',
    right: -5,
    top: -5,
    minWidth: 18,
    height: 18,
    paddingHorizontal: 4,
    borderRadius: 9,
    backgroundColor: colors.orange,
    borderWidth: 2,
    borderColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
  notificationBadgeText: { color: '#fff', fontSize: 8, fontWeight: '900' },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    overflow: 'hidden',
    backgroundColor: colors.orangeSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarImage: { width: '100%', height: '100%' },
  avatarText: { color: colors.orange, fontWeight: '800' },
  search: {
    marginHorizontal: 18,
    height: 54,
    borderRadius: 18,
    backgroundColor: colors.surface,
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: 15,
    ...shadow,
  },
  searchIcon: { color: colors.navy, fontSize: 26 },
  searchInput: { flex: 1, paddingHorizontal: 10, color: colors.text, fontSize: 15 },
  filter: {
    width: 40,
    height: 40,
    borderRadius: 13,
    marginRight: 7,
    backgroundColor: colors.navy,
    alignItems: 'center',
    justifyContent: 'center',
  },
  filterActive: { backgroundColor: colors.orange },
  filterText: { color: '#fff', fontSize: 20 },
  chipScroll: { flexGrow: 0 },
  chips: { paddingHorizontal: 18, paddingVertical: 13, gap: 8 },
  chip: {
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: colors.line,
    paddingHorizontal: 16,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#fff',
  },
  chipActive: { backgroundColor: colors.navy, borderColor: colors.navy },
  chipText: { color: colors.text, fontSize: 13, fontWeight: '700' },
  chipTextActive: { color: '#fff' },
  filterPanel: { maxHeight: 330 },
  advancedFilters: { gap: 10, paddingHorizontal: 18, paddingBottom: 12 },
  filterRow: { flexDirection: 'row', gap: 9 },
  filterField: { flex: 1, gap: 5 },
  filterLabel: { color: colors.muted, fontWeight: '700', fontSize: 11 },
  filterInput: {
    height: 42,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 12,
    paddingHorizontal: 10,
    color: colors.text,
    backgroundColor: '#fff',
  },
  featureChips: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  smallChip: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 15,
    paddingHorizontal: 10,
    paddingVertical: 7,
    marginRight: 6,
    backgroundColor: '#fff',
  },
  smallChipOn: { backgroundColor: colors.navy, borderColor: colors.navy },
  smallChipText: { fontSize: 10, fontWeight: '800', color: colors.text },
  smallChipTextOn: { color: '#fff' },
  applyFilters: { backgroundColor: colors.orange, borderRadius: 13, padding: 12, alignItems: 'center' },
  applyFiltersText: { color: '#fff', fontWeight: '900' },
  resetFilters: { color: colors.orange, fontWeight: '800', paddingVertical: 12, textAlign: 'center' },
  viewBar: {
    height: 43,
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderTopColor: colors.line,
  },
  results: { color: colors.muted, fontSize: 12, fontWeight: '700' },
  viewSwitch: { flexDirection: 'row', backgroundColor: '#EEF2F6', padding: 3, borderRadius: 13 },
  viewOption: { paddingHorizontal: 14, paddingVertical: 6, borderRadius: 10 },
  viewOptionActive: { backgroundColor: '#fff', ...shadow },
  viewOptionText: { color: colors.muted, fontSize: 12, fontWeight: '800' },
  viewOptionTextActive: { color: colors.navy },
  mapWrap: { flex: 1, minHeight: 260, overflow: 'hidden', backgroundColor: '#DDE7EC' },
  areaButton: {
    position: 'absolute',
    top: 14,
    alignSelf: 'center',
    backgroundColor: colors.navy,
    borderRadius: 22,
    paddingHorizontal: 18,
    paddingVertical: 11,
    ...shadow,
  },
  areaButtonText: { color: '#fff', fontWeight: '800', fontSize: 12 },
  mapBottom: { position: 'absolute', left: 0, right: 0, bottom: 0, padding: 12 },
  emptyBar: { borderRadius: 18, padding: 15, backgroundColor: '#fff', ...shadow },
  emptyTitle: { color: colors.text, fontWeight: '800', fontSize: 17, textAlign: 'center' },
  emptyText: { color: colors.muted, lineHeight: 18, fontSize: 12, textAlign: 'center', marginTop: 4 },
  priceMarker: {
    backgroundColor: '#fff',
    borderWidth: 2,
    borderColor: colors.navy,
    paddingHorizontal: 9,
    paddingVertical: 6,
    borderRadius: 15,
  },
  priceMarkerText: { color: colors.navy, fontSize: 11, fontWeight: '900' },
  myMarker: { backgroundColor: colors.orange, borderColor: '#fff' },
  myMarkerText: { color: '#fff' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 28 },
  loadingText: { color: colors.muted, marginTop: 10 },
  clear: { color: colors.orange, fontWeight: '800', padding: 16 },
  list: { padding: 16, gap: 14 },
  emptyList: { flexGrow: 1 },
  card: {
    borderRadius: 18,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: colors.line,
    overflow: 'hidden',
  },
  cardCompact: { width: 285, flexDirection: 'row', ...shadow },
  cardImage: {
    height: 130,
    minWidth: 95,
    backgroundColor: colors.orangeSoft,
    justifyContent: 'center',
    alignItems: 'center',
  },
  cardCompactImage: { width: 95 },
  myBadge: {
    position: 'absolute',
    top: 8,
    left: 8,
    backgroundColor: colors.orange,
    paddingHorizontal: 9,
    paddingVertical: 5,
    borderRadius: 10,
  },
  myBadgeText: { color: '#fff', fontSize: 10, fontWeight: '900' },
  cardInfo: { padding: 14, flex: 1 },
  cardTitle: { color: colors.text, fontWeight: '800', fontSize: 16 },
  cardMeta: { color: colors.muted, fontSize: 12, marginTop: 4 },
  verified: { color: colors.success, fontWeight: '800', fontSize: 10, marginTop: 5 },
  cardFooter: { marginTop: 12, gap: 4 },
  cardPrice: { color: colors.navy, fontSize: 18, fontWeight: '900' },
  perMonth: { color: colors.muted, fontSize: 11, fontWeight: '600' },
  rating: { color: colors.muted, fontSize: 11 },
});
