import { digitsOnly } from '../../shared/formatters';

export type PostalAddress = { addressLine: string; district: string; city: string; state: string };

export async function lookupBrazilianPostalCode(value: string): Promise<PostalAddress> {
  const cep = digitsOnly(value);
  if (cep.length !== 8) throw new Error('O CEP precisa ter 8 números.');
  const response = await fetch(`https://viacep.com.br/ws/${cep}/json/`);
  const body = await response.json().catch(() => null);
  if (!response.ok || !body || body.erro) throw new Error('CEP não encontrado. Confira os números.');
  return {
    addressLine: body.logradouro ?? '',
    district: body.bairro ?? '',
    city: body.localidade ?? '',
    state: body.uf ?? '',
  };
}
