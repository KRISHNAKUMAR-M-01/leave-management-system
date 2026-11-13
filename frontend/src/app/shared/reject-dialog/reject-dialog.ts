import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface RejectDialogData {
  title?: string;
  message?: string;
  employeeName?: string;
}

@Component({
  selector: 'app-reject-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './reject-dialog.html',
  styleUrl: './reject-dialog.scss',
})
export class RejectDialog {
  protected readonly data = inject<RejectDialogData>(MAT_DIALOG_DATA, { optional: true });
  private readonly dialogRef = inject(MatDialogRef<RejectDialog>);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly commentControl = this.formBuilder.control('', [
    Validators.required,
    Validators.maxLength(500),
  ]);

  protected submit(): void {
    if (this.commentControl.invalid) {
      this.commentControl.markAsTouched();
      return;
    }
    this.dialogRef.close(this.commentControl.value.trim());
  }
}
