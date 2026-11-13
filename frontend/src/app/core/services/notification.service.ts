import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

const DISPLAY_DURATION_MS = 4000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  // Display a success notification
  success(message: string): void {
    this.show(message, 'success-snackbar');
  }

  // Display a error notification
  error(message: string): void {
    this.show(message, 'error-snackbar');
  }

  // Display a snackbar notification with the specified message and styling
  private show(message: string, panelClass: string): void {
    this.snackBar.open(message, 'Close', {
      duration: DISPLAY_DURATION_MS,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass,
    });
  }
}
