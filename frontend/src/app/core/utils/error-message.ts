import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api-error.model';

const NETWORK_ERROR_MESSAGE = 'Network error occurred. Please check your internet connection.';
const DEFAULT_ERROR_MESSAGE = 'An unexpected error occurred. Please try again later.';

export function getErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
        return DEFAULT_ERROR_MESSAGE;
    }
    if (error.status === 0) {
        return NETWORK_ERROR_MESSAGE;
    }
    const apiError = error.error as Partial<ApiError> | null;
    return apiError?.message ?? DEFAULT_ERROR_MESSAGE;
}

export function getFieldErrors(error: unknown): Record<string, string> {
    if (error instanceof HttpErrorResponse) {
        const apiError = error.error as Partial<ApiError> | null;
        return apiError?.fieldErrors ?? {};
    }
    return {};
} 