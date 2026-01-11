// API Service Layer for the Verified Meeting Scheduler

import {
  Room,
  Participant,
  Meeting,
  CreateMeetingRequest,
  SchedulingResult,
  PropertyViolation,
  VerificationStats,
  MeetingStatus,
} from '@/types';

const API_BASE = 'http://localhost:8081/api';

async function fetchApi<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Request failed' }));
    throw new Error(error.message || `HTTP ${response.status}`);
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    const text = await response.text();
    return text ? JSON.parse(text) : undefined as T;
  }

  return undefined as T;
}

// Special handler for meeting creation that always returns SchedulingResult
async function createMeetingApi(meeting: CreateMeetingRequest): Promise<SchedulingResult> {
  const response = await fetch(`${API_BASE}/meetings`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(meeting),
  });

  const data = await response.json();
  
  // If response is OK, return the scheduling result
  if (response.ok) {
    return data as SchedulingResult;
  }
  
  // If response is not OK but contains a SchedulingResult structure, return it
  if (data && typeof data.success === 'boolean') {
    return data as SchedulingResult;
  }
  
  // Otherwise, construct an error SchedulingResult
  return {
    success: false,
    constraintViolations: [data.message || 'Failed to create meeting'],
    runtimeWarnings: [],
    solverStatus: 'ERROR',
    explanation: data.message || 'An error occurred while processing the request',
    solvingTimeMs: 0,
  };
}

// Special handler for meeting update that always returns SchedulingResult
async function updateMeetingApi(id: number, meeting: Partial<CreateMeetingRequest>): Promise<SchedulingResult> {
  const response = await fetch(`${API_BASE}/meetings/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(meeting),
  });

  const data = await response.json();
  
  // If response is OK, return the scheduling result
  if (response.ok) {
    return data as SchedulingResult;
  }
  
  // If response is not OK but contains a SchedulingResult structure, return it
  if (data && typeof data.success === 'boolean') {
    return data as SchedulingResult;
  }
  
  // Otherwise, construct an error SchedulingResult
  return {
    success: false,
    constraintViolations: [data.message || 'Failed to update meeting'],
    runtimeWarnings: [],
    solverStatus: 'ERROR',
    explanation: data.message || 'An error occurred while processing the request',
    solvingTimeMs: 0,
  };
}

// Room API
export const roomApi = {
  getAll: () => fetchApi<Room[]>('/rooms'),
  getById: (id: number) => fetchApi<Room>(`/rooms/${id}`),
  getAvailable: () => fetchApi<Room[]>('/rooms/available'),
  create: (room: Omit<Room, 'id'>) =>
    fetchApi<Room>('/rooms', {
      method: 'POST',
      body: JSON.stringify(room),
    }),
  update: (id: number, room: Partial<Room>) =>
    fetchApi<Room>(`/rooms/${id}`, {
      method: 'PUT',
      body: JSON.stringify(room),
    }),
  delete: (id: number) =>
    fetchApi<void>(`/rooms/${id}`, { method: 'DELETE' }),
};

// Participant API
export const participantApi = {
  getAll: () => fetchApi<Participant[]>('/participants'),
  getById: (id: number) => fetchApi<Participant>(`/participants/${id}`),
  create: (participant: Omit<Participant, 'id'>) =>
    fetchApi<Participant>('/participants', {
      method: 'POST',
      body: JSON.stringify(participant),
    }),
  update: (id: number, participant: Partial<Participant>) =>
    fetchApi<Participant>(`/participants/${id}`, {
      method: 'PUT',
      body: JSON.stringify(participant),
    }),
  delete: (id: number) =>
    fetchApi<void>(`/participants/${id}`, { method: 'DELETE' }),
};

// Meeting API
export const meetingApi = {
  getAll: () => fetchApi<Meeting[]>('/meetings'),
  getById: (id: number) => fetchApi<Meeting>(`/meetings/${id}`),
  getByStatus: (status: MeetingStatus) => fetchApi<Meeting[]>(`/meetings/status/${status}`),
  getByRoom: (roomId: number) => fetchApi<Meeting[]>(`/meetings/room/${roomId}`),
  create: createMeetingApi,
  update: updateMeetingApi,
  delete: (id: number) =>
    fetchApi<void>(`/meetings/${id}`, { method: 'DELETE' }),
  confirm: (id: number) =>
    fetchApi<Meeting>(`/meetings/${id}/confirm`, { method: 'POST' }),
  reject: (id: number) =>
    fetchApi<Meeting>(`/meetings/${id}/reject`, { method: 'POST' }),
  cancel: (id: number) =>
    fetchApi<Meeting>(`/meetings/${id}/cancel`, { method: 'POST' }),
};

// Verification API
export const verificationApi = {
  getStats: () => fetchApi<VerificationStats>('/meetings/verification/stats'),
  getViolations: () => fetchApi<PropertyViolation[]>('/meetings/verification/violations'),
  checkPending: () =>
    fetchApi<PropertyViolation[]>('/meetings/verification/check-pending', { method: 'POST' }),
  getZ3Enabled: () => fetchApi<{ enabled: boolean }>('/meetings/verification/z3-enabled'),
  setZ3Enabled: (enabled: boolean) =>
    fetchApi<{ enabled: boolean }>('/meetings/verification/z3-enabled', {
      method: 'POST',
      body: JSON.stringify({ enabled }),
    }),
};
