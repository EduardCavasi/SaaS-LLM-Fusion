'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { verificationApi } from '@/lib/api';
import { VerificationStats } from '@/types';

export default function VerificationBanner() {
  const [stats, setStats] = useState<VerificationStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const data = await verificationApi.getStats();
        setStats(data);
      } catch (error) {
        console.error('Failed to fetch verification stats:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
    const interval = setInterval(fetchStats, 5000); // Poll every 5 seconds
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="bg-[var(--secondary)] border-b border-[var(--border)] px-6 py-3">
        <div className="flex items-center gap-2 text-sm text-[var(--muted-foreground)]">
          <span className="animate-pulse-slow">●</span>
          Loading verification status...
        </div>
      </div>
    );
  }

  const hasViolations = stats && stats.totalViolations > 0;
  const hasCritical = stats && stats.criticalViolations > 0;

  return (
    <div className={`border-b px-6 py-3 ${
      hasCritical 
        ? 'bg-red-900/20 border-red-800/50' 
        : hasViolations 
          ? 'bg-amber-900/20 border-amber-800/50'
          : 'bg-[var(--secondary)] border-[var(--border)]'
    }`}>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-6">
          {/* Z3 Status */}
          <div className={`z3-indicator ${stats?.z3SolverInitialized ? '' : 'offline'}`}>
            <span>{stats?.z3SolverInitialized ? '✓' : '✗'}</span>
            <span>Z3 Solver {stats?.z3SolverInitialized ? 'Online' : 'Offline'}</span>
          </div>

          {/* Events Counter */}
          <div className="text-sm text-[var(--muted-foreground)]">
            <span className="font-medium text-[var(--foreground)]">{stats?.totalEvents || 0}</span> events tracked
          </div>

          {/* Violations Summary */}
          {hasViolations && (
            <Link href="/verification" className="flex items-center gap-2 text-sm">
              {hasCritical ? (
                <span className="badge severity-critical">
                  ⚠ {stats.criticalViolations} Critical
                </span>
              ) : null}
              {stats.errorViolations > 0 && (
                <span className="badge severity-error">
                  {stats.errorViolations} Errors
                </span>
              )}
              {stats.warningViolations > 0 && (
                <span className="badge severity-warning">
                  {stats.warningViolations} Warnings
                </span>
              )}
            </Link>
          )}

          {!hasViolations && (
            <span className="text-sm text-[var(--success)]">
              ✓ No violations detected
            </span>
          )}
        </div>

        <Link 
          href="/verification" 
          className="text-sm text-[var(--primary)] hover:underline"
        >
          View Details →
        </Link>
      </div>
    </div>
  );
}

