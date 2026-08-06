import { Navigate, Route, HashRouter, Routes } from 'react-router-dom';
import { Shell } from './layout/Shell';
import { defaultRouteFor } from './layout/departments';
import { Audit } from './pages/diretoria/Audit';
import { Dashboard } from './pages/diretoria/Dashboard';
import { Support } from './pages/atendimento/Support';
import { Moderation } from './pages/operacoes/Moderation';
import { Contracts } from './pages/juridico/Contracts';
import { ListingDocuments } from './pages/juridico/ListingDocuments';
import { Team } from './pages/rh/Team';
import { AppUsers } from './pages/rh/AppUsers';
import { Payments } from './pages/financeiro/Payments';
import { Login } from './pages/Login';
import { AuthProvider, useAuth } from './state/AuthContext';

function NoAccess() {
  return (
    <div className="empty-state">
      <p>Sua conta não tem acesso a nenhum módulo da Central Interna. Fale com a Diretoria.</p>
    </div>
  );
}

function AuthenticatedApp() {
  const { session, permissions } = useAuth();

  if (!session) return <Login />;

  return (
    <Routes>
      <Route path="/" element={<Shell />}>
        <Route index element={<Navigate to={defaultRouteFor(permissions)} replace />} />
        <Route path="diretoria/dashboard" element={<Dashboard />} />
        <Route path="diretoria/auditoria" element={<Audit />} />
        <Route path="atendimento/fila" element={<Support />} />
        <Route path="operacoes/moderacao" element={<Moderation />} />
        <Route path="juridico/contratos" element={<Contracts />} />
        <Route path="juridico/documentos" element={<ListingDocuments />} />
        <Route path="rh/equipe" element={<Team />} />
        <Route path="rh/usuarios" element={<AppUsers />} />
        <Route path="financeiro/pagamentos" element={<Payments />} />
        <Route path="sem-acesso" element={<NoAccess />} />
        <Route path="*" element={<Navigate to={defaultRouteFor(permissions)} replace />} />
      </Route>
    </Routes>
  );
}

export function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <AuthenticatedApp />
      </HashRouter>
    </AuthProvider>
  );
}
