import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';
import { CountryPhoneField } from '../../shared/components/CountryPhoneField';
import { formatPostalCode } from '../../shared/formatters';
import { lookupBrazilianPostalCode } from '../../core/api/postalCode';

export function EditProfileScreen({ navigation }: NativeStackScreenProps<RootStackParamList, 'EditProfile'>) {
  const auth = useAuth();
  const user = auth.user;
  const [name, setName] = useState(user?.name ?? '');
  const [phone, setPhone] = useState(user?.phone ?? '');
  const [postalCode, setPostalCode] = useState(user?.postalCode ?? '');
  const [addressLine, setAddressLine] = useState(user?.addressLine ?? '');
  const [addressNumber, setAddressNumber] = useState(user?.addressNumber ?? '');
  const [addressComplement, setAddressComplement] = useState(user?.addressComplement ?? '');
  const [district, setDistrict] = useState(user?.district ?? '');
  const [city, setCity] = useState(user?.city ?? '');
  const [state, setState] = useState(user?.state ?? '');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [cepLoading, setCepLoading] = useState(false);

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

  async function save() {
    if (name.trim().length < 3 || phone.replace(/\D/g, '').length < 10)
      return setError('Revise o nome e o telefone.');
    if (
      postalCode.replace(/\D/g, '').length !== 8 ||
      !addressLine.trim() ||
      !addressNumber.trim() ||
      !district.trim() ||
      !city.trim() ||
      state.trim().length !== 2
    )
      return setError('Complete o endereço residencial.');
    setLoading(true);
    setError('');
    try {
      await auth.updateProfile({
        name: name.trim(),
        phone: phone.trim(),
        postalCode: postalCode.trim(),
        addressLine: addressLine.trim(),
        addressNumber: addressNumber.trim(),
        addressComplement: addressComplement.trim(),
        district: district.trim(),
        city: city.trim(),
        state: state.trim().toUpperCase(),
      });
      navigation.goBack();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível salvar.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.safe} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.header}>
          <Text onPress={navigation.goBack} style={styles.back}>
            ‹ Voltar
          </Text>
          <Text style={styles.headerTitle}>Configurações da conta</Text>
          <View style={{ width: 52 }} />
        </View>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          <Section title="Dados pessoais" />
          <Field label="Nome completo" value={name} onChangeText={setName} autoCapitalize="words" />
          <CountryPhoneField value={phone} onChangeText={setPhone} />
          <Field label="CPF" value={user?.cpfMasked ?? ''} editable={false} />
          <Field label="E-mail" value={user?.email ?? ''} editable={false} />
          <Section title="Endereço residencial" />
          <View>
            <Field
              label="CEP"
              value={formatPostalCode(postalCode)}
              onChangeText={changePostalCode}
              keyboardType="number-pad"
            />
            {cepLoading && (
              <View style={styles.cepStatus}>
                <ActivityIndicator size="small" color={colors.orange} />
                <Text style={styles.cepText}>Buscando endereço...</Text>
              </View>
            )}
          </View>
          <Field label="Rua/Avenida" value={addressLine} onChangeText={setAddressLine} />
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
          <Field label="Bairro" value={district} onChangeText={setDistrict} />
          <View style={styles.row}>
            <View style={{ flex: 1 }}>
              <Field label="Cidade" value={city} onChangeText={setCity} />
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
          {!!error && <Text style={styles.error}>{error}</Text>}
          <PrimaryButton label="Salvar alterações" onPress={save} loading={loading} />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function Section({ title }: { title: string }) {
  return <Text style={styles.section}>{title}</Text>;
}
function Field({ label, ...props }: React.ComponentProps<typeof TextInput> & { label: string }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        {...props}
        placeholderTextColor="#98A2B3"
        style={[styles.input, props.editable === false && styles.disabled]}
      />
    </View>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surface },
  header: {
    height: 62,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 18,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  back: { color: colors.navy, fontWeight: '800', fontSize: 16 },
  headerTitle: { color: colors.text, fontWeight: '800', fontSize: 17 },
  content: { padding: 24, gap: 16, paddingBottom: 42 },
  section: {
    color: colors.navy,
    fontSize: 17,
    fontWeight: '800',
    marginTop: 7,
    paddingBottom: 4,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  row: { flexDirection: 'row', gap: 12 },
  field: { gap: 7 },
  label: { color: colors.text, fontWeight: '700' },
  input: {
    height: 52,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    paddingHorizontal: 16,
    color: colors.text,
    fontSize: 16,
  },
  disabled: { backgroundColor: colors.background, color: colors.muted },
  cepStatus: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 7 },
  cepText: { color: colors.muted, fontSize: 12 },
  error: { color: colors.error },
});
