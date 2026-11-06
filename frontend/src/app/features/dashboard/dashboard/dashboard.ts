import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';

import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly http = inject(HttpClient);
  protected readonly authService = inject(AuthService);

  protected readonly departmentCount = signal<number | null>(null);

  constructor() {
    this.http
      .get<unknown[]>(`${environment.apiUrl}/departments`)
      .subscribe((departments) => this.departmentCount.set(departments.length));
  }
}