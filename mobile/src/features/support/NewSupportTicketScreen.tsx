import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
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
import { createSupportTicket, SupportCategory } from '../../core/api/marketplace';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

const categories: { value: SupportCategory; title: string; description: string; icon: string }[] = [
  {
    value: 'PAYMENT',
    title: 'Pagamentos e reembolsos',
    description: 'Cobrança, cartão, repasse ou estorno.',
    icon: 'R$',
  },
  { value: 'LISTING', title: 'Meu anúncio', description: 'Cadastro, fotos, edição ou aprovação.', icon: '▣' },
  {
    value: 'BOOKING',
    title: 'Reserva ou locação',
    description: 'Datas, acesso, cancelamento ou problema no espaço.',
    icon: '⌂',
  },
  {
    value: 'CONTRACT',
    title: 'Contrato e assinatura',
    description: 'PDF, assinatura gov.br ou liberação.',
    icon: '✓',
  },
  {
    value: 'SECURITY',
    title: 'Segurança',
    description: 'Denúncia, item proibido ou situação urgente.',
    icon: '!',
  },
  {
    value: 'ACCOUNT',
    title: 'Minha conta',
    description: 'Cadastro, CPF, telefone, endereço ou acesso.',
    icon: '@',
  },
  {
    value: 'OTHER',
    title: 'Outros',
    description: 'Não encontrou o motivo? Explique para a equipe.',
    icon: '…',
  },
];

export function NewSupportTicketScreen({
  navigation,
}: NativeStackScreenProps<RootStackParamList, 'NewSupportTicket'>) {
  const { token } = useAuth();
  const [category, setCategory] = useState<SupportCategory | null>(null);
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [sending, setSending] = useState(false);
  async function submit() {
    if (!token || !category)
      return Alert.alert('Escolha o assunto', 'Selecione uma das opções para direcionarmos seu chamado.');
    if (subject.trim().length < 4 || description.trim().length < 10)
      return Alert.alert(
        'Conte um pouco mais',
        'Informe um título e uma descrição com pelo menos 10 caracteres.',
      );
    setSending(true);
    try {
      const ticket = await createSupportTicket(token, {
        category,
        subject: subject.trim(),
        description: description.trim(),
      });
      navigation.replace('SupportTicketChat', {
        ticketId: ticket.id,
        ticketNumber: ticket.ticketNumber,
        subject: ticket.subject,
        status: ticket.status,
      });
    } catch (e: any) {
      Alert.alert('Não foi possível abrir o chamado', e.message);
    } finally {
      setSending(false);
    }
  }
  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.header}>
          <Pressable onPress={() => navigation.goBack()}>
            <Text style={styles.back}>‹</Text>
          </Pressable>
          <View>
            <Text style={styles.headerTitle}>Novo chamado</Text>
            <Text style={styles.headerSub}>Vamos direcionar você para a equipe certa</Text>
          </View>
        </View>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          <Text style={styles.section}>Qual é o motivo?</Text>
          {categories.map((item) => (
            <Pressable
              key={item.value}
              onPress={() => {
                setCategory(item.value);
                if (item.value !== 'OTHER' && !subject) setSubject(item.title);
              }}
              style={[styles.category, category === item.value && styles.selected]}
            >
              <View style={styles.icon}>
                <Text style={styles.iconText}>{item.icon}</Text>
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.categoryTitle}>{item.title}</Text>
                <Text style={styles.categoryDescription}>{item.description}</Text>
              </View>
              <Text style={styles.check}>{category === item.value ? '●' : '○'}</Text>
            </Pressable>
          ))}
          <Text style={styles.label}>Título do chamado</Text>
          <TextInput
            value={subject}
            onChangeText={setSubject}
            maxLength={140}
            placeholder="Ex.: Não consigo enviar meu anúncio"
            style={styles.input}
          />
          <Text style={styles.label}>Descreva o que aconteceu</Text>
          <TextInput
            value={description}
            onChangeText={setDescription}
            maxLength={2000}
            placeholder="Inclua detalhes que ajudem nossa equipe a entender e resolver."
            multiline
            style={[styles.input, styles.area]}
          />
          <PrimaryButton label="Abrir chamado" onPress={submit} loading={sending} />
          <Text style={styles.notice}>
            Não envie senhas, número completo de cartão ou código de segurança.
          </Text>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    minHeight: 70,
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  back: { fontSize: 38, color: colors.text, lineHeight: 42 },
  headerTitle: { fontSize: 19, fontWeight: '900', color: colors.text },
  headerSub: { color: colors.muted, fontSize: 11, marginTop: 2 },
  content: { padding: 18, gap: 10, paddingBottom: 40 },
  section: { color: colors.text, fontSize: 20, fontWeight: '900', marginBottom: 4 },
  category: {
    flexDirection: 'row',
    gap: 12,
    alignItems: 'center',
    padding: 14,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: '#fff',
  },
  selected: { borderColor: colors.orange, backgroundColor: '#FFF8F3' },
  icon: {
    width: 38,
    height: 38,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#EDF2F8',
  },
  iconText: { color: colors.navy, fontWeight: '900' },
  categoryTitle: { color: colors.text, fontWeight: '800' },
  categoryDescription: { color: colors.muted, fontSize: 12, marginTop: 3, lineHeight: 17 },
  check: { color: colors.orange, fontSize: 20 },
  label: { color: colors.text, fontWeight: '800', marginTop: 10 },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    paddingHorizontal: 14,
    minHeight: 48,
    color: colors.text,
  },
  area: { minHeight: 125, paddingTop: 13, textAlignVertical: 'top' },
  notice: { textAlign: 'center', color: colors.muted, fontSize: 11, lineHeight: 16, marginTop: 3 },
});
