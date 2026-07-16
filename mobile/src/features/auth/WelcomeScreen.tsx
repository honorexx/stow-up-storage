import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { RootStackParamList } from '../../app/navigation/types';
import { BrandMark } from '../../shared/components/BrandMark';
import { PrimaryButton } from '../../shared/components/PrimaryButton';
import { colors } from '../../shared/theme';

type Props = NativeStackScreenProps<RootStackParamList, 'Welcome'>;

export function WelcomeScreen({ navigation }: Props) {
  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.hero}>
        <BrandMark light />
        <View>
          <Text style={styles.title}>Espaço para o que importa.</Text>
          <Text style={styles.subtitle}>
            Encontre boxes, garagens e depósitos próximos, com segurança e praticidade.
          </Text>
        </View>
        <View style={styles.illustration}>
          <View style={styles.pin}>
            <Text style={styles.pinText}>⌖</Text>
          </View>
          <View style={[styles.card, styles.cardBack]} />
          <View style={styles.card}>
            <View style={styles.cardImage} />
            <View style={styles.cardLine} />
            <View style={[styles.cardLine, styles.short]} />
          </View>
        </View>
      </View>
      <View style={styles.actions}>
        <PrimaryButton label="Criar minha conta" onPress={() => navigation.navigate('Register')} />
        <PrimaryButton label="Já tenho uma conta" secondary onPress={() => navigation.navigate('Login')} />
        <Text onPress={() => navigation.replace('Main')} style={styles.explore}>
          Explorar primeiro
        </Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surface },
  hero: {
    flex: 1,
    backgroundColor: colors.navy,
    paddingHorizontal: 28,
    paddingTop: 34,
    paddingBottom: 24,
    justifyContent: 'space-between',
    overflow: 'hidden',
  },
  title: {
    color: '#fff',
    fontSize: 36,
    lineHeight: 41,
    fontWeight: '800',
    letterSpacing: -1.2,
    maxWidth: 320,
  },
  subtitle: { color: '#C8D7E8', fontSize: 16, lineHeight: 24, marginTop: 14, maxWidth: 330 },
  illustration: { height: 170, alignItems: 'center', justifyContent: 'center' },
  pin: {
    position: 'absolute',
    top: 6,
    right: 42,
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: colors.orange,
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 3,
  },
  pinText: { color: '#fff', fontWeight: '900', fontSize: 27 },
  card: {
    width: 245,
    height: 125,
    borderRadius: 20,
    backgroundColor: '#fff',
    padding: 12,
    transform: [{ rotate: '-3deg' }],
  },
  cardBack: { position: 'absolute', backgroundColor: '#30557C', transform: [{ rotate: '7deg' }], left: 77 },
  cardImage: { height: 66, borderRadius: 12, backgroundColor: '#DDE8F2' },
  cardLine: { height: 9, width: '75%', borderRadius: 5, backgroundColor: colors.navy, marginTop: 10 },
  short: { width: '45%', backgroundColor: colors.orange, marginTop: 7 },
  actions: { padding: 24, gap: 12 },
  explore: { color: colors.navy, fontWeight: '700', textAlign: 'center', paddingVertical: 8 },
});
