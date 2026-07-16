import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import {
  FlatList,
  Image,
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
import { listRentalMessages, RentalMessage, sendRentalMessage } from '../../core/api/marketplace';
import { colors } from '../../shared/theme';
import { useAuth } from '../auth/AuthContext';
import { API_ORIGIN } from '../../core/api/auth';

export function ConversationScreen({
  navigation,
  route,
}: NativeStackScreenProps<RootStackParamList, 'Conversation'>) {
  const auth = useAuth();
  const [messages, setMessages] = useState<RentalMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  async function refresh() {
    if (!auth.token) return;
    try {
      setMessages(await listRentalMessages(auth.token, route.params.conversationId));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível carregar.');
    }
  }
  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 5000);
    return () => clearInterval(timer);
  }, [auth.token, route.params.conversationId]);
  async function send() {
    const body = draft.trim();
    if (!body || !auth.token || sending) return;
    setSending(true);
    setError('');
    try {
      const message = await sendRentalMessage(auth.token, route.params.conversationId, body);
      setMessages((current) => [...current, message]);
      setDraft('');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível enviar.');
    } finally {
      setSending(false);
    }
  }
  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.safe} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.header}>
          <Text onPress={navigation.goBack} style={styles.back}>
            ‹
          </Text>
          <View style={styles.avatar}>
            {route.params.participantPhotoUrl ? (
              <Image
                source={{ uri: `${API_ORIGIN}${route.params.participantPhotoUrl}` }}
                style={styles.avatarImage}
              />
            ) : (
              <Text style={styles.avatarText}>{route.params.title.slice(0, 1).toUpperCase()}</Text>
            )}
          </View>
          <View style={styles.identity}>
            <Text style={styles.title}>{route.params.title}</Text>
            <Text style={styles.secure}>Conversa protegida pelo StowUp</Text>
          </View>
        </View>
        {!!error && <Text style={styles.error}>{error}</Text>}
        <FlatList
          data={messages}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={styles.messages}
          renderItem={({ item }) => (
            <View style={[styles.bubble, item.mine ? styles.mine : styles.theirs]}>
              <Text style={[styles.body, item.mine && styles.mineBody]}>{item.body}</Text>
              <Text style={[styles.time, item.mine && styles.mineTime]}>
                {new Date(item.createdAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
              </Text>
            </View>
          )}
        />
        <View style={styles.composer}>
          <TextInput
            value={draft}
            onChangeText={setDraft}
            multiline
            placeholder="Escreva uma mensagem"
            placeholderTextColor={colors.muted}
            style={styles.input}
          />
          <Pressable onPress={send} style={styles.send}>
            <Text style={styles.sendText}>{sending ? '…' : '➤'}</Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#F5F7FA' },
  header: {
    height: 64,
    backgroundColor: '#fff',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 15,
    paddingHorizontal: 18,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  back: { color: colors.navy, fontSize: 32 },
  avatar: {
    width: 42,
    height: 42,
    borderRadius: 14,
    overflow: 'hidden',
    backgroundColor: colors.orangeSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarImage: { width: '100%', height: '100%' },
  avatarText: { color: colors.orange, fontWeight: '900', fontSize: 17 },
  identity: { flex: 1 },
  title: { color: colors.text, fontWeight: '900', fontSize: 17 },
  secure: { color: colors.muted, fontSize: 11, marginTop: 2 },
  error: { color: colors.error, padding: 10, textAlign: 'center' },
  messages: { padding: 15, gap: 8, flexGrow: 1, justifyContent: 'flex-end' },
  bubble: { maxWidth: '82%', borderRadius: 17, paddingHorizontal: 13, paddingVertical: 9 },
  mine: { alignSelf: 'flex-end', backgroundColor: colors.navy, borderBottomRightRadius: 5 },
  theirs: { alignSelf: 'flex-start', backgroundColor: '#fff', borderBottomLeftRadius: 5 },
  body: { color: colors.text, fontSize: 15, lineHeight: 20 },
  mineBody: { color: '#fff' },
  time: { color: colors.muted, fontSize: 9, marginTop: 4, textAlign: 'right' },
  mineTime: { color: '#ffffff99' },
  composer: {
    flexDirection: 'row',
    gap: 9,
    alignItems: 'flex-end',
    padding: 10,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: colors.line,
  },
  input: {
    flex: 1,
    maxHeight: 110,
    minHeight: 45,
    borderRadius: 16,
    backgroundColor: '#F1F4F7',
    paddingHorizontal: 14,
    paddingVertical: 11,
    color: colors.text,
  },
  send: {
    width: 45,
    height: 45,
    borderRadius: 16,
    backgroundColor: colors.orange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendText: { color: '#fff', fontSize: 18, fontWeight: '900' },
});
