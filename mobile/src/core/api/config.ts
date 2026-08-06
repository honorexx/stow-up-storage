import { Platform } from 'react-native';

const webHost = globalThis.location?.hostname || 'localhost';
const configuredUrl =
  Platform.OS === 'web'
    ? `http://${webHost}:8080/api`
    : (process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api');

export const API_URL = configuredUrl.replace(/\/$/, '');
export const API_ORIGIN = API_URL.replace(/\/api\/?$/, '');
