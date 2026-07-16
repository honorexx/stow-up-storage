import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Pressable, SafeAreaView, ScrollView, StyleSheet, Text, View } from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import { colors } from '../../shared/theme';

const sections = [
  [
    '1. A plataforma',
    'A StowUp conecta pessoas que disponibilizam espaços de armazenamento a pessoas interessadas em locá-los. A plataforma intermedeia tecnologia, comunicação, documentos e pagamentos, mas não se torna proprietária dos espaços nem dos bens armazenados.',
  ],
  [
    '2. Cadastro e identidade',
    'Você deve fornecer dados verdadeiros, proteger sua conta e manter telefone, CPF e endereço atualizados. Fraude, risco à segurança ou uso indevido podem levar à suspensão.',
  ],
  [
    '3. Anúncios',
    'O anunciante responde pela legitimidade, segurança, medidas, fotos, endereço e condições do espaço. Anúncios passam por moderação antes da publicação.',
  ],
  [
    '4. Pagamentos',
    'Valores, taxas e regras aparecem antes da contratação. O provedor de pagamento processa os dados sensíveis; a StowUp não armazena o número completo do cartão nem o código de segurança.',
  ],
  [
    '5. Contrato e assinatura',
    'Cada locação gera contrato próprio. A liberação pode depender de assinatura eletrônica e validação da autenticidade e integridade do PDF.',
  ],
  [
    '6. Itens proibidos',
    'Não armazene itens ilícitos, roubados, explosivos, drogas ilícitas, armas em desacordo com a lei, resíduos perigosos, animais, pessoas, perecíveis sem estrutura adequada ou itens que coloquem pessoas e patrimônio em risco.',
  ],
  [
    '7. Inadimplência',
    'Em atraso, haverá notificação para regularização e retirada. O anunciante não se torna automaticamente dono dos bens após 15 dias. Qualquer retenção, remoção, depósito, descarte ou alienação depende de procedimento juridicamente permitido e documentado.',
  ],
  [
    '8. Suporte, privacidade e segurança',
    'Chamados ficam registrados por protocolo. Dados são tratados para cadastro, contrato, pagamento, prevenção à fraude, suporte e obrigações legais. Situações urgentes também devem ser comunicadas às autoridades.',
  ],
];

export function TermsOfUseScreen({ navigation }: NativeStackScreenProps<RootStackParamList, 'TermsOfUse'>) {
  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()}>
          <Text style={styles.back}>‹</Text>
        </Pressable>
        <View>
          <Text style={styles.headerTitle}>Termos de Uso</Text>
          <Text style={styles.version}>Versão 13/07/2026</Text>
        </View>
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.warning}>
          <Text style={styles.warningTitle}>Documento em revisão jurídica</Text>
          <Text style={styles.warningText}>
            Esta versão integra o protótipo e precisa ser revisada por advogado antes da operação comercial
            real.
          </Text>
        </View>
        {sections.map(([title, body]) => (
          <View key={title} style={styles.section}>
            <Text style={styles.title}>{title}</Text>
            <Text style={styles.body}>{body}</Text>
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surface },
  header: {
    minHeight: 68,
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
  version: { color: colors.orange, fontSize: 11, fontWeight: '700', marginTop: 2 },
  content: { padding: 20, gap: 18, paddingBottom: 45 },
  warning: { backgroundColor: '#FFF4E8', borderRadius: 15, padding: 15 },
  warningTitle: { color: '#8A3B00', fontWeight: '900' },
  warningText: { color: '#8A3B00', lineHeight: 19, marginTop: 4, fontSize: 12 },
  section: { gap: 7 },
  title: { color: colors.navy, fontWeight: '900', fontSize: 17 },
  body: { color: colors.muted, lineHeight: 22, fontSize: 14 },
});
