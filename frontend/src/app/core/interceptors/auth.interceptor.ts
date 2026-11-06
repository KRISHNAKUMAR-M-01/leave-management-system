import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import {inject} from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
    const authService = inject(AuthService);

    const isOwnApi = request.url.startsWith(environment.apiUrl);
    const isLoginCall = request.url.endsWith('/auth/login');
    const token = authService.token();

    const authorizedRequest = token && isOwnApi && !isLoginCall ? request.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }}) : request;
    
    return next(authorizedRequest).pipe(
        catchError((error: unknown) => {
            const isUnauthorized = error instanceof HttpErrorResponse && error.status === 401;
            if (isUnauthorized && !isLoginCall && authService.isLoggedIn()) {
                authService.logout(true);
            }
            return throwError(() => error);
        })
    );
};