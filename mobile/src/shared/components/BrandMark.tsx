import { Image, StyleSheet, Text, View } from 'react-native';
import { colors } from '../theme';

type Props = { compact?: boolean; light?: boolean };

export function BrandMark({ compact = false, light = false }: Props) {
  const navy = light ? '#FFFFFF' : colors.navy;
  return (
    <View style={styles.row} accessibilityLabel="StowUp">
      <View style={[styles.mark, light && styles.markLight, compact && styles.markCompact]}>
        <Image
          source={require('../../../assets/branding/stowup-symbol.png')}
          resizeMode="contain"
          style={styles.symbol}
        />
      </View>
      {!compact && (
        <Text style={[styles.name, { color: navy }]}>
          Stow<Text style={styles.up}>Up</Text>
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  mark: { width: 52, height: 52, borderRadius: 15, alignItems: 'center', justifyContent: 'center' },
  markLight: { backgroundColor: '#FFFFFF' },
  markCompact: { width: 38, height: 38, borderRadius: 11 },
  symbol: { width: '88%', height: '88%' },
  name: { fontSize: 34, fontWeight: '800', letterSpacing: -1.2 },
  up: { color: colors.orange },
});
