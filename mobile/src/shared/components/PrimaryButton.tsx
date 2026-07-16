import { ActivityIndicator, Pressable, StyleSheet, Text } from 'react-native';
import { colors } from '../theme';

type Props = {
  label: string;
  onPress: () => void;
  loading?: boolean;
  secondary?: boolean;
  disabled?: boolean;
};

export function PrimaryButton({
  label,
  onPress,
  loading = false,
  secondary = false,
  disabled = false,
}: Props) {
  return (
    <Pressable
      disabled={loading || disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        secondary && styles.secondary,
        disabled && styles.disabled,
        pressed && styles.pressed,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={secondary ? colors.navy : '#fff'} />
      ) : (
        <Text style={[styles.label, secondary && styles.secondaryLabel]}>{label}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    height: 54,
    borderRadius: 16,
    backgroundColor: colors.orange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondary: { backgroundColor: colors.surface, borderWidth: 1.5, borderColor: colors.navy },
  disabled: { opacity: 0.45 },
  pressed: { opacity: 0.82 },
  label: { color: '#fff', fontWeight: '800', fontSize: 16 },
  secondaryLabel: { color: colors.navy },
});
