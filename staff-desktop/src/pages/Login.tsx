import { useState, type FormEvent } from 'react';
import { useAuth } from '../state/AuthContext';

export function Login() {
  const { login, loading, error } = useAuth();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      await login(identifier, password);
    } catch {
      // erro já exposto pelo contexto
    }
  }

  return (
    <div className="login-screen">
      <form className="login-card" onSubmit={handleSubmit}>
        <span className="eyebrow">AMBIENTE RESTRITO</span>
        <h1>StowUp HQ</h1>
        <p className="muted">Central Interna — acesso exclusivo da equipe StowUp.</p>
        <label>
          Código SU ou e-mail corporativo
          <input
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            placeholder="SU123456 ou nome@stowup.com.br"
            autoComplete="username"
            required
          />
        </label>
        <label>
          Senha
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error && <p className="error-text">{error}</p>}
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Entrando…' : 'Entrar na central'}
        </button>
        <small className="muted">Clientes e proprietários usam o aplicativo StowUp, não esta central.</small>
      </form>
    </div>
  );
}
