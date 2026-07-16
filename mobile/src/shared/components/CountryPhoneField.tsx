import { useMemo, useState } from 'react';
import { FlatList, Modal, Pressable, SafeAreaView, StyleSheet, Text, TextInput, View } from 'react-native';
import { colors } from '../theme';
import { digitsOnly, formatBrazilianPhone } from '../formatters';

type Country = { code: string; name: string; flag: string; dial: string };
const countries: Country[] = [
  { code: 'BR', name: 'Brasil', flag: '🇧🇷', dial: '+55' },
  { code: 'PT', name: 'Portugal', flag: '🇵🇹', dial: '+351' },
  { code: 'US', name: 'Estados Unidos', flag: '🇺🇸', dial: '+1' },
  { code: 'CA', name: 'Canadá', flag: '🇨🇦', dial: '+1' },
  { code: 'AR', name: 'Argentina', flag: '🇦🇷', dial: '+54' },
  { code: 'CL', name: 'Chile', flag: '🇨🇱', dial: '+56' },
  { code: 'UY', name: 'Uruguai', flag: '🇺🇾', dial: '+598' },
  { code: 'PY', name: 'Paraguai', flag: '🇵🇾', dial: '+595' },
  { code: 'BO', name: 'Bolívia', flag: '🇧🇴', dial: '+591' },
  { code: 'PE', name: 'Peru', flag: '🇵🇪', dial: '+51' },
  { code: 'CO', name: 'Colômbia', flag: '🇨🇴', dial: '+57' },
  { code: 'MX', name: 'México', flag: '🇲🇽', dial: '+52' },
  { code: 'ES', name: 'Espanha', flag: '🇪🇸', dial: '+34' },
  { code: 'FR', name: 'França', flag: '🇫🇷', dial: '+33' },
  { code: 'DE', name: 'Alemanha', flag: '🇩🇪', dial: '+49' },
  { code: 'IT', name: 'Itália', flag: '🇮🇹', dial: '+39' },
  { code: 'GB', name: 'Reino Unido', flag: '🇬🇧', dial: '+44' },
  { code: 'IE', name: 'Irlanda', flag: '🇮🇪', dial: '+353' },
  { code: 'NL', name: 'Países Baixos', flag: '🇳🇱', dial: '+31' },
  { code: 'CH', name: 'Suíça', flag: '🇨🇭', dial: '+41' },
  { code: 'AO', name: 'Angola', flag: '🇦🇴', dial: '+244' },
  { code: 'MZ', name: 'Moçambique', flag: '🇲🇿', dial: '+258' },
  { code: 'CV', name: 'Cabo Verde', flag: '🇨🇻', dial: '+238' },
  { code: 'JP', name: 'Japão', flag: '🇯🇵', dial: '+81' },
  { code: 'CN', name: 'China', flag: '🇨🇳', dial: '+86' },
  { code: 'IN', name: 'Índia', flag: '🇮🇳', dial: '+91' },
  { code: 'AU', name: 'Austrália', flag: '🇦🇺', dial: '+61' },
  { code: 'NZ', name: 'Nova Zelândia', flag: '🇳🇿', dial: '+64' },
];

export function CountryPhoneField({
  value,
  onChangeText,
  label = 'Telefone',
}: {
  value: string;
  onChangeText: (value: string) => void;
  label?: string;
}) {
  const initial = countries.find((item) => value.startsWith(item.dial)) ?? countries[0];
  const [country, setCountry] = useState(initial);
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const nationalDigits = (() => {
    const all = digitsOnly(value);
    const dial = digitsOnly(country.dial);
    return all.startsWith(dial) ? all.slice(dial.length) : all;
  })();
  const display =
    country.code === 'BR'
      ? formatBrazilianPhone(nationalDigits)
      : nationalDigits.replace(/(\d{3})(?=\d)/g, '$1 ').trim();
  const filtered = useMemo(
    () =>
      countries.filter((item) =>
        `${item.name} ${item.dial}`.toLocaleLowerCase('pt-BR').includes(search.toLocaleLowerCase('pt-BR')),
      ),
    [search],
  );
  function select(next: Country) {
    setCountry(next);
    onChangeText(`${next.dial} ${nationalDigits}`.trim());
    setOpen(false);
    setSearch('');
  }
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.phoneRow}>
        <Pressable onPress={() => setOpen(true)} style={styles.countryButton}>
          <Text style={styles.flag}>{country.flag}</Text>
          <Text style={styles.dial}>{country.dial}</Text>
          <Text style={styles.arrow}>⌄</Text>
        </Pressable>
        <TextInput
          value={display}
          onChangeText={(text) =>
            onChangeText(
              `${country.dial} ${country.code === 'BR' ? formatBrazilianPhone(text) : digitsOnly(text)}`,
            )
          }
          keyboardType="phone-pad"
          placeholder={country.code === 'BR' ? '(11) 99999-0000' : 'Número de telefone'}
          placeholderTextColor="#98A2B3"
          style={styles.input}
        />
      </View>
      <Modal visible={open} animationType="slide" onRequestClose={() => setOpen(false)}>
        <SafeAreaView style={styles.modal}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>Selecione o país</Text>
            <Pressable onPress={() => setOpen(false)}>
              <Text style={styles.close}>Fechar</Text>
            </Pressable>
          </View>
          <TextInput
            value={search}
            onChangeText={setSearch}
            placeholder="Buscar país ou código"
            placeholderTextColor={colors.muted}
            style={styles.search}
          />
          <FlatList
            data={filtered}
            keyExtractor={(item) => item.code}
            keyboardShouldPersistTaps="handled"
            renderItem={({ item }) => (
              <Pressable onPress={() => select(item)} style={styles.countryItem}>
                <Text style={styles.itemFlag}>{item.flag}</Text>
                <Text style={styles.countryName}>{item.name}</Text>
                <Text style={styles.itemDial}>{item.dial}</Text>
              </Pressable>
            )}
          />
        </SafeAreaView>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  field: { gap: 7 },
  label: { color: colors.text, fontSize: 14, fontWeight: '700' },
  phoneRow: {
    height: 52,
    flexDirection: 'row',
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    backgroundColor: '#FCFCFD',
    overflow: 'hidden',
  },
  countryButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 11,
    gap: 5,
    borderRightWidth: 1,
    borderRightColor: colors.line,
  },
  flag: { fontSize: 19 },
  dial: { color: colors.text, fontWeight: '700' },
  arrow: { color: colors.muted },
  input: { flex: 1, paddingHorizontal: 12, color: colors.text, fontSize: 16 },
  modal: { flex: 1, backgroundColor: '#fff' },
  modalHeader: {
    height: 62,
    paddingHorizontal: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  modalTitle: { color: colors.text, fontSize: 19, fontWeight: '800' },
  close: { color: colors.orange, fontWeight: '800' },
  search: {
    height: 50,
    margin: 16,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    paddingHorizontal: 15,
    color: colors.text,
  },
  countryItem: {
    minHeight: 58,
    paddingHorizontal: 20,
    flexDirection: 'row',
    alignItems: 'center',
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
    gap: 13,
  },
  itemFlag: { fontSize: 25 },
  countryName: { flex: 1, color: colors.text, fontSize: 16, fontWeight: '600' },
  itemDial: { color: colors.muted, fontWeight: '700' },
});
