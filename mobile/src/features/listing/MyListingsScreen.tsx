import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useFocusEffect } from '@react-navigation/native';
import * as ImagePicker from 'expo-image-picker';
import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Image,
  Modal,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import { API_ORIGIN } from '../../core/api/auth';
import { deleteOwnedSpace, listMySpaces, OwnerSpace, uploadSpacePhoto } from '../../core/api/marketplace';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

export function MyListingsScreen({ navigation }: NativeStackScreenProps<RootStackParamList, 'MyListings'>) {
  const auth = useAuth();
  const [items, setItems] = useState<OwnerSpace[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleting, setDeleting] = useState<OwnerSpace | null>(null);
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const goHome = () =>
    navigation.reset({ index: 0, routes: [{ name: 'Main', params: { screen: 'Explorar' } }] });
  const refresh = useCallback(() => {
    if (!auth.token) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    listMySpaces(auth.token)
      .then(setItems)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [auth.token]);
  useFocusEffect(refresh);
  useEffect(() => {
    if (!auth.token) return;
    const timer = setInterval(
      () =>
        listMySpaces(auth.token!)
          .then(setItems)
          .catch(() => undefined),
      8000,
    );
    return () => clearInterval(timer);
  }, [auth.token]);
  async function addPhoto(item: OwnerSpace) {
    if (!auth.token) return;
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      quality: 0.85,
      allowsMultipleSelection: false,
      preferredAssetRepresentationMode: ImagePicker.UIImagePickerPreferredAssetRepresentationMode.Compatible,
    });
    if (result.canceled) return;
    const asset = result.assets[0];
    try {
      await uploadSpacePhoto(auth.token, item.id, {
        uri: asset.uri,
        fileName: asset.fileName || `espaco-${Date.now()}.jpg`,
        mimeType: asset.mimeType || 'image/jpeg',
      });
      refresh();
    } catch (e: any) {
      Alert.alert('Não foi possível enviar a foto', e.message);
    }
  }
  async function confirmDelete() {
    if (!auth.token || !deleting || !password) return;
    setBusy(true);
    try {
      await deleteOwnedSpace(auth.token, deleting.id, password);
      setItems((current) => current.filter((item) => item.id !== deleting.id));
      setDeleting(null);
      setPassword('');
      await auth.refreshProfile();
    } catch (e: any) {
      Alert.alert('Não foi possível excluir o anúncio', e.message);
    } finally {
      setBusy(false);
    }
  }
  if (!auth.user)
    return (
      <SafeAreaView style={styles.safe}>
        <Header onBack={goHome} />
        <View style={styles.empty}>
          <Text style={styles.title}>Entre para anunciar</Text>
          <Text style={styles.subtitle}>Use sua conta StowUp para administrar seus espaços.</Text>
          <View style={styles.full}>
            <PrimaryButton label="Entrar" onPress={() => navigation.replace('Login')} />
          </View>
        </View>
      </SafeAreaView>
    );
  return (
    <SafeAreaView style={styles.safe}>
      <Header onBack={goHome} onAdd={() => navigation.navigate('CreateListing')} />
      {loading ? (
        <ActivityIndicator style={{ flex: 1 }} color={colors.orange} />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={items.length ? styles.list : styles.emptyList}
          onRefresh={refresh}
          refreshing={loading}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.icon}>▱</Text>
              <Text style={styles.title}>Você ainda não anunciou</Text>
              <Text style={styles.subtitle}>
                {error ||
                  'Cadastre um box, garagem, armário, quarto ou galpão e acompanhe a análise por aqui.'}
              </Text>
              <View style={styles.full}>
                <PrimaryButton
                  label="Cadastrar meu primeiro espaço"
                  onPress={() => navigation.navigate('CreateListing')}
                />
              </View>
            </View>
          }
          renderItem={({ item }) => (
            <Pressable
              onPress={() => navigation.navigate('ListingDocument', { spaceId: item.id, title: item.title })}
              style={styles.card}
            >
              {item.photos?.[0] ? (
                <Image source={{ uri: `${API_ORIGIN}${item.photos[0]}` }} style={styles.photo} />
              ) : (
                <View style={styles.photoEmpty}>
                  <Text style={{ fontSize: 30 }}>▱</Text>
                </View>
              )}
              <View style={styles.info}>
                <View style={styles.row}>
                  <Text numberOfLines={1} style={styles.cardTitle}>
                    {item.title}
                  </Text>
                  <Text style={[styles.status, item.lifecycleStatus === 'ACTIVE' && styles.published]}>
                    {item.lifecycleStatus}
                  </Text>
                </View>
                <Text style={styles.meta}>
                  {item.type} · {item.size} m² · {item.district}, {item.city}
                </Text>
                <Text style={styles.meta}>
                  {item.heightMeters} m altura × {item.lengthMeters} m comprimento × {item.depthMeters} m
                  profundidade
                </Text>
                <Text style={styles.meta}>
                  Volume: {Number(item.volumeCubicMeters).toFixed(2)} m³ · Fotos: {item.photos.length}/
                  {item.minimumPhotos}
                </Text>
                {!item.photoRequirementMet && (
                  <Text style={styles.warning}>
                    Adicione pelo menos {item.minimumPhotos} fotos para permitir a aprovação.
                  </Text>
                )}
                <Text style={styles.price}>R$ {Number(item.price).toFixed(2).replace('.', ',')}/mês</Text>
                <Text style={styles.address}>
                  {item.addressLine}, {item.addressNumber}
                </Text>
                <Text style={styles.documentHint}>Toque para documentos e acompanhamento</Text>
                <View style={styles.actions}>
                  <Text
                    onPress={(e) => {
                      e.stopPropagation();
                      addPhoto(item);
                    }}
                    style={styles.photoAction}
                  >
                    Adicionar foto
                  </Text>
                  <Text
                    onPress={(e) => {
                      e.stopPropagation();
                      setDeleting(item);
                    }}
                    style={styles.deleteAction}
                  >
                    Excluir anúncio
                  </Text>
                </View>
              </View>
            </Pressable>
          )}
        />
      )}
      <Modal transparent visible={!!deleting} animationType="fade" onRequestClose={() => setDeleting(null)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Excluir anúncio?</Text>
            <Text style={styles.modalBody}>
              O anúncio será retirado da busca e não poderá ser recuperado. Digite sua senha para confirmar.
            </Text>
            <TextInput
              value={password}
              onChangeText={setPassword}
              placeholder="Sua senha"
              secureTextEntry
              style={styles.passwordInput}
            />
            <View style={styles.modalActions}>
              <Pressable
                disabled={busy}
                onPress={() => {
                  setDeleting(null);
                  setPassword('');
                }}
              >
                <Text style={styles.cancel}>Cancelar</Text>
              </Pressable>
              <Pressable disabled={busy || !password} onPress={confirmDelete}>
                <Text style={[styles.confirmDelete, (!password || busy) && { opacity: 0.45 }]}>Excluir</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}
