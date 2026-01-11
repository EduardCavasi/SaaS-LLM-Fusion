'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { verificationApi, meetingApi, roomApi, participantApi } from '@/lib/api';
import { VerificationStats, Meeting, PropertyViolation } from '@/types';

export default function Dashboard() {
  const [stats, setStats] = useState<VerificationStats | null>(null);
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [violations, setViolations] = useState<PropertyViolation[]>([]);
  const [roomCount, setRoomCount] = useState(0);
  const [participantCount, setParticipantCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [statsData, meetingsData, violationsData, rooms, participants] = await Promise.all([
          verificationApi.getStats(),
          meetingApi.getAll(),
          verificationApi.getViolations(),
          roomApi.getAll(),
          participantApi.getAll(),
        ]);
        setStats(statsData);
        setMeetings(meetingsData);
        setViolations(violationsData);
        setRoomCount(rooms.length);
        setParticipantCount(participants.length);
      } catch (error) {
        console.error('Failed to fetch dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const pendingMeetings = meetings.filter(m => m.status === 'PENDING');
  const confirmedMeetings = meetings.filter(m => m.status === 'CONFIRMED');
  const recentViolations = violations.slice(0, 5);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[var(--muted-foreground)]">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Dashboard</h1>
          <p className="text-[var(--muted-foreground)]">
            Overview of your verified meeting scheduler
          </p>
        </div>
        <Link href="/meetings" className="btn btn-primary">
          + New Meeting
        </Link>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="stat-card">
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Total Meetings</div>
          <div className="stat-value">{meetings.length}</div>
          <div className="text-xs text-[var(--muted-foreground)] mt-2">
            {pendingMeetings.length} pending, {confirmedMeetings.length} confirmed
          </div>
        </div>

        <div className="stat-card">
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Rooms</div>
          <div className="stat-value">{roomCount}</div>
          <div className="text-xs text-[var(--muted-foreground)] mt-2">
            Available for booking
          </div>
        </div>

        <div className="stat-card">
          <div className="text-[var(--muted-foreground)] text-sm mb-1">Participants</div>
          <div className="stat-value">{participantCount}</div>
          <div className="text-xs text-[var(--muted-foreground)] mt-2">
            Registered users
          </div>
        </div>

        <div className="stat-card">
          <div className="text-[var(--muted-foreground)] text-sm mb-1">RV Events</div>
          <div className="stat-value">{stats?.totalEvents || 0}</div>
          <div className="text-xs text-[var(--muted-foreground)] mt-2">
            Monitored operations
          </div>
        </div>
      </div>

      {/* Verification Status Card */}
      <div className="card">
        <h2 className="text-lg font-semibold mb-4">🔍 Verification Status</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Z3 Solver Status */}
          <div className="space-y-3">
            <h3 className="text-sm font-medium text-[var(--muted-foreground)]">Z3 SMT Solver</h3>
            <div className={`flex items-center gap-3 p-3 rounded-lg ${
              stats?.z3SolverInitialized 
                ? 'bg-green-900/20 border border-green-800/30' 
                : 'bg-red-900/20 border border-red-800/30'
            }`}>
              <div className={`w-3 h-3 rounded-full ${
                stats?.z3SolverInitialized ? 'bg-green-500' : 'bg-red-500'
              }`} />
              <div>
                <div className={stats?.z3SolverInitialized ? 'text-green-400' : 'text-red-400'}>
                  {stats?.z3SolverInitialized ? 'Operational' : 'Offline'}
                </div>
                <div className="text-xs text-[var(--muted-foreground)]">
                  Static constraint validation
                </div>
              </div>
            </div>
            <div className="text-xs text-[var(--muted-foreground)]">
              Checks: No room overlaps, participant conflicts, capacity limits
            </div>
          </div>

          {/* Runtime Verification Status */}
          <div className="space-y-3">
            <h3 className="text-sm font-medium text-[var(--muted-foreground)]">Runtime Verification</h3>
            <div className="grid grid-cols-3 gap-2">
              <div className="p-3 bg-[var(--secondary)] rounded-lg text-center">
                <div className="text-2xl font-bold text-[var(--critical)]">
                  {stats?.criticalViolations || 0}
                </div>
                <div className="text-xs text-[var(--muted-foreground)]">Critical</div>
              </div>
              <div className="p-3 bg-[var(--secondary)] rounded-lg text-center">
                <div className="text-2xl font-bold text-[var(--destructive)]">
                  {stats?.errorViolations || 0}
                </div>
                <div className="text-xs text-[var(--muted-foreground)]">Errors</div>
              </div>
              <div className="p-3 bg-[var(--secondary)] rounded-lg text-center">
                <div className="text-2xl font-bold text-[var(--warning)]">
                  {stats?.warningViolations || 0}
                </div>
                <div className="text-xs text-[var(--muted-foreground)]">Warnings</div>
              </div>
            </div>
            <div className="text-xs text-[var(--muted-foreground)]">
              Monitors: G(create→F confirm∨reject), G(delete→created), G¬overlap
            </div>
          </div>
        </div>
      </div>

      {/* Two Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Violations */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">⚠️ Recent Violations</h2>
            <Link href="/verification" className="text-sm text-[var(--primary)] hover:underline">
              View all
            </Link>
          </div>
          {recentViolations.length === 0 ? (
            <div className="text-center py-8 text-[var(--muted-foreground)]">
              <div className="text-4xl mb-2">✓</div>
              <p>No violations detected</p>
              <p className="text-xs mt-1">All LTL properties are satisfied</p>
            </div>
          ) : (
            <div className="space-y-2">
              {recentViolations.map((v, i) => (
                <div key={i} className={`violation-card ${v.severity.toLowerCase()}`}>
                  <div className="flex items-start justify-between">
                    <div>
                      <span className={`badge severity-${v.severity.toLowerCase()} mr-2`}>
                        {v.severity}
                      </span>
                      <span className="font-medium">{v.propertyName}</span>
                    </div>
                  </div>
                  <p className="text-sm text-[var(--muted-foreground)] mt-1">{v.description}</p>
                  <p className="text-xs text-[var(--muted-foreground)] mt-1">{v.details}</p>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Pending Meetings */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">📋 Pending Meetings</h2>
            <Link href="/meetings" className="text-sm text-[var(--primary)] hover:underline">
              View all
            </Link>
          </div>
          {pendingMeetings.length === 0 ? (
            <div className="text-center py-8 text-[var(--muted-foreground)]">
              <div className="text-4xl mb-2">📅</div>
              <p>No pending meetings</p>
              <p className="text-xs mt-1">All meetings have been processed</p>
            </div>
          ) : (
            <div className="space-y-2">
              {pendingMeetings.slice(0, 5).map((meeting) => (
                <div key={meeting.id} className="p-3 bg-[var(--secondary)] rounded-lg">
                  <div className="flex items-center justify-between">
                    <div className="font-medium">{meeting.title}</div>
                    <span className="badge badge-pending">Pending</span>
                  </div>
                  <div className="text-sm text-[var(--muted-foreground)] mt-1">
                    {meeting.roomName} • {new Date(meeting.startTime).toLocaleString()}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
