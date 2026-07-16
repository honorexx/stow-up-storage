import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import {
  ActivityIndicator,
  Image,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { MainTabParamList, RootStackParamList } from '../../app/navigation/types';
import { BrandMark } from '../../shared/components/BrandMark';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';
import { API_ORIGIN } from '../../core/api/auth';
import { useState } from 'react';

export function ProfileScreen({ navigation }: BottomTabScreenProps<MainTabParamList, 'Perfil'>) {
  const auth = useAuth();
  const [uploading, setUploading] = useState(false);
  const [photoError, setPhotoError] = useState('');
  const root = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();

  if (!auth.user) {
    return (
      <SafeAreaView style={styles.safe}>
        <Header />
        <View style={styles.center}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>?</Text>
          </View>
          <Text style={styles.title}>Entre no StowUp</Text>
          <Text style={styles.subtitle}>Acesse suas reservas, cartões, conversas e preferências.</Text>
          <View style={styles.buttons}>
            <PrimaryButton label="Entrar na conta" onPress={() => root?.navigate('Login')} />
            <PrimaryButton secondary label="Criar conta" onPress={() => root?.navigate('Register')} />
          </View>
        </View>
      </SafeAreaView>
    );
  }

  const initials = auth.user.name
    .split(' ')
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
  async function choosePhoto() {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) return;
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.8,
      preferredAssetRepresentationMode: ImagePicker.UIImagePickerPreferredAssetRepresentationMode.Compatible,
    });
    if (result.canceled) return;
    const asset = result.assets[0];
    setPhotoError('');
    setUploading(true);
    try {
      await auth.updateAvatar({ uri: asset.uri, mimeType: asset.mimeType, fileName: asset.fileName });
    } catch (e) {
      setPhotoError(e instanceof Error ? e.message : 'Não foi possível salvar a foto.');
    } finally {
      setUploading(false);
    }
  }
  return (
    <SafeAreaView style={styles.safe}>
      <Header />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.profileRow}>
          <Pressable onPress={choosePhoto} style={styles.avatar}>
            {auth.user.photoUrl ? (
              <Image source={{ uri: `${API_ORIGIN}${auth.user.photoUrl}` }} style={styles.avatarImage} />
            ) : (
              <Text style={styles.avatarText}>{initials}</Text>
            )}
            {uploading && (
              <View style={styles.uploading}>
                <ActivityIndicator color="#fff" />
              </View>
            )}
            <View style={styles.camera}>
              <Text style={{ fontSize: 12 }}>＋</Text>
            </View>
          </Pressable>
          <View style={styles.identity}>
            <Text style={styles.profileTitle}>{auth.user.name}</Text>
            <Text style={styles.email}>{auth.user.email}</Text>
          </View>
        </View>
        {!!photoError && <Text style={styles.photoError}>{photoError}</Text>}
        <View style={styles.card}>
          <Info label="Telefone" value={auth.user.phone || 'Não informado'} />
          <View style={styles.divider} />
          <Info label="CPF" value={auth.user.cpfMasked || 'Não informado'} />
          <View style={styles.divider} />
          <Info
            label="Endereço"
            value={
              [
                auth.user.addressLine,
                auth.user.addressNumber,
                auth.user.district,
                auth.user.city,
                auth.user.state,
              ]
                .filter(Boolean)
                .join(', ') || 'Não informado'
            }
          />
          <View style={styles.divider} />
          <Info
            label="Tipo de conta"
            value={
              auth.user.role === 'CUSTOMER'
                ? 'Cliente'
                : auth.user.role === 'OWNER'
                  ? 'Proprietário'
                  : 'Administrador'
            }
          />
        </View>
        <View style={styles.buttons}>
          <PrimaryButton label="Editar perfil" onPress={() => root?.navigate('EditProfile')} />
          <PrimaryButton
            secondary
            label="Alterar senha"
            onPress={() => root?.navigate('PasswordRecovery', { email: auth.user?.email })}
          />
          <PrimaryButton secondary label="Avaliações disponíveis" onPress={() => root?.navigate('Reviews')} />
          <PrimaryButton secondary label="Meus anúncios" onPress={() => root?.navigate('MyListings')} />
          {auth.user.role === 'OWNER' && (
            <PrimaryButton
              secondary
              label="Painel do proprietário"
              onPress={() => root?.navigate('OwnerDashboard')}
            />
          )}
          <PrimaryButton
            secondary
            label="Sair da conta"
            onPress={async () => {
              await auth.signOut();
              root?.reset({ index: 0, routes: [{ name: 'Welcome' }] });
            }}
          />
          <Pressable onPress={() => root?.navigate('DeleteAccount')} style={styles.deleteAccount}>
            <Text style={styles.deleteAccountText}>Excluir minha conta</Text>
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function Header() {
  return (
    <View style={styles.header}>
      <BrandMark compact />
      <Text style={styles.headerTitle}>Perfil</Text>
    </View>
  );
}
function Info({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.info}>
      <Text style={styles.infoLabel}>{label}</Text>
      <Text style={styles.infoValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    height: 62,
    backgroundColor: '#fff',
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  headerTitle: { color: colors.text, fontSize: 20, fontWeight: '800' },
  center: { flex: 1, padding: 28, justifyContent: 'center', alignItems: 'center' },
  content: { padding: 22, paddingBottom: 42 },
  profileRow: { flexDirection: 'row', alignItems: 'center', gap: 16, marginBottom: 24 },
  identity: { flex: 1, alignItems: 'flex-start' },
  avatar: {
    width: 76,
    height: 76,
    borderRadius: 25,
    backgroundColor: colors.orangeSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarImage: { width: '100%', height: '100%', borderRadius: 25 },
  uploading: {
    ...StyleSheet.absoluteFillObject,
    borderRadius: 25,
    backgroundColor: '#0007',
    alignItems: 'center',
    justifyContent: 'center',
  },
  camera: {
    position: 'absolute',
    right: -4,
    bottom: -4,
    width: 25,
    height: 25,
    borderRadius: 13,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: { color: colors.orange, fontWeight: '900', fontSize: 25 },
  title: { color: colors.text, fontSize: 25, fontWeight: '800', textAlign: 'center' },
  profileTitle: { color: colors.text, fontSize: 25, fontWeight: '800', textAlign: 'left' },
  subtitle: {
    color: colors.muted,
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
    marginTop: 8,
    maxWidth: 310,
  },
  email: { color: colors.muted, marginTop: 4 },
  photoError: { color: colors.error, marginTop: -12, marginBottom: 14 },
  card: { borderRadius: 18, backgroundColor: '#fff', padding: 18, borderWidth: 1, borderColor: colors.line },
  info: { gap: 4 },
  infoLabel: { color: colors.muted, fontSize: 12, fontWeight: '700' },
  infoValue: { color: colors.text, fontSize: 16, fontWeight: '700' },
  divider: { height: 1, backgroundColor: colors.line, marginVertical: 16 },
  buttons: { width: '100%', gap: 12, marginTop: 28 },
  deleteAccount: { height: 48, alignItems: 'center', justifyContent: 'center' },
  deleteAccountText: { color: colors.error, fontWeight: '800' },
});
