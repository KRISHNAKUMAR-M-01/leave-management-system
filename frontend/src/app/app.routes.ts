import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';

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
