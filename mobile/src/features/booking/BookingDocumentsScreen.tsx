import { NativeStackScreenProps } from '@react-navigation/native-stack';
import * as DocumentPicker from 'expo-document-picker';
import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';
import * as WebBrowser from 'expo-web-browser';
import { useEffect, useState } from 'react';
import { Alert, Pressable, SafeAreaView, ScrollView, StyleSheet, Text, View } from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import {
  contractPdfUrl,
  createPaymentCheckout,
  getRentalContract,
  RentalContract,
  uploadSignedContract,
} from '../../core/api/payments';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

export function BookingDocumentsScreen({
  navigation,
  route,
}: NativeStackScreenProps<RootStackParamList, 'BookingDocuments'>) {
  const { token } = useAuth();
  const booking = route.params.booking;
  const [contract, setContract] = useState<RentalContract | null>(null);
  const [busy, setBusy] = useState('');
  useEffect(() => {
    if (!token) return;
    let mounted = true;
    const refresh = () =>
      getRentalContract(token, booking.id)
        .then((value) => {
          if (mounted) setContract(value);
        })
        .catch(() => undefined);
    refresh();
    const timer = setInterval(refresh, 5000);
    return () => {
      mounted = false;
      clearInterval(timer);
    };
  }, [token, booking.id]);
  async function pay() {
    if (!token) return;
    if (contract?.status !== 'APPROVED') {
      Alert.alert('Contrato pendente', 'O pagamento será liberado quando o contrato assinado for aprovado.');
      return;
    }
    setBusy('pay');
    try {
      const checkout = await createPaymentCheckout(token, booking.id);
      await WebBrowser.openBrowserAsync(checkout.checkoutUrl, {
        presentationStyle: WebBrowser.WebBrowserPresentationStyle.PAGE_SHEET,
      });
      setContract(await getRentalContract(token, booking.id));
    } catch (e: any) {
      Alert.alert('Pagamento', e.message);
    } finally {
      setBusy('');
    }
  }
  async function download() {
    if (!token) return;
    setBusy('download');
    try {
      const destination = `${FileSystem.cacheDirectory}${booking.contract}.pdf`;
      await FileSystem.deleteAsync(destination, { idempotent: true });
      const result = await FileSystem.downloadAsync(contractPdfUrl(booking.id), destination, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (result.status < 200 || result.status >= 300) {
        await FileSystem.deleteAsync(result.uri, { idempotent: true });
        throw new Error('Não foi possível gerar o contrato em PDF. Tente novamente.');
      }
      if (await Sharing.isAvailableAsync())
        await Sharing.shareAsync(result.uri, {
          mimeType: 'application/pdf',
          dialogTitle: 'Salvar ou enviar contrato StowUp',
        });
      else Alert.alert('Contrato salvo', result.uri);
    } catch (e: any) {
      Alert.alert('Contrato', e.message);
    } finally {
      setBusy('');
    }
  }
  async function chooseSigned() {
    if (!token) return;
    const result = await DocumentPicker.getDocumentAsync({
      type: 'application/pdf',
      copyToCacheDirectory: true,
      multiple: false,
    });
    if (result.canceled) return;
    setBusy('upload');
    try {
      setContract(
        await uploadSignedContract(token, booking.id, {
          uri: result.assets[0].uri,
          name: result.assets[0].name,
          mimeType: result.assets[0].mimeType,
        }),
      );
      Alert.alert('PDF enviado', 'A equipe poderá validar a assinatura e o nome no VALIDAR/ITI.');
    } catch (e: any) {
      Alert.alert('Envio do contrato', e.message);
    } finally {
      setBusy('');
    }
  }
  const canUpload = !contract || contract.status === 'GENERATED' || contract.status === 'REJECTED';
  const canPay = contract?.status === 'APPROVED' && !contract.paymentApproved;
  const uploadLabel =
    contract?.status === 'REJECTED'
      ? 'Reenviar PDF corrigido'
      : contract?.status === 'PENDING_REVIEW'
        ? 'PDF enviado — em análise'
        : contract?.status === 'APPROVED'
          ? 'Contrato aprovado'
          : 'Selecionar PDF assinado';
  const paymentLabel = contract?.paymentApproved
    ? 'Pagamento aprovado'
    : canPay
      ? 'Pagar com Mercado Pago'
      : 'Aguardando aprovação do contrato';
  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()}>
          <Text style={styles.back}>‹</Text>
        </Pressable>
        <View>
          <Text style={styles.headerTitle}>Contrato e pagamento</Text>
          <Text style={styles.protocol}>
            {booking.id} · {booking.contract}
          </Text>
        </View>
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.summary}>
          <Text style={styles.title}>{booking.spaceTitle}</Text>
          <Text style={styles.total}>R$ {Number(booking.total).toFixed(2).replace('.', ',')}</Text>
          <Text style={styles.muted}>
            {booking.months} {booking.months === 1 ? 'mês' : 'meses'} · início{' '}
            {new Date(`${booking.start}T12:00:00`).toLocaleDateString('pt-BR')}
          </Text>
        </View>
        <Step
          number="1"
          title="Baixe e confira o contrato"
          text="Confira seus dados, os limites da intermediação, as regras do espaço e os itens proibidos."
        >
          <PrimaryButton
            label="Salvar contrato PDF"
            onPress={download}
            loading={busy === 'download'}
            secondary
          />
        </Step>
        <Step
          number="2"
          title="Assine como locatário"
          text="Somente você, locatário, deve assinar este PDF pelo gov.br. O anunciante já assinou o termo de responsabilidade pelo espaço e pela guarda."
        >
          <PrimaryButton
            label="Abrir assinatura gov.br"
            onPress={() => WebBrowser.openBrowserAsync('https://assinador.iti.br/')}
            secondary
          />
        </Step>
        <Step
          number="3"
          title="Envie o PDF assinado"
          text="A equipe verificará a integridade, a assinatura e o seu nome. Se houver rejeição, o motivo aparecerá aqui para você corrigir e reenviar."
        >
          <Status contract={contract} />
          <PrimaryButton
            label={uploadLabel}
            onPress={chooseSigned}
            loading={busy === 'upload'}
            disabled={!canUpload}
          />
        </Step>
        <Step
          number="4"
          title="Faça o pagamento"
          text="O pagamento é liberado somente depois que o contrato assinado for aprovado. O Mercado Pago protege os dados do seu cartão."
        >
          <PrimaryButton label={paymentLabel} onPress={pay} loading={busy === 'pay'} disabled={!canPay} />
        </Step>
        <Text style={styles.legal}>
          Fluxo da reserva: assinatura do locatário, validação do contrato e, por último, pagamento. A reserva
          é confirmada após o pagamento aprovado.
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}
function Step({
  number,
  title,
  text,
  children,
}: React.PropsWithChildren<{ number: string; title: string; text: string }>) {
  return (
    <View style={styles.step}>
      <View style={styles.stepHead}>
        <View style={styles.number}>
          <Text style={styles.numberText}>{number}</Text>
        </View>
        <Text style={styles.stepTitle}>{title}</Text>
      </View>
      <Text style={styles.stepText}>{text}</Text>
      <View style={{ marginTop: 12 }}>{children}</View>
    </View>
  );
}
function Status({ contract }: { contract: RentalContract | null }) {
  if (!contract) return null;
  const labels = {
    GENERATED: 'Aguardando o PDF assinado',
    PENDING_REVIEW: 'Em validação pela equipe',
    APPROVED: 'Contrato aprovado — pagamento liberado',
    REJECTED: 'Documento rejeitado — corrija e reenvie',
  };
  return (
    <View style={[styles.status, contract.status === 'REJECTED' && styles.statusRejected]}>
      <Text style={styles.statusTitle}>{labels[contract.status]}</Text>
      {contract.status === 'REJECTED' ? (
        <>
          <Text style={styles.reasonLabel}>Motivo da rejeição</Text>
          <Text style={styles.statusText}>
            {contract.reviewNotes || 'A equipe solicitou uma correção no documento.'}
          </Text>
        </>
      ) : contract.reviewNotes ? (
        <Text style={styles.statusText}>{contract.reviewNotes}</Text>
      ) : null}
    </View>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    minHeight: 68,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingHorizontal: 18,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  back: { fontSize: 38, lineHeight: 42, color: colors.text },
  headerTitle: { color: colors.text, fontWeight: '900', fontSize: 19 },
  protocol: { color: colors.orange, fontSize: 11, fontWeight: '700', marginTop: 2 },
  content: { padding: 18, gap: 14, paddingBottom: 45 },
  summary: { backgroundColor: colors.navy, borderRadius: 20, padding: 19 },
  title: { color: '#fff', fontWeight: '900', fontSize: 20 },
  total: { color: '#fff', fontWeight: '900', fontSize: 25, marginTop: 14 },
  muted: { color: '#C8D8E8', marginTop: 4 },
  step: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: 18, padding: 17 },
  stepHead: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  number: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: '#FFF0E8',
    alignItems: 'center',
    justifyContent: 'center',
  },
  numberText: { color: colors.orange, fontWeight: '900' },
  stepTitle: { color: colors.text, fontWeight: '900', fontSize: 17 },
  stepText: { color: colors.muted, lineHeight: 20, marginTop: 9 },
  status: { marginTop: 12, marginBottom: 12, backgroundColor: '#E8F7EF', borderRadius: 12, padding: 11 },
  statusRejected: { backgroundColor: '#FDECEC' },
  statusTitle: { color: colors.text, fontWeight: '900', fontSize: 12 },
  reasonLabel: { color: '#9E2F2F', fontWeight: '900', fontSize: 11, marginTop: 8 },
  statusText: { color: colors.muted, fontSize: 11, marginTop: 3, lineHeight: 16 },
  legal: { color: colors.muted, fontSize: 11, lineHeight: 17, textAlign: 'center' },
});
