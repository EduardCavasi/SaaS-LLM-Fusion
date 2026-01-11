'use client';

import { useEffect, useState } from 'react';
import { verificationApi } from '@/lib/api';
import { VerificationStats, PropertyViolation } from '@/types';

export default function VerificationPage() {
  const [stats, setStats] = useState<VerificationStats | null>(null);
  const [violations, setViolations] = useState<PropertyViolation[]>([]);
  const [loading, setLoading] = useState(true);
  const [checking, setChecking] = useState(false);

  const fetchData = async () => {
    try {
      const [statsData, violationsData] = await Promise.all([
        verificationApi.getStats(),
        verificationApi.getViolations(),
      ]);
      setStats(statsData);
      setViolations(violationsData);
    } catch (error) {
      console.error('Failed to fetch verification data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleCheckPending = async () => {
    setChecking(true);
    try {
      const newViolations = await verificationApi.checkPending();
      if (newViolations.length > 0) {
        setViolations([...newViolations, ...violations]);
      }
      fetchData();
    } catch (error) {
      console.error('Failed to check pending meetings:', error);
    } finally {
      setChecking(false);
    }
  };

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return '🔴';
      case 'ERROR': return '🟠';
      case 'WARNING': return '🟡';
      default: return '⚪';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[var(--muted-foreground)]">Loading verification data...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Runtime Verification</h1>
          <p className="text-[var(--muted-foreground)]">
            Monitor LTL properties and constraint violations
          </p>
        </div>
        <button
          onClick={handleCheckPending}
          disabled={checking}
          className="btn btn-primary"
        >
          {checking ? 'Checking...' : '🔍 Check Pending Meetings'}
        </button>
      </div>

      {/* LTL Properties Explanation */}
      <div className="card">
        <h2 className="text-lg font-semibold mb-4">📜 Monitored LTL Properties</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 bg-[var(--secondary)] rounded-lg">
            <div className="font-mono text-sm text-[var(--primary)] mb-2">
              G(create(id) → F(confirm(id) ∨ reject(id)))
            </div>
            <div className="text-sm text-[var(--muted-foreground)]">
              <strong>Property 1:</strong> Every created meeting must eventually be confirmed or rejected.
            </div>
          </div>
          <div className="p-4 bg-[var(--secondary)] rounded-lg">
            <div className="font-mono text-sm text-[var(--primary)] mb-2">
              G(delete(id) → previouslyCreated(id))
            </div>
            <div className="text-sm text-[var(--muted-foreground)]">
              <strong>Property 2:</strong> Cannot delete a meeting that was never created.
            </div>
          </div>
          <div className="p-4 bg-[var(--secondary)] rounded-lg">
            <div className="font-mono text-sm text-[var(--primary)] mb-2">
              G ¬overlaps(meetingA, meetingB)
            </div>
            <div className="text-sm text-[var(--muted-foreground)]">
              <strong>Property 3:</strong> No two meetings can overlap in the same room.
            </div>
          </div>
          <div className="p-4 bg-[var(--secondary)] rounded-lg">
            <div className="font-mono text-sm text-[var(--primary)] mb-2">
              G(assign(room, attendees) → attendees ≤ capacity)
            </div>
            <div className="text-sm text-[var(--muted-foreground)]">
              <strong>Property 4:</strong> Room capacity must never be exceeded.
            </div>
          </div>
        </div>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className={`stat-card ${stats?.z3SolverInitialized ? 'border-green-800/30' : 'border-red-800/30'}`}>
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Z3 Solver</div>
          <div className={`text-2xl font-bold ${stats?.z3SolverInitialized ? 'text-green-400' : 'text-red-400'}`}>
            {stats?.z3SolverInitialized ? 'Online' : 'Offline'}
          </div>
          <div className="text-xs text-[var(--muted-foreground)] mt-2">
            SMT constraint solving
          </div>
        </div>

        <div className="stat-card">
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Total Events</div>
          <div className="stat-value">{stats?.totalEvents || 0}</div>
          <div className="text-xs text-[var(--muted-foreground)] mt-2">
            Operations monitored
          </div>
        </div>

        <div className="stat-card">
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Tracked Meetings</div>
          <div className="stat-value">{stats?.trackedMeetings || 0}</div>
          <div className="text-xs text-[var(--muted-foreground)] mt-2">
            {stats?.pendingMeetings || 0} pending confirmation
          </div>
        </div>

        <div className={`stat-card ${(stats?.totalViolations || 0) > 0 ? 'border-red-800/30' : 'border-green-800/30'}`}>
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Total Violations</div>
          <div className={`text-2xl font-bold ${(stats?.totalViolations || 0) > 0 ? 'text-red-400' : 'text-green-400'}`}>
            {stats?.totalViolations || 0}
          </div>
          <div className="text-xs text-[var(--muted-foreground)] mt-2">
            Property violations
          </div>
        </div>
      </div>

      {/* Violations Breakdown */}
      <div className="grid grid-cols-3 gap-4">
        <div className="card text-center">
          <div className="text-4xl mb-2">🔴</div>
          <div className="text-3xl font-bold text-[var(--critical)]">
            {stats?.criticalViolations || 0}
          </div>
          <div className="text-sm text-[var(--muted-foreground)]">Critical</div>
          <div className="text-xs text-[var(--muted-foreground)] mt-1">
            System invariant violations
          </div>
        </div>
        <div className="card text-center">
          <div className="text-4xl mb-2">🟠</div>
          <div className="text-3xl font-bold text-[var(--destructive)]">
            {stats?.errorViolations || 0}
          </div>
          <div className="text-sm text-[var(--muted-foreground)]">Errors</div>
          <div className="text-xs text-[var(--muted-foreground)] mt-1">
            Constraint violations
          </div>
        </div>
        <div className="card text-center">
          <div className="text-4xl mb-2">🟡</div>
          <div className="text-3xl font-bold text-[var(--warning)]">
            {stats?.warningViolations || 0}
          </div>
          <div className="text-sm text-[var(--muted-foreground)]">Warnings</div>
          <div className="text-xs text-[var(--muted-foreground)] mt-1">
            Potential issues
          </div>
        </div>
      </div>

      {/* Violations List */}
      <div className="card">
        <h2 className="text-lg font-semibold mb-4">⚠️ Violation History</h2>
        {violations.length === 0 ? (
          <div className="text-center py-12">
            <div className="text-6xl mb-4">✓</div>
            <div className="text-xl font-medium text-green-400">All Properties Satisfied</div>
            <p className="text-[var(--muted-foreground)] mt-2">
              No runtime verification violations have been detected.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {violations.map((violation, index) => (
              <div
                key={index}
                className={`violation-card ${violation.severity.toLowerCase()}`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <span className="text-2xl">{getSeverityIcon(violation.severity)}</span>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className={`badge severity-${violation.severity.toLowerCase()}`}>
                          {violation.severity}
                        </span>
                        <span className="font-mono text-sm font-medium">
                          {violation.propertyName}
                        </span>
                      </div>
                      <div className="text-sm mt-1">{violation.description}</div>
                    </div>
                  </div>
                  <div className="text-xs text-[var(--muted-foreground)]">
                    {new Date(violation.detectedAt).toLocaleString()}
                  </div>
                </div>
                <div className="mt-3 p-3 bg-black/20 rounded text-sm font-mono">
                  {violation.details}
                </div>
                {violation.meetingId && (
                  <div className="mt-2 text-xs text-[var(--muted-foreground)]">
                    Meeting ID: {violation.meetingId}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

