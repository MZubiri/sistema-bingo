import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginResponse, Role } from './types';
import { apiBaseUrl } from './api-url';

const API_URL = apiBaseUrl();
const SESSION_KEY = 'bingo.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sessionSignal = signal<LoginResponse | null>(this.loadSession());
  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.sessionSignal());
  readonly role = computed(() => this.sessionSignal()?.role ?? null);

  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_URL}/auth/login`, { username, password }).pipe(
      tap((session) => {
        localStorage.setItem(SESSION_KEY, JSON.stringify(session));
        this.sessionSignal.set(session);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(SESSION_KEY);
    this.sessionSignal.set(null);
    this.router.navigateByUrl('/login');
  }

  token(): string | null {
    const session = this.sessionSignal();
    if (!session || this.isTokenExpired(session.token)) {
      this.clearSession();
      return null;
    }
    return session.token;
  }

  hasRole(role: Role): boolean {
    return this.token() !== null && this.sessionSignal()?.role === role;
  }

  redirectByRole(): void {
    const role = this.sessionSignal()?.role;
    this.router.navigateByUrl(role === 'ADMIN' ? '/admin/dashboard' : '/representante/dashboard');
  }

  clearSession(): void {
    localStorage.removeItem(SESSION_KEY);
    this.sessionSignal.set(null);
  }

  private loadSession(): LoginResponse | null {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    try {
      const session = JSON.parse(raw) as LoginResponse;
      if (this.isTokenExpired(session.token)) {
        localStorage.removeItem(SESSION_KEY);
        return null;
      }
      return session;
    } catch {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}
