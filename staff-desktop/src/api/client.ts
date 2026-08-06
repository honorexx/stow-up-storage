const DEFAULT_API_URL = 'http://localhost:8080/api';

function apiUrl(): string {
  return (import.meta.env.VITE_API_URL as string | undefined) ?? DEFAULT_API_URL;
}

export class ApiError extends Error {}

type Json = Record<string, unknown> | unknown[];

async function parseError(response: Response): Promise<never> {
  const isJson = response.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await response.json().catch(() => null) : null;
  const message =
    body && typeof body === 'object' && 'message' in body && typeof body.message === 'string'
      ? body.message
      : `Não foi possível concluir a operação (${response.status}).`;
  throw new ApiError(message);
}

async function request<T>(path: string, options: RequestInit, token?: string | null): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${apiUrl()}${path}`, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new ApiError(
      `Não foi possível conectar ao backend em ${apiUrl()}. Verifique se o Spring Boot está iniciado.`,
    );
  }
  if (!response.ok) await parseError(response);
  if (response.status === 204) return undefined as T;
  const isJson = response.headers.get('content-type')?.includes('application/json');
  return isJson ? ((await response.json()) as T) : (undefined as T);
}

export async function downloadFile(path: string, token: string | null): Promise<Blob> {
  const response = await fetch(`${apiUrl()}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) await parseError(response);
  return response.blob();
}

export function openBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 4000);
}

export const api = {
  get: <T>(path: string, token?: string | null) => request<T>(path, { method: 'GET' }, token),
  post: <T>(path: string, payload: Json | undefined, token?: string | null) =>
    request<T>(path, { method: 'POST', body: payload ? JSON.stringify(payload) : undefined }, token),
  patch: <T>(path: string, payload: Json, token?: string | null) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(payload) }, token),
  delete: <T>(path: string, token?: string | null) => request<T>(path, { method: 'DELETE' }, token),
};
