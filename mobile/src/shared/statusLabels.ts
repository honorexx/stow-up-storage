const SPACE_LIFECYCLE_LABELS: Record<string, string> = {
  IN_REVIEW: 'Em análise',
  ACTIVE: 'Ativo',
  OCCUPIED: 'Ocupado',
  TEMP_UNAVAILABLE: 'Indisponível',
  SUSPENDED: 'Suspenso',
  WITHDRAWN: 'Excluído',
  REJECTED: 'Reprovado',
};

const MODERATION_LABELS: Record<string, string> = {
  PENDING: 'Pendente',
  IN_REVIEW: 'Em análise',
  APPROVED: 'Aprovado',
  REJECTED: 'Reprovado',
  SUSPENDED: 'Suspenso',
  WITHDRAWN: 'Excluído',
};

export function spaceLifecycleLabel(status?: string | null) {
  if (!status) return 'Não informado';
  return SPACE_LIFECYCLE_LABELS[status] ?? status;
}

export function moderationStatusLabel(status?: string | null) {
  if (!status) return 'Não informado';
  return MODERATION_LABELS[status] ?? status;
}
