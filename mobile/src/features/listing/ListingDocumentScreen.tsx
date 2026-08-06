import { NativeStackScreenProps } from '@react-navigation/native-stack';
import * as DocumentPicker from 'expo-document-picker';
import * as WebBrowser from 'expo-web-browser';
import { useEffect, useState } from 'react';
import { Alert, Pressable, SafeAreaView, ScrollView, StyleSheet, Text, View } from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import {
  getListingDocument,
  listSpaceDocuments,
  ListingDocument,
  listingDocumentPdfUrl,
  SpaceDocument,
  uploadSignedListingDocument,
  uploadSpaceDocument,
} from '../../core/api/marketplace';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';
import { downloadAuthenticatedPdf } from '../../core/files/download';

export function ListingDocumentScreen({
  navigation,
  route,
}: NativeStackScreenProps<RootStackParamList, 'ListingDocument'>) {
  const { token } = useAuth();
  const [document, setDocument] = useState<ListingDocument | null>(null);
  const [busy, setBusy] = useState('');
  const [extraDocs, setExtraDocs] = useState<SpaceDocument[]>([]);
  useEffect(() => {
    if (!token) return;
    const refresh = () => {
      getListingDocument(token, route.params.spaceId)
        .then(setDocument)
        .catch(() => undefined);
      listSpaceDocuments(token, route.params.spaceId)
        .then(setExtraDocs)
        .catch(() => undefined);
    };
    refresh();
    const timer = setInterval(refresh, 8000);
    return () => clearInterval(timer);
  }, [token, route.params.spaceId]);
  async function download() {
    if (!token) return;
    setBusy('download');
    try {
      await downloadAuthenticatedPdf(
        listingDocumentPdfUrl(route.params.spaceId),
        token,
        `termo-anunciante-${route.params.spaceId}.pdf`,
      );
    } catch (e: any) {
      Alert.alert('Documento', e.message);
    } finally {
      setBusy('');
    }
  }
  async function upload() {
    if (!token) return;
    const result = await DocumentPicker.getDocumentAsync({
      type: 'application/pdf',
      copyToCacheDirectory: true,
    });
    if (result.canceled) return;
    setBusy('upload');
    try {
      setDocument(
        await uploadSignedListingDocument(token, route.params.spaceId, {
          uri: result.assets[0].uri,
          name: result.assets[0].name,
          mimeType: result.assets[0].mimeType,
        }),
      );
      Alert.alert('Documento enviado', 'A equipe de documentação recebeu o PDF e fará a validação.');
    } catch (e: any) {
      Alert.alert('Envio', e.message);
    } finally {
      setBusy('');
    }
  }
  async function uploadExtra() {
    if (!token) return;
    const result = await DocumentPicker.getDocumentAsync({
      type: ['application/pdf', 'image/*'],
      copyToCacheDirectory: true,
    });
    if (result.canceled) return;
    setBusy('extra');
    try {
      const a = result.assets[0];
      await uploadSpaceDocument(
        token,
        route.params.spaceId,
        { uri: a.uri, name: a.name, mimeType: a.mimeType },
        'COMPLEMENTARY',
      );
      setExtraDocs(await listSpaceDocuments(token, route.params.spaceId));
      Alert.alert('Documento salvo', 'O arquivo ficará disponível para a equipe administrativa.');
    } catch (e: any) {
      Alert.alert('Documento complementar', e.message);
    } finally {
      setBusy('');
    }
  }
  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Pressable
          onPress={() =>
            navigation.reset({ index: 0, routes: [{ name: 'Main', params: { screen: 'Explorar' } }] })
          }
        >
          <Text style={styles.back}>‹ Início</Text>
        </Pressable>
        <Text style={styles.headerTitle}>Termo do anunciante</Text>
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>{route.params.title}</Text>
        <Text style={styles.subtitle}>
          Este documento registra que o espaço é seu ou está sob sua responsabilidade e define seus deveres de
          guarda, segurança, acesso e comunicação de incidentes.
        </Text>
        <Step n="1" title="Baixe e confira o termo">
          <PrimaryButton
            label="Salvar termo em PDF"
            onPress={download}
            loading={busy === 'download'}
            secondary
          />
        </Step>
        <Step n="2" title="Assine pelo gov.br">
          <PrimaryButton
            label="Abrir assinador gov.br"
            onPress={() => WebBrowser.openBrowserAsync('https://assinador.iti.br/')}
            secondary
          />
        </Step>
        <Step n="3" title="Envie o PDF original assinado">
          <PrimaryButton
            label={document?.signedAt ? 'Reenviar PDF assinado' : 'Selecionar PDF assinado'}
            onPress={upload}
            loading={busy === 'upload'}
          />
        </Step>
        <View style={styles.status}>
          <Text style={styles.statusTitle}>Situação: {label(document?.status)}</Text>
          <Text style={styles.statusText}>{document?.reviewNotes || help(document?.status)}</Text>
        </View>
        <View style={styles.status}>
          <Text style={styles.statusTitle}>Documentação complementar do espaço</Text>
          <Text style={styles.statusText}>
            Envie licenças, seguros ou outros documentos relevantes. Por padrão, somente a administração
            poderá acessá-los.
          </Text>
          {extraDocs.map((d) => (
            <Text key={d.id} style={styles.doc}>
              ✓ {d.name} · {d.visibility}
            </Text>
          ))}
          <View style={{ marginTop: 12 }}>
            <PrimaryButton
              label="Anexar documento complementar"
              onPress={uploadExtra}
              loading={busy === 'extra'}
              secondary
            />
          </View>
        </View>
        <Text style={styles.legal}>
          A aprovação documental não publica o anúncio sozinha. A equipe também verificará endereço, descrição
          e fotos.
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}
function Step({ n, title, children }: { n: string; title: string; children: React.ReactNode }) {
  return (
    <View style={styles.step}>
      <View style={styles.number}>
        <Text style={styles.numberText}>{n}</Text>
      </View>
      <View style={{ flex: 1, gap: 12 }}>
        <Text style={styles.stepTitle}>{title}</Text>
        {children}
      </View>
    </View>
  );
}
function label(status?: string) {
  return (
    (
      {
        GENERATED: 'Aguardando assinatura',
        PENDING_REVIEW: 'Em análise',
        APPROVED: 'Documento aprovado',
        REJECTED: 'Correção necessária',
      } as Record<string, string>
    )[status || ''] || 'Carregando'
  );
}
function help(status?: string) {
  if (status === 'PENDING_REVIEW')
    return 'A atualização ocorrerá automaticamente quando a equipe concluir a análise.';
  if (status === 'APPROVED') return 'Documento validado. Agora falta apenas a aprovação do anúncio.';
  if (status === 'REJECTED') return 'Confira a orientação, corrija e envie novamente.';
  return 'Baixe, assine e envie o arquivo original.';
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
  back: { color: colors.navy, fontWeight: '800' },
  headerTitle: { color: colors.text, fontWeight: '900', fontSize: 17 },
  content: { padding: 22, gap: 16, paddingBottom: 45 },
  title: { color: colors.text, fontWeight: '900', fontSize: 24 },
  subtitle: { color: colors.muted, lineHeight: 21 },
  step: {
    flexDirection: 'row',
    gap: 13,
    padding: 17,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 17,
  },
  number: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: colors.orange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  numberText: { color: '#fff', fontWeight: '900' },
  stepTitle: { color: colors.navy, fontWeight: '900', fontSize: 16 },
  status: { padding: 16, borderRadius: 15, backgroundColor: '#F2F6FA' },
  statusTitle: { color: colors.navy, fontWeight: '900' },
  statusText: { color: colors.muted, lineHeight: 20, marginTop: 5 },
  doc: { color: colors.text, fontSize: 12, marginTop: 8 },
  legal: { color: colors.muted, fontSize: 12, lineHeight: 18 },
});
