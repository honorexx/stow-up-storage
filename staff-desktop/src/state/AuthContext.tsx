import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { api, ApiError } from '../api/client';
import type { AuthResponse, StaffRole, StaffUserInfo } from '../api/types';

const STORAGE_KEY = 'stowup.hq.session';

export interface Session {
  token: string;
  user: StaffUserInfo;
}

export interface Permissions {
  isAdmin: boolean;
  canSupport: boolean;
  canModerate: boolean;
  canReviewDocuments: boolean;
  canReviewContracts: boolean;
}

function permissionsFor(roles: StaffRole[]): Permissions {
  const isAdmin = roles.includes('ADMIN');
  return {
    isAdmin,
    canSupport: isAdmin || roles.includes('SUPPORT'),
    canModerate: isAdmin || roles.includes('MODERATOR') || roles.includes('LISTING_ANALYST'),
    canReviewDocuments: isAdmin || roles.includes('DOCUMENT_ANALYST'),
    canReviewContracts: isAdmin || roles.includes('MODERATOR') || roles.includes('CONTRACT_ANALYST'),
  };
}

function rolesOf(user: StaffUserInfo | undefined): StaffRole[] {
  if (!user) return [];
  return [user.role, ...(user.additionalRoles ?? [])];
}

export const ROLE_LABELS: Record<StaffRole, string> = {
  ADMIN: 'Diretoria · Acesso mestre',
  SUPPORT: 'Atendimento',
  MODERATOR: 'Operações · Moderação',
  LISTING_ANALYST: 'Operações · Análise de anúncios',
  DOCUMENT_ANALYST: 'Jurídico · Documentação',
  CONTRACT_ANALYST: 'Jurídico · Contratos',
};

interface AuthContextValue {
  session: Session | null;
  permissions: Permissions;
  loading: boolean;
  error: string | null;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => readStoredSession());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const login = useCallback(async (identifier: string, password: string) => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.post<AuthResponse>('/auth/staff-login', {
        email: identifier.trim(),
        password,
      });
      const next: Session = { token: response.token, user: response.user };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      setSession(next);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível entrar na central.');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setSession(null);
  }, []);

  const permissions = useMemo(() => permissionsFor(rolesOf(session?.user)), [session?.user]);

  const value = useMemo<AuthContextValue>(
    () => ({ session, permissions, loading, error, login, logout }),
    [session, permissions, loading, error, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth deve ser usado dentro de AuthProvider.');
  return context;
}
