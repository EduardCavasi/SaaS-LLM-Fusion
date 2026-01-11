'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

const navItems = [
  { href: '/', label: 'Dashboard', icon: '📊' },
  { href: '/meetings', label: 'Meetings', icon: '📅' },
  { href: '/rooms', label: 'Rooms', icon: '🏢' },
  { href: '/participants', label: 'Participants', icon: '👥' },
  { href: '/verification', label: 'Verification', icon: '🔍' },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 min-h-screen bg-[var(--card)] border-r border-[var(--border)] p-4 flex flex-col">
      <div className="mb-8">
        <h1 className="text-xl font-bold bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent">
          Meeting Scheduler
        </h1>
        <p className="text-xs text-[var(--muted-foreground)] mt-1">
          Verified with Z3 + Runtime Monitoring
        </p>
      </div>

      <nav className="flex-1 space-y-1">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={`nav-link ${pathname === item.href ? 'active' : ''}`}
          >
            <span className="text-lg">{item.icon}</span>
            <span>{item.label}</span>
          </Link>
        ))}
      </nav>

      <div className="mt-auto pt-4 border-t border-[var(--border)]">
        <div className="text-xs text-[var(--muted-foreground)]">
          <p>Formal Methods Project</p>
          <p className="mt-1">Z3 SMT Solver + RV Monitor</p>
        </div>
      </div>
    </aside>
  );
}

