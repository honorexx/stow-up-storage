export async function getItemAsync(key: string) {
  return globalThis.localStorage?.getItem(key) ?? null;
}

export async function setItemAsync(key: string, value: string) {
  globalThis.localStorage?.setItem(key, value);
}

export async function deleteItemAsync(key: string) {
  globalThis.localStorage?.removeItem(key);
}
