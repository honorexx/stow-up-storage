import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import * as DocumentPicker from 'expo-document-picker';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import {
  listSupportTicketMessages,
  sendSupportTicketMessage,
  uploadSupportAttachment,
  SupportTicketMessage,
} from '../../core/api/marketplace';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';

export function SupportTicketChatScreen({
  route,
  navigation,
}: NativeStackScreenProps<RootStackParamList, 'SupportTicketChat'>) {
  const { token } = useAuth();
  const { ticketId, ticketNumber, subject, status } = route.params;
  const [messages, setMessages] = useState<SupportTicketMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const load = useCallback(() => {
    if (!token) return;
    listSupportTicketMessages(token, ticketId)
      .then(setMessages)
      .catch((e) => Alert.alert('Erro', e.message))
      .finally(() => setLoading(false));
  }, [token, ticketId]);
  useEffect(() => {
    load();
    const timer = setInterval(load, 2000);
    return () => clearInterval(timer);
  }, [load]);
  async function send() {
    const body = draft.trim();
    if (!body || !token || status === 'CLOSED') return;
    setSending(true);
    try {
      const message = await sendSupportTicketMessage(token, ticketId, body);
      setMessages((current) => [...current, message]);
      setDraft('');
    } catch (e: any) {
      Alert.alert('Não foi possível enviar', e.message);
    } finally {
      setSending(false);
    }
  }
  async function attach() {
    if (!token) return;
    const result = await DocumentPicker.getDocumentAsync({
      type: ['application/pdf', 'image/*', 'text/plain', 'application/zip', 'application/msword'],
      copyToCacheDirectory: true,
    });
    if (result.canceled) return;
    const file = result.assets[0];
    setSending(true);
    try {
      const message = await uploadSupportAttachment(token, ticketId, {
        uri: file.uri,
        name: file.name,
        mimeType: file.mimeType,
      });
      setMessages((current) => [...current.filter((item) => item.id !== message.id), message]);
    } catch (e: any) {
      Alert.alert('Não foi possível enviar o anexo', e.message);
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
          <View style={{ flex: 1 }}>
            <Text numberOfLines={1} style={styles.headerTitle}>
              {subject}
            </Text>
            <Text style={styles.protocol}>
              {ticketNumber} · {status === 'OPEN' ? 'Em atendimento' : 'Encerrado'}
            </Text>
          </View>
        </View>
        {loading ? (
          <ActivityIndicator style={{ flex: 1 }} color={colors.orange} />
        ) : (
          <FlatList
            data={messages}
            keyExtractor={(item) => String(item.id)}
            contentContainerStyle={styles.messages}
            renderItem={({ item }) => (
              <View style={[styles.bubble, item.senderType === 'CUSTOMER' ? styles.mine : styles.theirs]}>
                <Text style={[styles.sender, item.senderType === 'CUSTOMER' && styles.white]}>
                  {item.senderType === 'CUSTOMER' ? 'Você' : item.senderName}
                </Text>
                <Text style={[styles.body, item.senderType === 'CUSTOMER' && styles.white]}>{item.body}</Text>
                {!!item.attachmentName && (
                  <View style={styles.attachment}>
                    <Text style={styles.attachmentIcon}>📎</Text>
                    <Text
                      numberOfLines={1}
                      style={[styles.attachmentName, item.senderType === 'CUSTOMER' && styles.white]}
                    >
                      {item.attachmentName}
                    </Text>
                  </View>
                )}
                <Text style={[styles.time, item.senderType === 'CUSTOMER' && styles.softWhite]}>
                  {new Date(item.createdAt).toLocaleDateString('pt-BR')}{' '}
                  {new Date(item.createdAt).toLocaleTimeString('pt-BR', {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </Text>
              </View>
            )}
          />
        )}
        {status === 'CLOSED' ? (
          <View style={styles.closed}>
            <Text style={styles.closedText}>
              Este chamado foi encerrado. Abra um novo chamado se precisar de ajuda.
            </Text>
          </View>
        ) : (
          <View style={styles.composer}>
            <Pressable disabled={sending} onPress={attach} style={styles.attach}>
              <Text style={styles.attachText}>＋</Text>
            </Pressable>
            <TextInput
              value={draft}
              onChangeText={setDraft}
              placeholder="Escreva sua mensagem..."
              placeholderTextColor={colors.muted}
              style={styles.input}
              multiline
              maxLength={2000}
            />
            <Pressable
              disabled={sending || !draft.trim()}
              onPress={send}
              style={[styles.send, (!draft.trim() || sending) && { opacity: 0.45 }]}
            >
              <Text style={styles.sendText}>➤</Text>
            </Pressable>
          </View>
        )}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
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
  headerTitle: { color: colors.text, fontWeight: '900', fontSize: 17 },
  protocol: { color: colors.orange, fontSize: 11, fontWeight: '700', marginTop: 3 },
  messages: { padding: 16, gap: 10, flexGrow: 1 },
  bubble: { maxWidth: '84%', borderRadius: 18, paddingHorizontal: 14, paddingVertical: 10 },
  mine: { alignSelf: 'flex-end', backgroundColor: colors.navy, borderBottomRightRadius: 5 },
  theirs: {
    alignSelf: 'flex-start',
    backgroundColor: '#fff',
    borderBottomLeftRadius: 5,
    borderWidth: 1,
    borderColor: colors.line,
  },
  sender: { color: colors.orange, fontSize: 11, fontWeight: '900', marginBottom: 4 },
  body: { color: colors.text, fontSize: 15, lineHeight: 21 },
  time: { color: colors.muted, fontSize: 9, alignSelf: 'flex-end', marginTop: 5 },
  white: { color: '#fff' },
  softWhite: { color: '#D8E5F2' },
  composer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 10,
    padding: 12,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: colors.line,
  },
  input: {
    flex: 1,
    minHeight: 44,
    maxHeight: 110,
    borderRadius: 18,
    backgroundColor: colors.background,
    paddingHorizontal: 15,
    paddingVertical: 11,
    color: colors.text,
  },
  attach: {
    width: 44,
    height: 44,
    borderRadius: 15,
    backgroundColor: '#EDF2F8',
    alignItems: 'center',
    justifyContent: 'center',
  },
  attachText: { color: colors.navy, fontSize: 24, fontWeight: '700' },
  attachment: { flexDirection: 'row', gap: 6, alignItems: 'center', marginTop: 7 },
  attachmentIcon: { fontSize: 14 },
  attachmentName: { flex: 1, color: colors.navy, fontWeight: '800', fontSize: 12 },
  send: {
    width: 44,
    height: 44,
    borderRadius: 15,
    backgroundColor: colors.orange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendText: { color: '#fff', fontSize: 18, fontWeight: '900' },
  closed: { margin: 12, borderRadius: 14, padding: 13, backgroundColor: '#EEF1F5' },
  closedText: { color: colors.muted, textAlign: 'center', lineHeight: 18, fontSize: 12 },
});
