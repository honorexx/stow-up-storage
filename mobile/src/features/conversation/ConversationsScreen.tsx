import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useFocusEffect } from '@react-navigation/native';
import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { MainTabParamList, RootStackParamList } from '../../app/navigation/types';
import { Conversation, listConversations } from '../../core/api/marketplace';
import { colors } from '../../shared/theme';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { useAuth } from '../auth/AuthContext';
import { API_ORIGIN } from '../../core/api/auth';

export function ConversationsScreen({ navigation }: BottomTabScreenProps<MainTabParamList, 'Conversas'>) {
  const auth = useAuth();
  const root = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();
  const [items, setItems] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const refresh = useCallback(() => {
    if (!auth.token) return;
    setLoading(true);
    listConversations(auth.token)
      .then(setItems)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [auth.token]);
  useFocusEffect(refresh);
  useEffect(() => {
    if (!auth.token) return;
    const timer = setInterval(
      () =>
        listConversations(auth.token!)
          .then(setItems)
          .catch(() => undefined),
      8000,
    );
    return () => clearInterval(timer);
  }, [auth.token]);
  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Conversas</Text>
      </View>
      {!auth.user ? (
        <View style={styles.empty}>
          <Text style={styles.icon}>◉</Text>
          <Text style={styles.title}>Suas conversas ficam aqui</Text>
          <Text style={styles.subtitle}>
            Entre para conversar com proprietários e clientes com segurança.
          </Text>
          <View style={styles.button}>
            <PrimaryButton label="Entrar na conta" onPress={() => root?.navigate('Login')} />
          </View>
        </View>
      ) : loading && !items.length ? (
        <ActivityIndicator style={{ flex: 1 }} color={colors.orange} />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.id}
          contentContainerStyle={items.length ? styles.list : styles.emptyList}
          onRefresh={refresh}
          refreshing={loading}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.icon}>◉</Text>
              <Text style={styles.title}>Nenhuma conversa ainda</Text>
              <Text style={styles.subtitle}>
                {error || 'Abra um anúncio e toque em “Falar com o anunciante”.'}
              </Text>
            </View>
          }
          renderItem={({ item }) => (
            <Pressable
              onPress={() =>
                root?.navigate('Conversation', {
                  conversationId: item.id,
                  title: item.participantName,
                  participantPhotoUrl: item.participantPhotoUrl,
                })
              }
              style={styles.item}
            >
              <View style={styles.avatar}>
                {item.participantPhotoUrl ? (
                  <Image
                    source={{ uri: `${API_ORIGIN}${item.participantPhotoUrl}` }}
                    style={styles.avatarImage}
                  />
                ) : (
                  <Text style={styles.avatarText}>{item.participantName.slice(0, 1).toUpperCase()}</Text>
                )}
              </View>
              <View style={{ flex: 1 }}>
                <View style={styles.itemTop}>
                  <Text style={styles.name}>{item.participantName}</Text>
                  <Text style={styles.date}>{new Date(item.updatedAt).toLocaleDateString('pt-BR')}</Text>
                </View>
                <Text style={styles.space} numberOfLines={1}>
                  {item.spaceTitle}
                </Text>
                <Text style={styles.last} numberOfLines={1}>
                  {item.lastMessage}
                </Text>
              </View>
            </Pressable>
          )}
        />
      )}
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: {
    height: 62,
    paddingHorizontal: 20,
    justifyContent: 'center',
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  headerTitle: { color: colors.text, fontSize: 22, fontWeight: '900' },
  list: { padding: 15, gap: 10 },
  emptyList: { flexGrow: 1 },
  item: {
    flexDirection: 'row',
    gap: 13,
    padding: 15,
    borderRadius: 17,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: colors.line,
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 17,
    backgroundColor: colors.orangeSoft,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  avatarImage: { width: '100%', height: '100%' },
  avatarText: { color: colors.orange, fontWeight: '900', fontSize: 18 },
  itemTop: { flexDirection: 'row', justifyContent: 'space-between', gap: 8 },
  name: { color: colors.text, fontWeight: '800', fontSize: 16 },
  date: { color: colors.muted, fontSize: 11 },
  space: { color: colors.orange, fontSize: 12, fontWeight: '700', marginTop: 2 },
  last: { color: colors.muted, marginTop: 5 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 30 },
  icon: { color: colors.orange, fontSize: 42 },
  title: { color: colors.text, fontWeight: '900', fontSize: 21, marginTop: 12, textAlign: 'center' },
  subtitle: { color: colors.muted, textAlign: 'center', lineHeight: 21, marginTop: 7 },
  button: { width: '100%', marginTop: 20 },
});
