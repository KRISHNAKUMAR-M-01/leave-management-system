import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, Role } from '../models/auth.model';
import { NotificationService } from './notification.service';
import { getTokenExpiryMs } from '../utils/Jwt';

const SESSION_KEY = 'leave_management_session';
const MAX_TIMER_MS = 2_147_483_647; // Maximum delay for setTimeout in milliseconds (approximately 24.8 days)

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

  // check for an active user session whenever the app boots up or refreshes;
  constructor() {
    this.restoreSession();
  }

  // This method is used to log in a user by sending their credentials to the backend API. It returns an Observable of LoginResponse.
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap((response) => {
        this.startSession(response);
      }),
    );
  }

  // This method is used to log out the current user. It clears the session and navigates to the login page.
  logout(sessionExpired = false): void {
    this.clearSession();
    if (sessionExpired) {
      this.notification.error('Your session has expired. Please log in again.');
    }
    void this.router.navigate(['/login']);
  }

  // checks if the currentlty logged-in user has one of the allwed role types.
  hasRole(...roles: Role[]): boolean {
    const currentRole = this.session()?.role;
    return currentRole !== undefined && roles.includes(currentRole);
  }

  // initialize a new authenticated session after a successful login
  private startSession(response: LoginResponse): void {
    this.session.set(response);
    this.saveSession(response);
    this.scheduleAutoLogout(response.token);
  }

  // restore the saved session only if its JWT is still valid
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

  // schedule automatic logout when the JWT expires
  private scheduleAutoLogout(token: string): void {
    clearTimeout(this.expiryTimer);

    const expiry = getTokenExpiryMs(token);
    if (expiry === null) {
      return;
    }

    const delay = Math.min(expiry - Date.now(), MAX_TIMER_MS);
    this.expiryTimer = setTimeout(() => this.logout(true), Math.max(delay, 0));
  }

  // Clear the session signal, cancel the expiry timer, and remove stored session data.
  private clearSession(): void {
    clearTimeout(this.expiryTimer);
    this.session.set(null);
    this.clearStoredSession();
  }

  // Save the user's session in localStorage for restoration after a page refresh.
  private saveSession(session: LoginResponse): void {
    try {
      localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {}
  }

  // Retrieve and parse the saved session from localStorage.
  private readStoredSession(): LoginResponse | null {
    try {
      const stored = localStorage.getItem(SESSION_KEY);
      return stored ? (JSON.parse(stored) as LoginResponse) : null;
    } catch {
      return null;
    }
  }

  // Remove the saved user session from localStorage.
  private clearStoredSession(): void {
    try {
      localStorage.removeItem(SESSION_KEY);
    } catch {}
  }
}
