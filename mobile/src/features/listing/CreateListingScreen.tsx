import { NativeStackScreenProps } from '@react-navigation/native-stack';
import * as ImagePicker from 'expo-image-picker';
import * as Location from 'expo-location';
import { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
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
import { createSpace, LocalImage, uploadSpacePhoto } from '../../core/api/marketplace';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';
import { formatPostalCode } from '../../shared/formatters';
import { lookupBrazilianPostalCode } from '../../core/api/postalCode';

const categories = ['Box', 'Armário', 'Quarto', 'Garagem', 'Depósito', 'Galpão', 'Container'];
export function CreateListingScreen({
  navigation,
}: NativeStackScreenProps<RootStackParamList, 'CreateListing'>) {
  const auth = useAuth();
  const [title, setTitle] = useState('');
  const [type, setType] = useState('Box');
  const [height, setHeight] = useState('');
  const [length, setLength] = useState('');
  const [depth, setDepth] = useState('');
  const [price, setPrice] = useState('');
  const [description, setDescription] = useState('');
  const [features, setFeatures] = useState('Câmeras, Segurança, Área interna');
  const [postalCode, setPostalCode] = useState('');
  const [addressLine, setAddressLine] = useState('');
  const [addressNumber, setAddressNumber] = useState('');
  const [addressComplement, setAddressComplement] = useState('');
  const [district, setDistrict] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [photos, setPhotos] = useState<LocalImage[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [cepLoading, setCepLoading] = useState(false);
  const [phase, setPhase] = useState('');

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

  async function pickPhotos() {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) return;
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsMultipleSelection: true,
      selectionLimit: 8,
      quality: 0.8,
      preferredAssetRepresentationMode: ImagePicker.UIImagePickerPreferredAssetRepresentationMode.Compatible,
    });
    if (!result.canceled)
      setPhotos(
        result.assets
          .slice(0, 8)
          .map((asset) => ({ uri: asset.uri, mimeType: asset.mimeType, fileName: asset.fileName })),
      );
  }
  async function submit() {
    if (!auth.token) return navigation.replace('Login');
    const heightValue = Number(height.replace(',', '.'));
    const lengthValue = Number(length.replace(',', '.'));
    const depthValue = Number(depth.replace(',', '.'));
    const areaValue = lengthValue * depthValue;
    const volumeValue = heightValue * lengthValue * depthValue;
    const priceValue = Number(price.replace(',', '.'));
    if (title.trim().length < 5) return setError('O nome do anúncio precisa ter pelo menos 5 caracteres.');
    if (!heightValue || !lengthValue || !depthValue)
      return setError('Informe altura, comprimento e profundidade do espaço.');
    if (heightValue > 100 || lengthValue > 1000 || depthValue > 1000)
      return setError('Revise as dimensões informadas.');
    if (!priceValue || priceValue < 50) return setError('O preço mínimo do anúncio é R$ 50 por mês.');
    if (description.trim().length < 30)
      return setError(
        `A descrição precisa ter pelo menos 30 caracteres. Você informou ${description.trim().length}.`,
      );
    const missing = [
      postalCode.replace(/\D/g, '').length !== 8 && 'CEP',
      !addressLine.trim() && 'rua',
      !addressNumber.trim() && 'número',
      !district.trim() && 'bairro',
      !city.trim() && 'cidade',
      state.trim().length !== 2 && 'UF',
    ].filter(Boolean);
    if (missing.length) return setError(`Complete o endereço do espaço. Falta: ${missing.join(', ')}.`);
    if (photos.length < 4)
      return setError('Adicione pelo menos 4 fotos: fachada, entrada, área interna e visão geral.');
    setLoading(true);
    setError('');
    setPhase('Localizando o endereço...');
    try {
      const query = `${addressLine}, ${addressNumber}, ${district}, ${city}, ${state}, ${postalCode}, Brasil`;
      let latitude: number | null = null;
      let longitude: number | null = null;
      try {
        const positions = await Promise.race([
          Location.geocodeAsync(query),
          new Promise<Location.LocationGeocodedLocation[]>((_, reject) =>
            setTimeout(() => reject(new Error('timeout')), 7000),
          ),
        ]);
        if (positions.length) {
          latitude = positions[0].latitude;
          longitude = positions[0].longitude;
        }
      } catch {
        /* O endereço textual já foi validado; coordenadas podem ser adicionadas depois. */
      }
      setPhase('Salvando o anúncio...');
      const amenities = features
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);
      const created = await createSpace(auth.token, {
        title: title.trim(),
        type,
        size: Math.max(1, Math.ceil(areaValue)),
        heightMeters: heightValue,
        lengthMeters: lengthValue,
        depthMeters: depthValue,
        price: priceValue,
        description: description.trim(),
        features: amenities.length ? amenities : ['Sem comodidades informadas'],
        postalCode,
        addressLine: addressLine.trim(),
        addressNumber: addressNumber.trim(),
        addressComplement: addressComplement.trim(),
        district: district.trim(),
        city: city.trim(),
        state: state.trim().toUpperCase(),
        latitude,
        longitude,
      });
      await auth.refreshProfile();
      setPhase('Enviando as fotos...');
      let failedPhotos = 0;
      for (const photo of photos) {
        try {
          await uploadSpacePhoto(auth.token, created.id, photo);
        } catch {
          failedPhotos++;
        }
      }
      if (failedPhotos)
        Alert.alert(
          'Anúncio cadastrado',
          `O anúncio foi salvo, mas ${failedPhotos} foto(s) não foram enviadas. Você poderá tentar novamente em Meus anúncios.`,
        );
      navigation.replace('ListingDocument', { spaceId: created.id, title: created.title });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível cadastrar o espaço.');
    } finally {
      setLoading(false);
      setPhase('');
    }
  }
  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.safe} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.header}>
          <Text onPress={navigation.goBack} style={styles.back}>
            ‹ Voltar
          </Text>
          <Text style={styles.headerTitle}>Novo anúncio</Text>
          <View style={{ width: 52 }} />
        </View>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          <Section title="Que espaço é esse?" />
          <Field
            label="Nome do anúncio"
            value={title}
            onChangeText={setTitle}
            placeholder="Ex.: Box seco próximo ao metrô"
          />
          <Text style={styles.label}>Categoria</Text>
          <ScrollView
            horizontal
            style={{ flexGrow: 0 }}
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.categories}
          >
            {categories.map((item) => (
              <Pressable
                key={item}
                onPress={() => setType(item)}
                style={[styles.category, type === item && styles.categoryActive]}
              >
                <Text style={[styles.categoryText, type === item && styles.categoryTextActive]}>{item}</Text>
              </Pressable>
            ))}
          </ScrollView>
          <Text style={styles.helper}>
            Informe as medidas internas. O StowUp calcula automaticamente a área, o volume e uma sugestão de
            preço.
          </Text>
          <View style={styles.row}>
            <View style={{ flex: 1 }}>
              <Field label="Altura (m)" value={height} onChangeText={setHeight} keyboardType="decimal-pad" />
            </View>
            <View style={{ flex: 1 }}>
              <Field
                label="Comprimento (m)"
                value={length}
                onChangeText={setLength}
                keyboardType="decimal-pad"
              />
            </View>
            <View style={{ flex: 1 }}>
              <Field
                label="Profundidade (m)"
                value={depth}
                onChangeText={setDepth}
                keyboardType="decimal-pad"
              />
            </View>
          </View>
          <DimensionSummary height={height} length={length} depth={depth} onUsePrice={setPrice} />
          <Field label="Preço/mês (R$)" value={price} onChangeText={setPrice} keyboardType="decimal-pad" />
          <Field
            label="Descrição detalhada"
            value={description}
            onChangeText={setDescription}
            multiline
            style={[styles.input, styles.textarea]}
            placeholder="Explique acesso, segurança, horários e restrições."
          />
          <Field
            label="Características informadas por você (separadas por vírgula)"
            value={features}
            onChangeText={setFeatures}
          />
          <Section title="Endereço do espaço" />
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
                <Text style={styles.cepText}>Preenchendo rua, bairro e cidade...</Text>
              </View>
            )}
          </View>
          <Field label="Rua/Avenida" value={addressLine} onChangeText={setAddressLine} />
          <View style={styles.row}>
            <View style={{ flex: 0.7 }}>
              <Field
                label="Número *"
                value={addressNumber}
                onChangeText={setAddressNumber}
                placeholder="Obrigatório"
              />
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
          <Section title="Fotos reais" />
          <Text style={styles.helper}>
            Mínimo obrigatório: 4 fotos, preferencialmente fachada, entrada, área interna e visão geral.
          </Text>
          <Pressable onPress={pickPhotos} style={styles.photoButton}>
            <Text style={styles.photoButtonText}>＋ Selecionar de 4 a 8 fotos</Text>
          </Pressable>
          <ScrollView horizontal style={{ flexGrow: 0 }} contentContainerStyle={{ gap: 9 }}>
            {photos.map((photo) => (
              <Image key={photo.uri} source={{ uri: photo.uri }} style={styles.preview} />
            ))}
          </ScrollView>
          <View style={styles.notice}>
            <Text style={styles.noticeTitle}>Documento obrigatório antes da publicação</Text>
            <Text style={styles.noticeText}>
              Depois de salvar, você deverá baixar e assinar o termo de responsabilidade do anunciante. O
              anúncio só será publicado após a análise do documento, das fotos e das informações.
            </Text>
          </View>
          {!!error && (
            <View style={styles.errorBox}>
              <Text style={styles.error}>{error}</Text>
            </View>
          )}
          {loading && <Text style={styles.phase}>{phase}</Text>}
          <PrimaryButton label="Continuar para assinatura" onPress={submit} loading={loading} />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
