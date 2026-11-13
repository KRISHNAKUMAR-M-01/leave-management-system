import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Employee, EmployeeFilter } from '../models/employee.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  // Search employees using filters and pagination
  search(filter: EmployeeFilter): Observable<Page<Employee>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<Employee>>(`${this.baseUrl}/employees`, { params });
  }

  // Deactivate an employee by ID
  deactivate(id: number): Observable<Employee> {
    return this.http.patch<Employee>(`${this.baseUrl}/employees/${id}/deactivate`, {});
  }

  // Activate a previously deactivated employee by ID
  activate(id: number): Observable<Employee> {
    return this.http.patch<Employee>(`${this.baseUrl}/employees/${id}/activate`, {});
  }

  // Permanently delete an employee by ID
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/employees/${id}`);
  }

  //Create a new employee
  create(payload: unknown): Observable<Employee> {
    return this.http.post<Employee>(`${this.baseUrl}/employees`, payload);
  }

  // Update an existing employee by ID
  update(id: number, payload: unknown): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/employees/${id}`, payload);
  }

  // Retrieve an employee by ID
  getById(id: number): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/employees/${id}`);
  }
}
