import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'leaves/apply',
        loadComponent: () =>
          import('./features/leaves/apply-leave/apply-leave').then((m) => m.ApplyLeave),
      },
      {
        path: 'leaves/history',
        loadComponent: () =>
          import('./features/leaves/leave-history/leave-history').then((m) => m.LeaveHistory),
      },
      {
        path: 'leaves/company',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () =>
          import('./features/leaves/company-leaves/company-leaves').then((m) => m.CompanyLeaves),
      },
      {
        path: 'leaves/approvals',
        canActivate: [roleGuard('ADMIN', 'MANAGER')],
        loadComponent: () =>
          import('./features/leaves/pending-approvals/pending-approvals').then(
            (m) => m.PendingApprovals,
          ),
      },
      {
        path: 'team-calendar',
        canActivate: [roleGuard('MANAGER')],
        loadComponent: () =>
          import('./features/leaves/team-calendar-page/team-calendar-page').then(
            (m) => m.TeamCalendarPage,
          ),
      },
      {
        path: 'employees',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () =>
          import('./features/employees/employee-list/employee-list').then((m) => m.EmployeeList),
      },
      {
        path: 'employees/:id',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () =>
          import('./features/employees/employee-detail/employee-detail').then(
            (m) => m.EmployeeDetail,
          ),
      },
      {
        path: 'holidays',
        loadComponent: () =>
          import('./features/holidays/holiday-list/holiday-list').then((m) => m.HolidayList),
      },
      {
        path: 'unauthorized',
        loadComponent: () =>
          import('./features/errors/error-page/error-page').then((m) => m.ErrorPage),
        data: {
          code: '403',
          title: 'Access denied',
          message: 'You do not have permission to view this page.',
        },
      },
      {
        path: '**',
        loadComponent: () =>
          import('./features/errors/error-page/error-page').then((m) => m.ErrorPage),
        data: {
          code: '404',
          title: 'Page not found',
          message: 'The page you are looking for does not exist.',
        },
      },
    ],
  },
];
