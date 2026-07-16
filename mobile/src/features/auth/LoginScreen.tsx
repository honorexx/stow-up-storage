import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../../app/navigation/types';
import { AuthForm } from './AuthForm';

export function LoginScreen({ navigation }: NativeStackScreenProps<RootStackParamList, 'Login'>) {
  return (
    <AuthForm
      mode="login"
      onBack={navigation.goBack}
      onSuccess={() => navigation.reset({ index: 0, routes: [{ name: 'Main' }] })}
      onSwitch={() => navigation.replace('Register')}
      onForgotPassword={() => navigation.navigate('PasswordRecovery')}
    />
  );
}