function Section({ title }: { title: string }) {
  return <Text style={styles.section}>{title}</Text>;
}
function Field({ label, style, ...props }: React.ComponentProps<typeof TextInput> & { label: string }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput {...props} placeholderTextColor="#98A2B3" style={[styles.input, style]} />
    </View>
  );
}
function DimensionSummary({
  height,
  length,
  depth,
  onUsePrice,
}: {
  height: string;
  length: string;
  depth: string;
  onUsePrice: (value: string) => void;
}) {
  const h = Number(height.replace(',', '.')) || 0;
  const l = Number(length.replace(',', '.')) || 0;
  const d = Number(depth.replace(',', '.')) || 0;
  const area = l * d;
  const volume = h * l * d;
  const suggestion = Math.max(50, Math.round(volume * 80));
  if (!volume) return null;
  return (
    <View style={styles.calculation}>
      <Text style={styles.calculationTitle}>Cálculo do espaço</Text>
      <Text style={styles.calculationText}>
        Área: {area.toFixed(2).replace('.', ',')} m² · Volume: {volume.toFixed(2).replace('.', ',')} m³
      </Text>
      <Pressable onPress={() => onUsePrice(String(suggestion))}>
        <Text style={styles.suggestedPrice}>Usar preço sugerido: R$ {suggestion},00/mês</Text>
      </Pressable>
    </View>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#fff' },
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
  headerTitle: { color: colors.text, fontSize: 18, fontWeight: '800' },
  content: { padding: 22, gap: 15, paddingBottom: 45 },
  section: {
    color: colors.navy,
    fontSize: 19,
    fontWeight: '900',
    marginTop: 10,
    paddingBottom: 5,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  row: { flexDirection: 'row', gap: 12 },
  field: { gap: 7 },
  label: { color: colors.text, fontSize: 14, fontWeight: '700' },
  helper: { color: colors.muted, fontSize: 12, lineHeight: 18 },
  calculation: { backgroundColor: '#F2F6FA', borderRadius: 14, padding: 14, gap: 5 },
  calculationTitle: { color: colors.navy, fontWeight: '900' },
  calculationText: { color: colors.muted, lineHeight: 19 },
  suggestedPrice: { color: colors.orange, fontWeight: '900', marginTop: 3 },
  input: {
    minHeight: 52,
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
  textarea: { minHeight: 120, paddingTop: 14, textAlignVertical: 'top' },
  categories: { gap: 8, paddingVertical: 3 },
  category: {
    borderWidth: 1,
    borderColor: colors.line,
    paddingHorizontal: 15,
    paddingVertical: 10,
    borderRadius: 18,
  },
  categoryActive: { backgroundColor: colors.navy, borderColor: colors.navy },
  categoryText: { color: colors.text, fontWeight: '700' },
  categoryTextActive: { color: '#fff' },
  photoButton: {
    height: 54,
    borderRadius: 15,
    borderWidth: 1.5,
    borderStyle: 'dashed',
    borderColor: colors.orange,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.orangeSoft,
  },
  photoButtonText: { color: colors.orange, fontWeight: '800' },
  preview: { width: 92, height: 92, borderRadius: 13 },
  notice: { padding: 15, borderRadius: 14, backgroundColor: '#F2F6FA' },
  noticeTitle: { color: colors.navy, fontWeight: '800' },
  noticeText: { color: colors.muted, lineHeight: 19, marginTop: 4 },
  errorBox: {
    padding: 13,
    borderRadius: 12,
    backgroundColor: '#FFF0F0',
    borderWidth: 1,
    borderColor: '#FFD0D0',
  },
  error: { color: colors.error, lineHeight: 20, fontWeight: '700' },
  phase: { color: colors.navy, textAlign: 'center', fontWeight: '700' },
});
