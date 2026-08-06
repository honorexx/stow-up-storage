import { Platform } from 'react-native';

export async function downloadAuthenticatedPdf(url: string, token: string, fileName: string) {
  if (Platform.OS === 'web') {
    const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
    if (!response.ok) throw new Error('Não foi possível gerar o PDF. Tente novamente.');
    const objectUrl = URL.createObjectURL(await response.blob());
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = fileName;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
    return;
  }

  const FileSystem = await import('expo-file-system/legacy');
  const Sharing = await import('expo-sharing');
  const destination = `${FileSystem.cacheDirectory}${fileName}`;
  await FileSystem.deleteAsync(destination, { idempotent: true });
  const result = await FileSystem.downloadAsync(url, destination, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (result.status < 200 || result.status >= 300) {
    await FileSystem.deleteAsync(result.uri, { idempotent: true });
    throw new Error('Não foi possível gerar o PDF. Tente novamente.');
  }
  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(result.uri, { mimeType: 'application/pdf', dialogTitle: 'Salvar documento' });
  }
}
