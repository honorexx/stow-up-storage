import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useFocusEffect } from '@react-navigation/native';
import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import { API_ORIGIN } from '../../core/api/auth';
import { listFavorites, removeFavorite, Space } from '../../core/api/marketplace';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';
export function FavoritesScreen({ navigation }: NativeStackScreenProps<RootStackParamList, 'Favorites'>) {
  const auth = useAuth();
  const [items, setItems] = useState<Space[]>([]);
  const [loading, setLoading] = useState(true);
  const refresh = useCallback(() => {
    if (!auth.token) return;
    setLoading(true);
    listFavorites(auth.token)
      .then(setItems)
      .finally(() => setLoading(false));
  }, [auth.token]);
  useFocusEffect(refresh);
  async function remove(id: number) {
    if (!auth.token) return;
    await removeFavorite(auth.token, id);
    setItems((v) => v.filter((i) => i.id !== id));
  }
  return (
    <SafeAreaView style={s.safe}>
      <Header back={navigation.goBack} />
      {loading ? (
        <ActivityIndicator style={{ flex: 1 }} color={colors.orange} />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(i) => String(i.id)}
          contentContainerStyle={items.length ? s.list : s.empty}
          ListEmptyComponent={
            <View style={s.center}>
              <Text style={s.title}>Nenhum favorito ainda</Text>
              <Text style={s.muted}>Toque no coração de um espaço para encontrá-lo rapidamente depois.</Text>
            </View>
          }
          renderItem={({ item }) => (
            <Pressable
              onPress={() => navigation.navigate('SpaceDetail', { spaceId: item.id })}
              style={s.card}
            >
              {item.photos[0] ? (
                <Image source={{ uri: `${API_ORIGIN}${item.photos[0]}` }} style={s.photo} />
              ) : (
                <View style={s.photo} />
              )}
              <View style={s.info}>
                <Text style={s.cardTitle}>{item.title}</Text>
                <Text style={s.muted}>
                  {item.district}, {item.city} ·{' '}
                  {item.distance ? `${item.distance.toFixed(1)} km` : 'distância indisponível'}
                </Text>
                <Text style={s.price}>R$ {Number(item.price).toFixed(0)}/mês</Text>
                <Text onPress={() => remove(item.id)} style={s.remove}>
                  Remover dos favoritos
                </Text>
              </View>
            </Pressable>
          )}
        />
      )}
    </SafeAreaView>
  );
}
function Header({ back }: { back: () => void }) {
  return (
    <View style={s.header}>
      <Text onPress={back} style={s.back}>
        ‹ Voltar
      </Text>
      <Text style={s.headerTitle}>Favoritos</Text>
      <View style={{ width: 55 }} />
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
  headerTitle: { color: colors.text, fontSize: 19, fontWeight: '900' },
  list: { padding: 16, gap: 12 },
  empty: { flexGrow: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 30 },
  title: { fontSize: 22, fontWeight: '900', color: colors.text },
  muted: { color: colors.muted, lineHeight: 19, marginTop: 5 },
  card: {
    backgroundColor: '#fff',
    borderRadius: 18,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: colors.line,
    flexDirection: 'row',
  },
  photo: { width: 105, minHeight: 120, backgroundColor: colors.orangeSoft },
  info: { flex: 1, padding: 14 },
  cardTitle: { fontWeight: '900', fontSize: 16, color: colors.text },
  price: { fontWeight: '900', color: colors.navy, fontSize: 17, marginTop: 9 },
  remove: { color: colors.orange, fontWeight: '800', fontSize: 12, marginTop: 9 },
});
