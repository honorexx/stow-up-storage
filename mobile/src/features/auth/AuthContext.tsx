import * as SecureStore from '../../core/storage/secureStorage';
import { createContext, PropsWithChildren, useContext, useEffect, useMemo, useState } from 'react';
import {
  authenticate,
  deleteAccount as requestAccountDeletion,
  loadProfile,
  saveProfile,
  uploadAvatar,
  User,
  RegistrationInput,
  ProfileInput,
  UploadImage,
} from '../../core/api/auth';

const TOKEN_KEY = 'stowup.accessToken';

type AuthContextValue = {
  token: string | null;
  user: User | null;
  restoring: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (input: RegistrationInput) => Promise<void>;
  signOut: () => Promise<void>;
  deleteAccount: (password: string) => Promise<void>;
  updateProfile: (input: ProfileInput) => Promise<void>;
  updateAvatar: (image: UploadImage) => Promise<void>;
  refreshProfile: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [restoring, setRestoring] = useState(true);

  useEffect(() => {
    SecureStore.getItemAsync(TOKEN_KEY)
      .then(async (stored) => {
        if (!stored) return;
        try {
          setUser(await loadProfile(stored));
          setToken(stored);
        } catch {
          await SecureStore.deleteItemAsync(TOKEN_KEY);
        }
      })
      .finally(() => setRestoring(false));
  }, []);

  async function accept(result: Awaited<ReturnType<typeof authenticate>>) {
    await SecureStore.setItemAsync(TOKEN_KEY, result.token);
    setToken(result.token);
    setUser(result.user);
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      restoring,
      signIn: async (email, password) => accept(await authenticate('login', { email, password })),
      signUp: async (input) => accept(await authenticate('register', input)),
      signOut: async () => {
        await SecureStore.deleteItemAsync(TOKEN_KEY);
        setToken(null);
        setUser(null);
      },
      deleteAccount: async (password) => {
        if (!token) throw new Error('Entre na sua conta para continuar.');
        await requestAccountDeletion(token, password);
        await SecureStore.deleteItemAsync(TOKEN_KEY);
        setToken(null);
        setUser(null);
      },
      updateProfile: async (input) => {
        if (!token) throw new Error('Entre na sua conta para editar o perfil.');
        setUser(await saveProfile(token, input));
      },
      updateAvatar: async (image) => {
        if (!token) throw new Error('Entre na sua conta para alterar a foto.');
        setUser(await uploadAvatar(token, image));
      },
      refreshProfile: async () => {
        if (!token) return;
        setUser(await loadProfile(token));
      },
    }),
    [token, user, restoring],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth precisa estar dentro de AuthProvider');
  return value;
}
