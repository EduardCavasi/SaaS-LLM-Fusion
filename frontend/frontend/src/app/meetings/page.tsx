'use client';

import { useEffect, useState } from 'react';
import { meetingApi, roomApi, participantApi } from '@/lib/api';
import { Meeting, Room, Participant, SchedulingResult, CreateMeetingRequest } from '@/types';

export default function MeetingsPage() {
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [schedulingResult, setSchedulingResult] = useState<SchedulingResult | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [formData, setFormData] = useState<CreateMeetingRequest>({
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    roomId: 0,
    participantIds: [],
  });

  const fetchData = async () => {
    try {
      const [meetingsData, roomsData, participantsData] = await Promise.all([
        meetingApi.getAll(),
        roomApi.getAvailable(),
        participantApi.getAll(),
      ]);
      setMeetings(meetingsData);
      setRooms(roomsData);
      setParticipants(participantsData);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const resetForm = () => {
    setFormData({
      title: '',
      description: '',
      startTime: '',
      endTime: '',
      roomId: 0,
      participantIds: [],
    });
    setSchedulingResult(null);
    setApiError(null);
  };

  const handleCreateMeeting = async (e: React.FormEvent) => {
    e.preventDefault();
    setSchedulingResult(null);
    setApiError(null);
    setSubmitting(true);

    // Client-side validation
    if (formData.roomId === 0) {
      setApiError('Please select a room');
      setSubmitting(false);
      return;
    }
    if (formData.participantIds.length === 0) {
      setApiError('Please select at least one participant');
      setSubmitting(false);
      return;
    }

    try {
      // Convert datetime-local to ISO format preserving the local time
      // datetime-local gives us "2026-02-12T11:30", we need to keep this time as-is
      const formatDateTime = (dateTimeLocal: string) => {
        // If already has timezone info, return as is
        if (dateTimeLocal.includes('Z') || dateTimeLocal.includes('+')) {
          return dateTimeLocal;
        }
        // Append seconds and timezone to make it a proper ISO string
        // This treats the input as the intended local time
        return `${dateTimeLocal}:00`;
      };

      const result = await meetingApi.create({
        ...formData,
        startTime: formatDateTime(formData.startTime),
        endTime: formatDateTime(formData.endTime),
      });
      setSchedulingResult(result);

      if (result.success) {
        fetchData();
        setTimeout(() => {
          setShowModal(false);
          resetForm();
        }, 2500);
      }
    } catch (error) {
      console.error('Failed to create meeting:', error);
      setApiError(error instanceof Error ? error.message : 'Failed to create meeting. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirm = async (id: number) => {
    try {
      await meetingApi.confirm(id);
      fetchData();
    } catch (error) {
      console.error('Failed to confirm meeting:', error);
    }
  };

  const handleReject = async (id: number) => {
    try {
      await meetingApi.reject(id);
      fetchData();
    } catch (error) {
      console.error('Failed to reject meeting:', error);
    }
  };

  const handleCancel = async (id: number) => {
    try {
      await meetingApi.cancel(id);
      fetchData();
    } catch (error) {
      console.error('Failed to cancel meeting:', error);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this meeting?')) return;
    
    const previousMeetings = [...meetings];
    setMeetings(meetings.filter(meeting => meeting.id !== id));
    
    try {
      await meetingApi.delete(id);
      await fetchData();
    } catch (error) {
      console.error('Failed to delete meeting:', error);
      setMeetings(previousMeetings);
    }
  };

  const getStatusBadge = (status: string) => {
    const classes: Record<string, string> = {
      PENDING: 'badge-pending',
      CONFIRMED: 'badge-confirmed',
      REJECTED: 'badge-rejected',
      CANCELLED: 'badge-cancelled',
      COMPLETED: 'badge-completed',
    };
    return <span className={`badge ${classes[status]}`}>{status}</span>;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[var(--muted-foreground)]">Loading meetings...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Meetings</h1>
          <p className="text-[var(--muted-foreground)]">
            Schedule and manage meetings with Z3 verification
          </p>
        </div>
        <button onClick={() => { resetForm(); setShowModal(true); }} className="btn btn-primary">
          + New Meeting
        </button>
      </div>

      {/* Meetings Table */}
      <div className="card overflow-hidden p-0">
        <table className="table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Room</th>
              <th>Time</th>
              <th>Participants</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {meetings.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center py-8 text-[var(--muted-foreground)]">
                  No meetings scheduled yet
                </td>
              </tr>
            ) : (
              meetings.map((meeting) => (
                <tr key={meeting.id}>
                  <td>
                    <div className="font-medium">{meeting.title}</div>
                    {meeting.description && (
                      <div className="text-xs text-[var(--muted-foreground)]">
                        {meeting.description}
                      </div>
                    )}
                  </td>
                  <td>{meeting.roomName}</td>
                  <td>
                    <div className="text-sm">
                      {new Date(meeting.startTime).toLocaleDateString()}
                    </div>
                    <div className="text-xs text-[var(--muted-foreground)]">
                      {new Date(meeting.startTime).toLocaleTimeString()} - {new Date(meeting.endTime).toLocaleTimeString()}
                    </div>
                  </td>
                  <td>
                    <div className="flex flex-wrap gap-1">
                      {meeting.participants?.slice(0, 2).map(p => (
                        <span key={p.id} className="text-xs bg-[var(--secondary)] px-2 py-1 rounded">
                          {p.name}
                        </span>
                      ))}
                      {(meeting.participants?.length || 0) > 2 && (
                        <span className="text-xs text-[var(--muted-foreground)]">
                          +{(meeting.participants?.length || 0) - 2} more
                        </span>
                      )}
                    </div>
                  </td>
                  <td>{getStatusBadge(meeting.status)}</td>
                  <td>
                    <div className="flex gap-2">
                      {meeting.status === 'PENDING' && (
                        <>
                          <button
                            onClick={() => handleConfirm(meeting.id)}
                            className="btn btn-success text-xs py-1 px-2"
                          >
                            Confirm
                          </button>
                          <button
                            onClick={() => handleReject(meeting.id)}
                            className="btn btn-danger text-xs py-1 px-2"
                          >
                            Reject
                          </button>
                        </>
                      )}
                      {meeting.status === 'CONFIRMED' && (
                        <button
                          onClick={() => handleCancel(meeting.id)}
                          className="btn btn-secondary text-xs py-1 px-2"
                        >
                          Cancel
                        </button>
                      )}
                      <button
                        onClick={() => handleDelete(meeting.id)}
                        className="btn btn-danger text-xs py-1 px-2"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Create Meeting Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content max-w-2xl" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-xl font-bold mb-4">Schedule New Meeting</h2>
            
            {/* API Error Display */}
            {apiError && (
              <div className="mb-4 p-4 rounded-lg bg-red-900/30 border border-red-700/50">
                <div className="flex items-center gap-2">
                  <span className="text-red-400 text-xl">⚠️</span>
                  <span className="font-medium text-red-400">Error</span>
                </div>
                <p className="text-sm text-red-300 mt-1">{apiError}</p>
              </div>
            )}

            {/* Z3 Verification Result - Enhanced Display */}
            {schedulingResult && (
              <div className={`mb-4 rounded-lg border-2 overflow-hidden ${
                schedulingResult.success 
                  ? 'border-green-600/50 bg-green-900/10' 
                  : 'border-red-600/50 bg-red-900/10'
              }`}>
                {/* Header */}
                <div className={`px-4 py-3 ${
                  schedulingResult.success ? 'bg-green-900/30' : 'bg-red-900/30'
                }`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <span className={`text-2xl ${schedulingResult.success ? 'text-green-400' : 'text-red-400'}`}>
                        {schedulingResult.success ? '✓' : '✗'}
                      </span>
                      <div>
                        <div className="font-bold text-lg">
                          Z3 SMT Solver: {schedulingResult.solverStatus}
                        </div>
                        <div className="text-xs text-[var(--muted-foreground)]">
                          Constraint solving completed in {schedulingResult.solvingTimeMs}ms
                        </div>
                      </div>
                    </div>
                    <div className={`px-3 py-1 rounded-full text-sm font-medium ${
                      schedulingResult.success 
                        ? 'bg-green-600/30 text-green-300' 
                        : 'bg-red-600/30 text-red-300'
                    }`}>
                      {schedulingResult.success ? 'SATISFIABLE' : 'UNSATISFIABLE'}
                    </div>
                  </div>
                </div>

                {/* Body */}
                <div className="p-4">
                  <p className="text-sm mb-3">{schedulingResult.explanation}</p>
                  
                  {/* Constraint Violations */}
                  {schedulingResult.constraintViolations && schedulingResult.constraintViolations.length > 0 && (
                    <div className="mt-4 p-3 rounded-lg bg-red-900/20 border border-red-800/30">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="text-red-400">🚫</span>
                        <span className="font-semibold text-red-400">Z3 Constraint Violations</span>
                        <span className="text-xs bg-red-700/50 text-red-200 px-2 py-0.5 rounded-full">
                          {schedulingResult.constraintViolations.length}
                        </span>
                      </div>
                      <ul className="space-y-2">
                        {schedulingResult.constraintViolations.map((violation, i) => (
                          <li key={i} className="flex items-start gap-2 text-sm">
                            <span className="text-red-400 mt-0.5">•</span>
                            <span className="text-red-200">{violation}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* Runtime Warnings */}
                  {schedulingResult.runtimeWarnings && schedulingResult.runtimeWarnings.length > 0 && (
                    <div className="mt-4 p-3 rounded-lg bg-amber-900/20 border border-amber-800/30">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="text-amber-400">⚠️</span>
                        <span className="font-semibold text-amber-400">Runtime Verification Warnings</span>
                        <span className="text-xs bg-amber-700/50 text-amber-200 px-2 py-0.5 rounded-full">
                          {schedulingResult.runtimeWarnings.length}
                        </span>
                      </div>
                      <ul className="space-y-2">
                        {schedulingResult.runtimeWarnings.map((warning, i) => (
                          <li key={i} className="flex items-start gap-2 text-sm">
                            <span className="text-amber-400 mt-0.5">•</span>
                            <span className="text-amber-200">{warning}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* Success message */}
                  {schedulingResult.success && (
                    <div className="mt-4 p-3 rounded-lg bg-green-900/20 border border-green-800/30">
                      <div className="flex items-center gap-2">
                        <span className="text-green-400">✓</span>
                        <span className="text-green-300 text-sm">
                          Meeting scheduled successfully! All constraints satisfied.
                        </span>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            <form onSubmit={handleCreateMeeting} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Title *</label>
                <input
                  type="text"
                  className="input"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  placeholder="Enter meeting title"
                  required
                  disabled={submitting}
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  className="input"
                  rows={2}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Optional description"
                  disabled={submitting}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Start Time *</label>
                  <input
                    type="datetime-local"
                    className="input"
                    value={formData.startTime}
                    onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                    required
                    disabled={submitting}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">End Time *</label>
                  <input
                    type="datetime-local"
                    className="input"
                    value={formData.endTime}
                    onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                    required
                    disabled={submitting}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Room * <span className="text-xs text-[var(--muted-foreground)]">(Z3 checks capacity & conflicts)</span></label>
                <select
                  className="input select"
                  value={formData.roomId}
                  onChange={(e) => setFormData({ ...formData, roomId: Number(e.target.value) })}
                  required
                  disabled={submitting}
                >
                  <option value={0}>Select a room...</option>
                  {rooms.map((room) => (
                    <option key={room.id} value={room.id}>
                      {room.name} (Capacity: {room.capacity})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">
                  Participants * 
                  <span className="text-xs text-[var(--muted-foreground)] ml-2">
                    ({formData.participantIds.length} selected - Z3 checks availability)
                  </span>
                </label>
                <div className="grid grid-cols-2 gap-2 max-h-40 overflow-y-auto p-2 bg-[var(--secondary)] rounded-lg">
                  {participants.map((p) => (
                    <label key={p.id} className="flex items-center gap-2 cursor-pointer hover:bg-[var(--muted)] p-1 rounded">
                      <input
                        type="checkbox"
                        checked={formData.participantIds.includes(p.id)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setFormData({
                              ...formData,
                              participantIds: [...formData.participantIds, p.id],
                            });
                          } else {
                            setFormData({
                              ...formData,
                              participantIds: formData.participantIds.filter((id) => id !== p.id),
                            });
                          }
                        }}
                        className="rounded"
                        disabled={submitting}
                      />
                      <span className="text-sm">{p.name}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Z3 Info Box */}
              <div className="p-3 rounded-lg bg-indigo-900/20 border border-indigo-800/30 text-sm">
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-indigo-400">🔍</span>
                  <span className="font-medium text-indigo-400">Z3 SMT Solver Verification</span>
                </div>
                <p className="text-[var(--muted-foreground)] text-xs">
                  When you submit, Z3 will verify: no room overlaps, participant availability, and room capacity constraints.
                </p>
              </div>

              <div className="flex gap-3 pt-4">
                <button 
                  type="submit" 
                  className="btn btn-primary flex-1"
                  disabled={submitting}
                >
                  {submitting ? (
                    <>
                      <span className="animate-spin">⏳</span> Verifying...
                    </>
                  ) : (
                    <>🔍 Verify & Schedule</>
                  )}
                </button>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="btn btn-secondary"
                  disabled={submitting}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
