import { API_URL } from './config';

export async function authorized<T>(path: string, token: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);

  try {
    const response = await fetch(`${API_URL}${path}`, {
      ...init,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        ...(init?.headers ?? {}),
      },
    });
    const body = await response.json().catch(() => null);
    if (!response.ok) {
      const details = body?.details
        ? Object.entries(body.details)
            .map(([field, message]) => `${field}: ${message}`)
            .join(' · ')
        : '';
      throw new Error(details || body?.message || 'Não foi possível carregar os dados.');
    }
    return body as T;
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      throw new Error('O servidor demorou para responder. Verifique se o Spring Boot está ligado.');
    }
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}
