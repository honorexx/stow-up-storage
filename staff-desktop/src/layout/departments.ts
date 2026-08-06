import type { Permissions } from '../state/AuthContext';

export interface NavItem {
  to: string;
  label: string;
}

export interface Department {
  id: string;
  name: string;
  icon: string;
  visible: (permissions: Permissions) => boolean;
  items: NavItem[];
}

export const DEPARTMENTS: Department[] = [
  {
    id: 'diretoria',
    name: 'Diretoria',
    icon: '📊',
    visible: (p) => p.isAdmin,
    items: [
      { to: '/diretoria/dashboard', label: 'Visão geral' },
      { to: '/diretoria/auditoria', label: 'Auditoria' },
    ],
  },
  {
    id: 'atendimento',
    name: 'Atendimento',
    icon: '🎧',
    visible: (p) => p.canSupport,
    items: [{ to: '/atendimento/fila', label: 'Fila de chamados' }],
  },
  {
    id: 'operacoes',
    name: 'Operações',
    icon: '🏢',
    visible: (p) => p.canModerate,
    items: [{ to: '/operacoes/moderacao', label: 'Moderação de anúncios' }],
  },
  {
    id: 'juridico',
    name: 'Jurídico',
    icon: '⚖️',
    visible: (p) => p.canReviewContracts || p.canReviewDocuments,
    items: [
      { to: '/juridico/contratos', label: 'Contratos' },
      { to: '/juridico/documentos', label: 'Documentos de anúncio' },
    ],
  },
  {
    id: 'rh',
    name: 'RH',
    icon: '👥',
    visible: (p) => p.isAdmin,
    items: [
      { to: '/rh/equipe', label: 'Equipe' },
      { to: '/rh/usuarios', label: 'Usuários do app' },
    ],
  },
  {
    id: 'financeiro',
    name: 'Financeiro',
    icon: '💰',
    visible: (p) => p.isAdmin,
    items: [{ to: '/financeiro/pagamentos', label: 'Pagamentos' }],
  },
];

export function defaultRouteFor(permissions: Permissions): string {
  for (const department of DEPARTMENTS) {
    if (department.visible(permissions) && department.items[0]) return department.items[0].to;
  }
  return '/sem-acesso';
}
