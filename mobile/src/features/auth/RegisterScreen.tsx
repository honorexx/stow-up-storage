import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../../app/navigation/types';
import { AuthForm } from './AuthForm';

export function RegisterScreen({ navigation }: NativeStackScreenProps<RootStackParamList, 'Register'>) {
  return (
    <AuthForm
      mode="register"
      onBack={navigation.goBack}
      onSuccess={() => navigation.reset({ index: 0, routes: [{ name: 'Main' }] })}
      onSwitch={() => navigation.replace('Login')}
      onTerms={() => navigation.navigate('TermsOfUse')}
    />
  );
}
