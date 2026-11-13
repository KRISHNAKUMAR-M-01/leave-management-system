import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { toIsoDate } from '../../core/utils/date';
import { PublicHolidayRequest } from '../../core/models/public-holiday.model';

@Component({
  selector: 'app-holiday-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './holiday-form-dialog.html',
  styleUrl: './holiday-form-dialog.scss',
})
export class HolidayFormDialog {
  private readonly dialogRef = inject(MatDialogRef<HolidayFormDialog, PublicHolidayRequest>);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly form = this.formBuilder.group({
    holidayName: ['', [Validators.required, Validators.maxLength(100)]],
    holidayDate: [new Date(), [Validators.required]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.getRawValue();
    const result: PublicHolidayRequest = {
      holidayName: val.holidayName.trim(),
      holidayDate: toIsoDate(val.holidayDate),
    };

    this.dialogRef.close(result);
  }
}

