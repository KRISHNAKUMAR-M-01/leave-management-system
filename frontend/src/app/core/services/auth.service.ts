import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, Role } from '../models/auth.model';
import { NotificationService } from './notification.service';
import { getTokenExpiryMs } from '../utils/Jwt';

const SESSION_KEY = 'leave_management_session';
const MAX_TIMER_MS =2_147_483_647;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notification = inject(NotificationService);

  private readonly session = signal<LoginResponse | null>(null);
  private expiryTimer: ReturnType<typeof setTimeout> | undefined;

  readonly currentUser = this.session.asReadonly();
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly token = computed(() => this.session()?.token ?? null);

  constructor() {
    this.restoreSession();
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap((response) => {
        this.startSession(response);
      })
    );
  }

  logout(sessionExpired = false): void {
    this.clearSession();
    if(sessionExpired) {
        this.notification.error('Your session has expired. Please log in again.');
    }
    void this.router.navigate(['/login']);
  }

  hasRole(...roles: Role[]): boolean {
    const currentRole = this.session()?.role;
    return currentRole !== undefined && roles.includes(currentRole);
  }

  private startSession(response: LoginResponse): void {
    this.session.set(response);
    this.saveSession(response);
    this.scheduleAutoLogout(response.token);
  }

  private restoreSession(): void {
    const stored = this.readStoredSession();
    if (stored === null) {
        return;
    }

    const expiry = getTokenExpiryMs(stored.token);
    if (expiry === null || expiry <= Date.now()) {
        this.clearStoredSession();
        return;
    }

    this.session.set(stored);
    this.scheduleAutoLogout(stored.token);
  }

  private scheduleAutoLogout(token: string): void {
    clearTimeout(this.expiryTimer);

    const expiry = getTokenExpiryMs(token);
    if (expiry === null) {
        return;
    }

    const delay = Math.min(expiry - Date.now(), MAX_TIMER_MS);
    this.expiryTimer = setTimeout(() => this.logout(true), Math.max(delay, 0));
  }

  private clearSession(): void {
    clearTimeout(this.expiryTimer);
    this.session.set(null);
    this.clearStoredSession();
  }

  private saveSession(session: LoginResponse): void {
    try {
        localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {}
  }

  private readStoredSession(): LoginResponse | null {
    try {
        const stored = localStorage.getItem(SESSION_KEY);
        return stored ? (JSON.parse(stored) as LoginResponse) : null;
    } catch {
        return null;
    }
  }

  private clearStoredSession(): void {
    try {
        localStorage.removeItem(SESSION_KEY);
    } catch {}
  }

}
