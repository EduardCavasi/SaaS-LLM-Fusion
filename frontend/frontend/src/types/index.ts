// API Types for the Verified Meeting Scheduler

export interface Room {
  id: number;
  name: string;
  capacity: number;
  location?: string;
  description?: string;
  available: boolean;
}

export interface Participant {
  id: number;
  name: string;
  email: string;
  department?: string;
}

export type MeetingStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';

export interface Meeting {
  id: number;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  roomId: number;
  roomName?: string;
  participantIds: number[];
  participants?: Participant[];
  status: MeetingStatus;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateMeetingRequest {
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  roomId: number;
  participantIds: number[];
}

export interface SchedulingResult {
  success: boolean;
  meeting?: Meeting;
  constraintViolations: string[];
  runtimeWarnings: string[];
  solverStatus: string;
  explanation: string;
  solvingTimeMs: number;
}

export type ViolationSeverity = 'WARNING' | 'ERROR' | 'CRITICAL';

export interface PropertyViolation {
  propertyName: string;
  description: string;
  severity: ViolationSeverity;
  meetingId?: number;
  detectedAt: string;
  details: string;
}

export interface VerificationStats {
  z3SolverInitialized: boolean;
  totalEvents: number;
  pendingMeetings: number;
  trackedMeetings: number;
  totalViolations: number;
  criticalViolations: number;
  errorViolations: number;
  warningViolations: number;
}

