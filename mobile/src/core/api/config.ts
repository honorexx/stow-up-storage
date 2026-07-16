export const API_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '');
export const API_ORIGIN = API_URL.replace(/\/api\/?$/, '');
