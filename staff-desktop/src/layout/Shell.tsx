import { NavLink, Outlet } from 'react-router-dom';
import { ROLE_LABELS, useAuth } from '../state/AuthContext';
import { DEPARTMENTS } from './departments';

export function Shell() {
  const { session, permissions, logout } = useAuth();
  if (!session) return null;

  const visibleDepartments = DEPARTMENTS.filter((department) => department.visible(permissions));

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-mark">SU</span>
          <div>
            <strong>StowUp HQ</strong>
            <small>Central Interna</small>
          </div>
        </div>
        <nav className="sidebar-nav">
          {visibleDepartments.map((department) => (
            <div key={department.id} className="nav-group">
              <span className="nav-group-title">
                {department.icon} {department.name}
              </span>
              {department.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="identity">
            <strong>{session.user.name}</strong>
            <small>{ROLE_LABELS[session.user.role]}</small>
          </div>
          <button type="button" className="btn btn-ghost" onClick={logout}>
            Sair
          </button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
