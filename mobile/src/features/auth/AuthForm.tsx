import { useState } from 'react';
import {
  ActivityIndicator,
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
import { useAuth } from './AuthContext';
import { BrandMark } from '../../shared/components/BrandMark';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { CountryPhoneField } from '../../shared/components/CountryPhoneField';
import { formatCpf, formatPostalCode } from '../../shared/formatters';
import { lookupBrazilianPostalCode } from '../../core/api/postalCode';

type Props = {
  mode: 'login' | 'register';
  onBack: () => void;
  onSuccess: () => void;
  onSwitch: () => void;
  onTerms?: () => void;
  onForgotPassword?: () => void;
};

export function AuthForm({ mode, onBack, onSuccess, onSwitch, onTerms, onForgotPassword }: Props) {
  const register = mode === 'register';
  const auth = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [cpf, setCpf] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [addressLine, setAddressLine] = useState('');
  const [addressNumber, setAddressNumber] = useState('');
  const [addressComplement, setAddressComplement] = useState('');
  const [district, setDistrict] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [cepLoading, setCepLoading] = useState(false);
  const [acceptedTerms, setAcceptedTerms] = useState(false);

  async function changePostalCode(text: string) {
    const formatted = formatPostalCode(text);
    setPostalCode(formatted);
    setError('');
    if (formatted.replace(/\D/g, '').length !== 8) return;
    setCepLoading(true);
    try {
      const found = await lookupBrazilianPostalCode(formatted);
      setAddressLine(found.addressLine);
      setDistrict(found.district);
      setCity(found.city);
      setState(found.state);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível consultar o CEP.');
    } finally {
      setCepLoading(false);
    }
  }

  async function submit() {
    setError('');
    if (register && name.trim().length < 3) return setError('Informe seu nome completo.');
    if (!email.includes('@')) return setError('Informe um e-mail válido.');
    if (password.length < 8) return setError('A senha precisa ter pelo menos 8 caracteres.');
    if (register && phone.replace(/\D/g, '').length < 10) return setError('Informe um telefone válido.');
    if (register && cpf.replace(/\D/g, '').length !== 11) return setError('Informe os 11 números do CPF.');
    if (register && postalCode.replace(/\D/g, '').length !== 8) return setError('Informe um CEP válido.');
    if (
      register &&
      (!addressLine.trim() ||
        !addressNumber.trim() ||
        !district.trim() ||
        !city.trim() ||
        state.trim().length !== 2)
    )
      return setError('Complete o endereço residencial.');
    if (register && !acceptedTerms) return setError('Leia e aceite os Termos de Uso para criar sua conta.');
    setLoading(true);
    try {
      if (register)
        await auth.signUp({
          name: name.trim(),
          email: email.trim(),
          password,
          phone: phone.trim(),
          cpf: cpf.trim(),
          postalCode: postalCode.trim(),
          addressLine: addressLine.trim(),
          addressNumber: addressNumber.trim(),
          addressComplement: addressComplement.trim(),
          district: district.trim(),
          city: city.trim(),
          state: state.trim().toUpperCase(),
          acceptedTerms,
          termsVersion: '2026-07-13',
        });
      else await auth.signIn(email.trim(), password);
      onSuccess();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível continuar.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.safe} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          <Pressable onPress={onBack}>
            <Text style={styles.back}>‹ Voltar</Text>
          </Pressable>
          <BrandMark />
          <View style={styles.heading}>
            <Text style={styles.title}>{register ? 'Crie sua conta' : 'Que bom ter você de volta'}</Text>
            <Text style={styles.subtitle}>
              {register
                ? 'Seus dados ficam protegidos e são usados para contratos e segurança.'
                : 'Entre para acompanhar anúncios, reservas e conversas.'}
            </Text>
          </View>
          <View style={styles.form}>
            {register && (
              <>
                <Section title="Dados pessoais" />
                <Field label="Nome completo" value={name} onChangeText={setName} autoCapitalize="words" />
                <CountryPhoneField value={phone} onChangeText={setPhone} />
                <Field
                  label="CPF"
                  value={cpf}
                  onChangeText={(text) => setCpf(formatCpf(text))}
                  keyboardType="number-pad"
                  placeholder="000.000.000-00"
                />
              </>
            )}
            <Field
              label="E-mail"
              value={email}
              onChangeText={setEmail}
              keyboardType="email-address"
              autoCapitalize="none"
            />
            <Field
              label="Senha"
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              autoCapitalize="none"
              autoComplete={register ? 'new-password' : 'current-password'}
              textContentType={register ? 'newPassword' : 'password'}
              placeholder={register ? 'Crie uma senha' : 'Digite sua senha'}
            />
            {register && <Text style={styles.passwordHint}>Use pelo menos 8 caracteres.</Text>}
            {!register && (
              <Pressable onPress={onForgotPassword} style={styles.forgotPassword}>
                <Text style={styles.forgotPasswordText}>Esqueci minha senha</Text>
              </Pressable>
            )}
            {register && (
              <>
                <Section title="Endereço residencial" />
                <View>
                  <Field
                    label="CEP"
                    value={postalCode}
                    onChangeText={changePostalCode}
                    keyboardType="number-pad"
                    placeholder="00000-000"
                  />
                  {cepLoading && (
                    <View style={styles.cepStatus}>
                      <ActivityIndicator size="small" color={colors.orange} />
                      <Text style={styles.cepText}>Buscando endereço...</Text>
                    </View>
                  )}
                </View>
                <Field
                  label="Rua/Avenida"
                  value={addressLine}
                  onChangeText={setAddressLine}
                  autoCapitalize="words"
                />
                <View style={styles.row}>
                  <View style={{ flex: 0.7 }}>
                    <Field label="Número" value={addressNumber} onChangeText={setAddressNumber} />
                  </View>
                  <View style={{ flex: 1.3 }}>
                    <Field
                      label="Complemento"
                      value={addressComplement}
                      onChangeText={setAddressComplement}
                      placeholder="Opcional"
                    />
                  </View>
                </View>
                <Field label="Bairro" value={district} onChangeText={setDistrict} autoCapitalize="words" />
                <View style={styles.row}>
                  <View style={{ flex: 1 }}>
                    <Field label="Cidade" value={city} onChangeText={setCity} autoCapitalize="words" />
                  </View>
                  <View style={{ width: 75 }}>
                    <Field
                      label="UF"
                      value={state}
                      onChangeText={(text) => setState(text.slice(0, 2))}
                      autoCapitalize="characters"
                    />
                  </View>
                </View>
              </>
            )}
            {register && (
              <Pressable
                style={styles.termsRow}
                onPress={() => setAcceptedTerms((value) => !value)}
                accessibilityRole="checkbox"
                accessibilityState={{ checked: acceptedTerms }}
              >
                <View style={[styles.checkbox, acceptedTerms && styles.checkboxChecked]}>
                  <Text style={styles.checkboxMark}>{acceptedTerms ? '✓' : ''}</Text>
                </View>
                <Text style={styles.termsText}>
                  Li e aceito os{' '}
                  <Text style={styles.termsLink} onPress={onTerms}>
                    Termos de Uso
                  </Text>{' '}
                  da StowUp (versão 13/07/2026).
                </Text>
              </Pressable>
            )}
            {!!error && <Text style={styles.error}>{error}</Text>}
            <PrimaryButton label={register ? 'Criar conta' : 'Entrar'} onPress={submit} loading={loading} />
          </View>
          <Text style={styles.switch} onPress={onSwitch}>
            {register ? 'Já possui conta? Entrar' : 'Ainda não possui conta? Cadastre-se'}
          </Text>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function Section({ title }: { title: string }) {
  return <Text style={styles.section}>{title}</Text>;
}
function Field(props: React.ComponentProps<typeof TextInput> & { label: string }) {
  const { label, ...input } = props;
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput {...input} placeholderTextColor="#98A2B3" style={styles.input} />
    </View>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surface },
  content: { flexGrow: 1, padding: 24, gap: 25 },
  back: { color: colors.navy, fontWeight: '700', fontSize: 16, paddingVertical: 5 },
  heading: { gap: 8 },
  title: { color: colors.text, fontSize: 30, fontWeight: '800', letterSpacing: -0.8 },
  subtitle: { color: colors.muted, fontSize: 15, lineHeight: 22 },
  form: { gap: 16 },
  section: {
    color: colors.navy,
    fontSize: 17,
    fontWeight: '800',
    marginTop: 8,
    paddingBottom: 3,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  row: { flexDirection: 'row', gap: 12 },
  field: { gap: 7 },
  label: { color: colors.text, fontSize: 14, fontWeight: '700' },
  input: {
    height: 52,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    paddingHorizontal: 15,
    fontSize: 16,
    color: colors.text,
    backgroundColor: '#FCFCFD',
  },
  cepStatus: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 7 },
  cepText: { color: colors.muted, fontSize: 12 },
  termsRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 11,
    padding: 13,
    borderRadius: 14,
    backgroundColor: '#F5F7FA',
  },
  checkbox: {
    width: 23,
    height: 23,
    borderWidth: 1.5,
    borderColor: colors.muted,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxChecked: { backgroundColor: colors.orange, borderColor: colors.orange },
  checkboxMark: { color: '#fff', fontWeight: '900' },
  termsText: { flex: 1, color: colors.muted, lineHeight: 20, fontSize: 13 },
  termsLink: { color: colors.navy, textDecorationLine: 'underline', fontWeight: '800' },
  error: { color: colors.error, lineHeight: 20 },
  switch: { color: colors.navy, fontWeight: '700', textAlign: 'center', padding: 8 },
  forgotPassword: { alignSelf: 'flex-end', paddingVertical: 2 },
  forgotPasswordText: { color: colors.navy, fontWeight: '800' },
  passwordHint: { color: colors.muted, fontSize: 12, marginTop: -9 },
});
