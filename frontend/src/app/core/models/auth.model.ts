export type Role = 'ADMIN' | 'MANAGER' | 'EMPLOYEE';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  fullName: string;
  email: string;
  role: Role;
}

export const ROLE_LABELS: Record<Role, String> = {
  ADMIN: 'Administrator',
  MANAGER: 'Managers',
  EMPLOYEE: 'Employee',
}