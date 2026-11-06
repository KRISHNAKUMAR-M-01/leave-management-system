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
} from '../models/leave.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class LeaveService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getLeaveTypes(): Observable<LeaveType[]> {
    return this.http.get<LeaveType[]>(`${this.baseUrl}/leave-types`);
  }

  getMyBalances(): Observable<LeaveBalance[]> {
    return this.http.get<LeaveBalance[]>(`${this.baseUrl}/leave-balances/me`);
  }

  applyForLeave(request: LeaveApplyRequest): Observable<LeaveRequestSummary> {
    return this.http.post<LeaveRequestSummary>(`${this.baseUrl}/leaves`, request);
  }

    getMyLeaves(filter: LeaveHistoryFilter): Observable<Page<LeaveRequestSummary>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<LeaveRequestSummary>>(`${this.baseUrl}/leaves/my`, { params });
  }
}