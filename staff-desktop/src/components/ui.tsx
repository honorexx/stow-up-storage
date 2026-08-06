import type { ReactNode } from 'react';

export function PageHeader({
  title,
  subtitle,
  actions,
}: {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {actions && <div className="page-header-actions">{actions}</div>}
    </div>
  );
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`card ${className}`}>{children}</div>;
}

export function MetricCard({ label, value, detail }: { label: string; value: string; detail?: string }) {
  return (
    <Card className="metric-card">
      <span className="metric-label">{label}</span>
      <strong className="metric-value">{value}</strong>
      {detail && <span className="muted">{detail}</span>}
    </Card>
  );
}

export function StatusBadge({ status, tone }: { status: string; tone?: 'ok' | 'warn' | 'bad' | 'neutral' }) {
  const resolvedTone =
    tone ??
    (['APPROVED', 'VALID', 'OPEN', 'ACTIVE', 'CLOSED', 'PAID', 'CONFIRMED'].includes(status)
      ? 'ok'
      : ['REJECTED', 'INVALID', 'SUSPENDED', 'FAILED'].includes(status)
        ? 'bad'
        : 'warn');
  return <span className={`badge badge-${resolvedTone}`}>{status}</span>;
}

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="empty-state">
      <p>{message}</p>
    </div>
  );
}

export function ErrorBanner({ message }: { message: string }) {
  return <div className="error-banner">{message}</div>;
}

export function LoadingBlock() {
  return <div className="loading-block">Carregando…</div>;
}

export function PrimaryButton({
  children,
  onClick,
  disabled,
  type = 'button',
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  type?: 'button' | 'submit';
}) {
  return (
    <button type={type} className="btn btn-primary" onClick={onClick} disabled={disabled}>
      {children}
    </button>
  );
}

export function GhostButton({
  children,
  onClick,
  disabled,
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
}) {
  return (
    <button type="button" className="btn btn-ghost" onClick={onClick} disabled={disabled}>
      {children}
    </button>
  );
}

export function DangerButton({
  children,
  onClick,
  disabled,
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
}) {
  return (
    <button type="button" className="btn btn-danger" onClick={onClick} disabled={disabled}>
      {children}
    </button>
  );
}
