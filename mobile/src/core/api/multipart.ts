import { Platform } from 'react-native';

export type MultipartFile = { uri: string; name: string; type: string };

export async function appendMultipartFile(form: FormData, field: string, file: MultipartFile) {
  if (Platform.OS === 'web') {
    const response = await fetch(file.uri);
    if (!response.ok) throw new Error('Não foi possível ler o arquivo selecionado. Selecione-o novamente.');
    form.append(field, await response.blob(), file.name);
    return;
  }
  form.append(field, { uri: file.uri, name: file.name, type: file.type } as any);
}
