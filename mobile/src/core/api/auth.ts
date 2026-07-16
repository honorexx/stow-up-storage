import { API_ORIGIN, API_URL } from './config';
export { API_ORIGIN } from './config';

export type User = {
  id: number;
  name: string;
  email: string;
  phone?: string | null;
  cpfMasked?: string | null;
  postalCode?: string | null;
  addressLine?: string | null;
  addressNumber?: string | null;
  addressComplement?: string | null;
  district?: string | null;
  city?: string | null;
  state?: string | null;
  photoUrl?: string | null;
  role: 'CUSTOMER' | 'OWNER' | 'SUPPORT' | 'MODERATOR' | 'ADMIN';
};
export type AuthResponse = { token: string; user: User };
export type RegistrationInput = {
  name: string;
  email: string;
  password: string;
  phone: string;
  cpf: string;
  postalCode: string;
  addressLine: string;
  addressNumber: string;
  addressComplement: string;
  district: string;
  city: string;
  state: string;
  acceptedTerms: boolean;
  termsVersion: string;
};
export type ProfileInput = Omit<
  RegistrationInput,
  'email' | 'password' | 'cpf' | 'acceptedTerms' | 'termsVersion'
>;
type AuthInput = { email: string; password: string } | RegistrationInput;
export async function authenticate(mode: 'login' | 'register', input: AuthInput): Promise<AuthResponse> {
  const response = await fetch(`${API_URL}/auth/${mode}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message ?? 'Não foi possível continuar. Tente novamente.');
  return body as AuthResponse;
}

export type PasswordRecoveryChannel = 'EMAIL' | 'SMS';
export type PasswordRecoveryRequestResult = {
  requestId: string;
  maskedDestination: string;
  resendAfterSeconds: number;
  expiresInSeconds: number;
};

async function passwordRecoveryRequest<T>(path: string, input: object): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(`${API_URL}/auth/password-recovery/${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
      signal: controller.signal,
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.message ?? 'Não foi possível recuperar a senha.');
    return body as T;
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError')
      throw new Error('O servidor demorou para responder. Tente novamente.');
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

export function requestPasswordRecovery(identifier: string, channel: PasswordRecoveryChannel) {
  return passwordRecoveryRequest<PasswordRecoveryRequestResult>('request', { identifier, channel });
}

export function verifyPasswordRecovery(requestId: string, code: string) {
  return passwordRecoveryRequest<{ resetToken: string }>('verify', { requestId, code });
}

export function resetPassword(requestId: string, resetToken: string, newPassword: string) {
  return passwordRecoveryRequest<void>('reset', { requestId, resetToken, newPassword });
}

export async function loadProfile(token: string): Promise<User> {
  const response = await fetch(`${API_URL}/profile`, { headers: { Authorization: `Bearer ${token}` } });
  if (!response.ok) throw new Error('Sessão expirada. Entre novamente.');
  return response.json();
}

export async function saveProfile(token: string, input: ProfileInput): Promise<User> {
  const response = await fetch(`${API_URL}/profile`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(input),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message ?? 'Não foi possível salvar o perfil.');
  return body as User;
}

export async function deleteAccount(token: string, password: string): Promise<void> {
  const response = await fetch(`${API_URL}/profile`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ password }),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message ?? 'Não foi possível excluir a conta.');
  }
}

export type UploadImage = { uri: string; mimeType?: string | null; fileName?: string | null };
export async function uploadAvatar(token: string, image: UploadImage): Promise<User> {
  const form = new FormData();
  const extension =
    image.fileName?.split('.').pop()?.toLowerCase() ||
    image.uri.split('.').pop()?.split('?')[0].toLowerCase() ||
    'jpg';
  const type =
    image.mimeType ||
    (extension === 'png'
      ? 'image/png'
      : extension === 'webp'
        ? 'image/webp'
        : extension === 'heic' || extension === 'heif'
          ? 'image/heic'
          : 'image/jpeg');
  form.append('file', { uri: image.uri, name: image.fileName || `profile.${extension}`, type } as any);
  const response = await fetch(`${API_URL}/profile/avatar`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message ?? 'Não foi possível enviar a foto.');
  return body as User;
}
