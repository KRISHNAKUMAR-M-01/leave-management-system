import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  LeaveApplyRequest,
  LeaveBalance,
  LeaveRequestSummary,
  LeaveType,
  LeaveHistoryFilter,
  AdminDashboard,
  ManagerDashboard,
} from '../models/leave.model';
import { Employee } from '../models/employee.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class LeaveService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  // Fetch all available leave types
  getLeaveTypes(): Observable<LeaveType[]> {
    return this.http.get<LeaveType[]>(`${this.baseUrl}/leave-types`);
  }

  // Fetch the logged-in employee's leave balances
  getMyBalances(): Observable<LeaveBalance[]> {
    return this.http.get<LeaveBalance[]>(`${this.baseUrl}/leave-balances/me`);
  }

  //Fetch leave balances for a specific employee.
  getEmployeeBalances(employeeId: number): Observable<LeaveBalance[]> {
    return this.http.get<LeaveBalance[]>(`${this.baseUrl}/leave-balances/employee/${employeeId}`);
  }

  // Submit a new leave application (with optional medical document).
  applyForLeave(request: LeaveApplyRequest, medicalDocument?: File | null): Observable<LeaveRequestSummary> {
    if (medicalDocument) {
      const formData = new FormData();
      formData.append(
        'request',
        new Blob([JSON.stringify(request)], { type: 'application/json' }),
      );
      formData.append('medicalDocument', medicalDocument);
      return this.http.post<LeaveRequestSummary>(`${this.baseUrl}/leaves`, formData);
    }
    return this.http.post<LeaveRequestSummary>(`${this.baseUrl}/leaves`, request);
  }

  // Fetch uploaded medical document for a leave request.
  getMedicalDocument(requestId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/leaves/${requestId}/medical-document`, {
      responseType: 'blob',
    });
  }

  // Fetch the logged-in employee's leave history with filters and pagination
  getMyLeaves(filter: LeaveHistoryFilter): Observable<Page<LeaveRequestSummary>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<LeaveRequestSummary>>(`${this.baseUrl}/leaves/my`, { params });
  }

  // Fetch company-wide leave history with filters and pagination.
  getAllLeaves(filter: LeaveHistoryFilter): Observable<Page<LeaveRequestSummary>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<LeaveRequestSummary>>(`${this.baseUrl}/leaves`, { params });
  }

  // Generate leave balances for the specified year.
  generateYearlyBalances(year: number): Observable<{ created: number }> {
    return this.http.post<{ created: number }>(
      `${this.baseUrl}/leave-balances/generate/${year}`,
      {},
    );
  }

  // Fetch pending leave requests.
  getPendingRequests(): Observable<LeaveRequestSummary[]> {
    return this.http.get<LeaveRequestSummary[]>(`${this.baseUrl}/leaves/pending`);
  }

  // Approve a leave request by ID.
  approveLeave(id: number): Observable<LeaveRequestSummary> {
    return this.http.patch<LeaveRequestSummary>(`${this.baseUrl}/leaves/${id}/approve`, {});
  }

  // Reject a leave request with a rejection comment.
  rejectLeave(id: number, comment: string): Observable<LeaveRequestSummary> {
    return this.http.patch<LeaveRequestSummary>(`${this.baseUrl}/leaves/${id}/reject`, { comment });
  }

  // Cancel a leave request by ID.
  cancelLeave(id: number): Observable<LeaveRequestSummary> {
    return this.http.patch<LeaveRequestSummary>(`${this.baseUrl}/leaves/${id}/cancel`, {});
  }

  // Fetch dashboard statistics for the Admin.
  getAdminDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(`${this.baseUrl}/dashboard/admin`);
  }

  // Fetch dashboard statistics for the Manager.
  getManagerDashboard(): Observable<ManagerDashboard> {
    return this.http.get<ManagerDashboard>(`${this.baseUrl}/dashboard/manager`);
  }

  // Fetch team leave records within the specified date range.
  getTeamCalendar(from: string, to: string): Observable<LeaveRequestSummary[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<LeaveRequestSummary[]>(`${this.baseUrl}/dashboard/team-calendar`, {
      params,
    });
  }

  // Fetch employees belonging to the logged-in Manager's team.
  getTeamMembers(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.baseUrl}/dashboard/team-members`);
  }
}
