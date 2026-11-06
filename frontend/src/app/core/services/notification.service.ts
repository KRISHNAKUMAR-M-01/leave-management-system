import {inject, Injectable} from '@angular/core';
import {MatSnackBar} from '@angular/material/snack-bar';

const DISPLAY_DURATION_MS = 4000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.show(message, 'success-snackbar');
  }

   error(message: string): void {
    this.show(message, 'error-snackbar');
  }

  private show(message: string, panelClass: string): void {
    this.snackBar.open(message, 'Close', {
      duration: DISPLAY_DURATION_MS,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass,
    });
  }
}