function Header({ onBack, onAdd }: { onBack: () => void; onAdd?: () => void }) {
  return (
    <View style={styles.header}>
      <Text onPress={onBack} style={styles.back}>
        ‹ Voltar
      </Text>
      <Text style={styles.headerTitle}>Meus anúncios</Text>
      {onAdd ? (
        <Pressable onPress={onAdd} style={styles.add}>
          <Text style={styles.addText}>＋</Text>
        </Pressable>
      ) : (
        <View style={{ width: 38 }} />
      )}
    </View>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    height: 62,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  back: { color: colors.navy, fontWeight: '800', fontSize: 16 },
  headerTitle: { color: colors.text, fontWeight: '800', fontSize: 18 },
  add: {
    width: 38,
    height: 38,
    borderRadius: 13,
    backgroundColor: colors.orange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addText: { color: '#fff', fontSize: 21 },
  list: { padding: 16, gap: 13 },
  emptyList: { flexGrow: 1 },
  empty: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 28 },
  icon: { color: colors.orange, fontSize: 52, marginBottom: 14 },
  title: { color: colors.text, fontSize: 24, fontWeight: '800', textAlign: 'center' },
  subtitle: { color: colors.muted, fontSize: 15, lineHeight: 22, textAlign: 'center', marginTop: 8 },
  full: { width: '100%', marginTop: 24 },
  card: {
    borderRadius: 18,
    backgroundColor: '#fff',
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: colors.line,
  },
  photo: { width: '100%', height: 160 },
  photoEmpty: {
    height: 130,
    backgroundColor: colors.orangeSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  info: { padding: 15 },
  row: { flexDirection: 'row', gap: 8 },
  cardTitle: { flex: 1, color: colors.text, fontWeight: '800', fontSize: 17 },
  status: {
    color: '#9A6700',
    backgroundColor: '#FFF7D6',
    paddingHorizontal: 9,
    paddingVertical: 4,
    borderRadius: 10,
    fontSize: 10,
    fontWeight: '800',
  },
  published: { color: colors.success, backgroundColor: '#E9F8F1' },
  meta: { color: colors.muted, marginTop: 7 },
  warning: { color: colors.error, fontSize: 11, fontWeight: '800', marginTop: 6 },
  price: { color: colors.navy, fontWeight: '900', fontSize: 17, marginTop: 10 },
  address: { color: colors.muted, fontSize: 12, marginTop: 3 },
  documentHint: { color: colors.orange, fontSize: 12, fontWeight: '800', marginTop: 9 },
  actions: { flexDirection: 'row', gap: 10, marginTop: 14 },
  photoAction: {
    flex: 1,
    color: colors.navy,
    fontWeight: '900',
    paddingVertical: 10,
    textAlign: 'center',
    borderWidth: 1,
    borderColor: colors.navy,
    borderRadius: 11,
  },
  deleteAction: {
    flex: 1,
    color: colors.error,
    fontWeight: '900',
    paddingVertical: 10,
    textAlign: 'center',
    borderWidth: 1,
    borderColor: colors.error,
    borderRadius: 11,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: '#0008',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  modalCard: { width: '100%', maxWidth: 440, borderRadius: 20, backgroundColor: '#fff', padding: 22 },
  modalTitle: { color: colors.text, fontSize: 21, fontWeight: '900' },
  modalBody: { color: colors.muted, lineHeight: 20, marginTop: 8 },
  passwordInput: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 12,
    padding: 13,
    marginTop: 18,
    color: colors.text,
  },
  modalActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 24, marginTop: 20 },
  cancel: { color: colors.navy, fontWeight: '800', padding: 8 },
  confirmDelete: { color: colors.error, fontWeight: '900', padding: 8 },
});
