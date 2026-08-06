import { api } from '../../api/client';
import type { DashboardData } from '../../api/types';
import { EmptyState, ErrorBanner, LoadingBlock, MetricCard, PageHeader } from '../../components/ui';
import { useResource } from '../../hooks/useResource';

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function Dashboard() {
  const { data, loading, error } = useResource((token) => api.get<DashboardData>('/admin/dashboard', token));

  return (
    <div>
      <PageHeader
        title="Visão geral da operação"
        subtitle="Indicadores calculados a partir de usuários, espaços, reservas e contratos da StowUp."
      />
      {loading && <LoadingBlock />}
      {error && <ErrorBanner message={error} />}
      {data && (
        <div className="metric-grid">
          <MetricCard label="Usuários" value={String(data.totalUsers)} detail="Cadastros totais" />
          <MetricCard label="Clientes" value={String(data.customers)} detail="Contas de clientes" />
          <MetricCard
            label="Proprietários"
            value={String(data.owners)}
            detail={`${data.verifiedOwners} verificados`}
          />
          <MetricCard
            label="Espaços ativos"
            value={String(data.activeSpaces)}
            detail={`${data.occupiedSpaces} ocupados`}
          />
          <MetricCard label="Em análise" value={String(data.pendingSpaces)} detail="Aguardando moderação" />
          <MetricCard
            label="Reservas"
            value={String(data.ongoingBookings)}
            detail="Pendentes ou em andamento"
          />
          <MetricCard
            label="Contratos ativos"
            value={String(data.activeContracts)}
            detail="Aprovados e vigentes"
          />
          <MetricCard
            label="Chamados pendentes"
            value={String(data.pendingReports)}
            detail="Aguardando atendimento"
          />
          <MetricCard
            label="Receita da plataforma"
            value={currency.format(data.platformRevenue)}
            detail="Comissão acumulada estimada"
          />
        </div>
      )}
      {!loading && !error && !data && <EmptyState message="Sem dados disponíveis." />}
    </div>
  );
}
