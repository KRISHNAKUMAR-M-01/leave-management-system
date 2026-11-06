import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/auth.model';

export function roleGuard(...allowedRoles: Role[]): CanActivateFn {
    return () => {
        const authService = inject(AuthService);
        const router = inject(Router);
    return authService.hasRole(...allowedRoles) ? true : router.createUrlTree(['/dashboard']);
    }
}