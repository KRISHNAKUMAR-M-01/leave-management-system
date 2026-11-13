export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type HalfDaySession = 'FIRST_HALF' | 'SECOND_HALF';

export interface LeaveType {
  id: number;
  leaveTypeName: string;
  daysPerYear: number;
}

export interface LeaveBalance {
  leaveTypeId: number;
  leaveTypeName: string;
  calendarYear: number;
  totalDays: number;
  usedDays: number;
  remainingDays: number;
}

export interface LeaveApplyRequest {
  leaveTypeId: number;
  startDate: string;
  endDate: string;
  reason: string;
  halfDaySession: HalfDaySession | null;
}

export interface LeaveRequestSummary {
  id: number;
  employeeId: number;
  employeeName: string;
  leaveTypeId: number;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  halfDay: boolean;
  halfDaySession: HalfDaySession | null;
  reason: string;
  leaveStatus: LeaveStatus;
  reviewedByName: string | null;
  reviewComment: string | null;
  reviewedAt: string | null;
  createdAt: string;
  hasMedicalDocument?: boolean;
  medicalDocumentName?: string | null;
}

export interface LeaveHistoryFilter {
    status?: LeaveStatus;
    leaveTypeId?: number;
    fromDate?: string;
    toDate?: string;
    page?: number;
    size?: number;
    employeeId?: number;
}

export interface LabelAmount {
  label: string;
  amount: number;
}

export interface LabelCount {
  label: string;
  count: number;
}

export interface AdminDashboard {
  totalEmployees: number;
  activeEmployees: number;
  pendingManagerApprovals: number;
  year: number;
  leavesByStatus: Record<string, number>;
  approvedDaysByLeaveType: LabelAmount[];
  employeesByDepartment: LabelCount[];
}

export interface ManagerDashboard {
  pendingApprovals: number;
  teamSize: number;
}
