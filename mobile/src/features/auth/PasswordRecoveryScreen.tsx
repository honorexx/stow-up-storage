import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import {
  PasswordRecoveryChannel,
  requestPasswordRecovery,
  resetPassword,
  verifyPasswordRecovery,
} from '../../core/api/auth';
import { BrandMark } from '../../shared/components/BrandMark';
import { CountryPhoneField } from '../../shared/components/CountryPhoneField';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from './AuthContext';

type Step = 'request' | 'code' | 'password';

export function PasswordRecoveryScreen({
  navigation,
  route,
}: NativeStackScreenProps<RootStackParamList, 'PasswordRecovery'>) {
  const auth = useAuth();
  const [step, setStep] = useState<Step>('request');
  const [email, setEmail] = useState(route.params?.email ?? auth.user?.email ?? '');
  const [phone, setPhone] = useState(auth.user?.phone ?? '');
  const [channel, setChannel] = useState<PasswordRecoveryChannel>('EMAIL');
  const [requestId, setRequestId] = useState('');
  const [destination, setDestination] = useState('');
  const [code, setCode] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [countdown, setCountdown] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setInterval(() => setCountdown((value) => Math.max(0, value - 1)), 1000);
    return () => clearInterval(timer);
  }, [countdown]);

  async function sendCode(selectedChannel = channel) {
    const identifier = selectedChannel === 'EMAIL' ? email.trim() : phone.trim();
    if (selectedChannel === 'EMAIL' && !identifier.includes('@'))
      return setError('Informe o e-mail cadastrado na sua conta.');
    if (selectedChannel === 'SMS' && identifier.replace(/\D/g, '').length < 10)
      return setError('Informe o celular cadastrado na sua conta.');
    setLoading(true);
    setError('');
    try {
      const result = await requestPasswordRecovery(identifier, selectedChannel);
      setChannel(selectedChannel);
      setRequestId(result.requestId);
      setDestination(result.maskedDestination);
      setCountdown(result.resendAfterSeconds);
      setCode('');
      setStep('code');
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível enviar o código.');
    } finally {
      setLoading(false);
    }
  }

  async function verifyCode() {
    if (code.length !== 6) return setError('Digite os seis números recebidos.');
    setLoading(true);
    setError('');
    try {
      const result = await verifyPasswordRecovery(requestId, code);
      setResetToken(result.resetToken);
      setStep('password');
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível validar o código.');
    } finally {
      setLoading(false);
    }
  }

  async function savePassword() {
    if (password.length < 8) return setError('A nova senha precisa ter pelo menos 8 caracteres.');
    if (password !== confirmation) return setError('As duas senhas precisam ser iguais.');
    setLoading(true);
    setError('');
    try {
      await resetPassword(requestId, resetToken, password);
      if (auth.user) await auth.signOut();
      Alert.alert('Senha alterada', 'Sua nova senha foi salva. Entre novamente para continuar.', [
        {
          text: 'Entrar',
          onPress: () => navigation.reset({ index: 0, routes: [{ name: 'Login' }] }),
        },
      ]);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível alterar a senha.');
    } finally {
      setLoading(false);
    }
  }

  function goBack() {
    setError('');
    if (step === 'password') return setStep('code');
    if (step === 'code') return setStep('request');
    navigation.goBack();
  }

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.safe} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          <Pressable onPress={goBack}>
            <Text style={styles.back}>‹ Voltar</Text>
          </Pressable>
          <BrandMark />
          <View style={styles.heading}>
            <Text style={styles.eyebrow}>SEGURANÇA DA CONTA</Text>
            <Text style={styles.title}>
              {step === 'request'
                ? 'Recuperar senha'
                : step === 'code'
                  ? 'Digite o código'
                  : 'Crie uma nova senha'}
            </Text>
            <Text style={styles.subtitle}>
              {step === 'request'
                ? 'Enviaremos um código de seis números. Você poderá escolher o e-mail ou o celular já cadastrado.'
                : step === 'code'
                  ? `Enviamos o código para ${destination}. Ele expira em 10 minutos.`
                  : 'Use pelo menos 8 caracteres e escolha uma senha diferente da anterior.'}
            </Text>
          </View>

          {step === 'request' && (
            <View style={styles.form}>
              <Text style={styles.label}>Como deseja receber?</Text>
              <View style={styles.channelRow}>
                <ChannelCard
                  selected={channel === 'EMAIL'}
                  icon="✉"
                  title="E-mail"
                  detail="Código na caixa de entrada"
                  onPress={() => setChannel('EMAIL')}
                />
                <ChannelCard
                  selected={channel === 'SMS'}
                  icon="▣"
                  title="SMS"
                  detail="Código no celular cadastrado"
                  onPress={() => setChannel('SMS')}
                />
              </View>
              {channel === 'EMAIL' ? (
                <Field
                  label="E-mail cadastrado"
                  value={email}
                  onChangeText={setEmail}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  editable={!route.params?.email}
                  placeholder="seuemail@exemplo.com"
                />
              ) : (
                <CountryPhoneField value={phone} onChangeText={setPhone} label="Celular cadastrado" />
              )}
              {!!error && <Text style={styles.error}>{error}</Text>}
              <PrimaryButton label="Enviar código" onPress={() => sendCode()} loading={loading} />
            </View>
          )}

          {step === 'code' && (
            <View style={styles.form}>
              <Field
                label="Código de 6 números"
                value={code}
                onChangeText={(value) => setCode(value.replace(/\D/g, '').slice(0, 6))}
                keyboardType="number-pad"
                placeholder="000000"
                maxLength={6}
                style={styles.codeInput}
              />
              {!!error && <Text style={styles.error}>{error}</Text>}
              <PrimaryButton label="Validar código" onPress={verifyCode} loading={loading} />
              <PrimaryButton
                secondary
                label={
                  countdown > 0
                    ? `Reenviar em ${countdown}s`
                    : `Reenviar por ${channel === 'EMAIL' ? 'e-mail' : 'SMS'}`
                }
                onPress={() => sendCode(channel)}
                disabled={countdown > 0}
              />
              <Pressable
                disabled={loading}
                onPress={() => {
                  setChannel(channel === 'EMAIL' ? 'SMS' : 'EMAIL');
                  setStep('request');
                  setError('');
                }}
                style={styles.alternative}
              >
                <Text style={styles.alternativeText}>
                  {channel === 'EMAIL'
                    ? 'Não tenho acesso ao e-mail — enviar por SMS'
                    : 'Enviar pelo e-mail cadastrado'}
                </Text>
              </Pressable>
            </View>
          )}

          {step === 'password' && (
            <View style={styles.form}>
              <Field
                label="Nova senha"
                value={password}
                onChangeText={setPassword}
                secureTextEntry
                autoCapitalize="none"
              />
              <Field
                label="Confirme a nova senha"
                value={confirmation}
                onChangeText={setConfirmation}
                secureTextEntry
                autoCapitalize="none"
              />
              {!!error && <Text style={styles.error}>{error}</Text>}
              <PrimaryButton label="Salvar nova senha" onPress={savePassword} loading={loading} />
            </View>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function Field({ label, ...props }: React.ComponentProps<typeof TextInput> & { label: string }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput {...props} placeholderTextColor="#98A2B3" style={[styles.input, props.style]} />
    </View>
  );
}

function ChannelCard({
  selected,
  icon,
  title,
  detail,
  onPress,
}: {
  selected: boolean;
  icon: string;
  title: string;
  detail: string;
  onPress: () => void;
}) {
  return (
    <Pressable onPress={onPress} style={[styles.channel, selected && styles.channelSelected]}>
      <Text style={styles.channelIcon}>{icon}</Text>
      <Text style={styles.channelTitle}>{title}</Text>
      <Text style={styles.channelDetail}>{detail}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surface },
  content: { flexGrow: 1, padding: 24, paddingBottom: 40, gap: 25 },
  back: { color: colors.navy, fontWeight: '700', fontSize: 16, paddingVertical: 5 },
  heading: { gap: 8 },
  eyebrow: { color: colors.orange, fontSize: 11, fontWeight: '900', letterSpacing: 1.3 },
  title: { color: colors.text, fontSize: 30, fontWeight: '800', letterSpacing: -0.8 },
  subtitle: { color: colors.muted, fontSize: 15, lineHeight: 22 },
  form: { gap: 16 },
  field: { gap: 7 },
  label: { color: colors.text, fontSize: 14, fontWeight: '700' },
  input: {
    height: 54,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    paddingHorizontal: 15,
    fontSize: 16,
    color: colors.text,
    backgroundColor: '#FCFCFD',
  },
  codeInput: { fontSize: 25, fontWeight: '900', letterSpacing: 9, textAlign: 'center' },
  channelRow: { flexDirection: 'row', gap: 11 },
  channel: {
    flex: 1,
    minHeight: 132,
    padding: 15,
    borderRadius: 17,
    borderWidth: 1.5,
    borderColor: colors.line,
    backgroundColor: '#fff',
  },
  channelSelected: { borderColor: colors.orange, backgroundColor: colors.orangeSoft },
  channelIcon: { color: colors.navy, fontSize: 24, marginBottom: 10 },
  channelTitle: { color: colors.text, fontWeight: '900', fontSize: 16 },
  channelDetail: { color: colors.muted, fontSize: 12, lineHeight: 17, marginTop: 4 },
  error: { color: colors.error, lineHeight: 20 },
  alternative: { padding: 8, alignItems: 'center' },
  alternativeText: { color: colors.navy, fontWeight: '800', textAlign: 'center' },
});
