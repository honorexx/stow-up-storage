import { NativeStackScreenProps } from '@react-navigation/native-stack';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useState } from 'react';
import { RootStackParamList } from '../../app/navigation/types';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

export function DeleteAccountScreen({
  navigation,
}: NativeStackScreenProps<RootStackParamList, 'DeleteAccount'>) {
  const auth = useAuth();
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function confirm() {
    if (password.length < 8) return setError('Digite sua senha atual para confirmar.');
    Alert.alert(
      'Excluir conta definitivamente?',
      'Seu acesso será encerrado e todos os seus anúncios serão retirados. Essa ação não pode ser desfeita.',
      [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Excluir minha conta', style: 'destructive', onPress: remove },
      ],
    );
  }

  async function remove() {
    setLoading(true);
    setError('');
    try {
      await auth.deleteAccount(password);
      navigation.reset({ index: 0, routes: [{ name: 'Welcome' }] });
    } catch (value) {
      setError(value instanceof Error ? value.message : 'Não foi possível excluir a conta.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.safe} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.header}>
          <Text onPress={() => navigation.goBack()} style={styles.back}>
            ‹ Voltar
          </Text>
          <Text style={styles.headerTitle}>Excluir conta</Text>
          <View style={{ width: 52 }} />
        </View>
        <View style={styles.content}>
          <View style={styles.warning}>
            <Text style={styles.warningTitle}>Essa ação é definitiva</Text>
            <Text style={styles.warningText}>
              Seu login será desativado, seus dados pessoais serão removidos e todos os seus anúncios sairão
              do aplicativo. Contratos e pagamentos já realizados permanecem preservados para segurança
              jurídica.
            </Text>
          </View>
          <Text style={styles.label}>Senha atual</Text>
          <TextInput
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoCapitalize="none"
            placeholder="Digite sua senha"
            placeholderTextColor="#98A2B3"
            style={styles.input}
          />
          {!!error && <Text style={styles.error}>{error}</Text>}
          <Pressable
            disabled={loading}
            onPress={confirm}
            style={({ pressed }) => [styles.delete, pressed && styles.pressed, loading && styles.disabled]}
          >
            <Text style={styles.deleteText}>{loading ? 'Excluindo…' : 'Excluir minha conta'}</Text>
          </Pressable>
          <Pressable disabled={loading} onPress={() => navigation.goBack()} style={styles.cancel}>
            <Text style={styles.cancelText}>Manter minha conta</Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#fff' },
  header: {
    height: 62,
    paddingHorizontal: 18,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  back: { color: colors.navy, fontWeight: '800', fontSize: 16 },
  headerTitle: { color: colors.text, fontWeight: '900', fontSize: 18 },
  content: { flex: 1, padding: 22 },
  warning: {
    padding: 18,
    borderRadius: 16,
    backgroundColor: '#FFF0F0',
    borderWidth: 1,
    borderColor: '#FFD0D0',
    marginBottom: 28,
  },
  warningTitle: { color: colors.error, fontWeight: '900', fontSize: 18 },
  warningText: { color: colors.text, lineHeight: 21, marginTop: 8 },
  label: { color: colors.text, fontWeight: '800', marginBottom: 8 },
  input: {
    height: 54,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    paddingHorizontal: 15,
    fontSize: 16,
    color: colors.text,
  },
  error: { color: colors.error, fontWeight: '700', marginTop: 10 },
  delete: {
    height: 54,
    borderRadius: 16,
    backgroundColor: colors.error,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 24,
  },
  deleteText: { color: '#fff', fontWeight: '900', fontSize: 16 },
  cancel: { height: 50, alignItems: 'center', justifyContent: 'center', marginTop: 8 },
  cancelText: { color: colors.navy, fontWeight: '800' },
  pressed: { opacity: 0.82 },
  disabled: { opacity: 0.55 },
});
