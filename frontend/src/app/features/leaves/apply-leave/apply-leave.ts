import { Component, computed, inject, OnInit, signal } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { HalfDaySession, LeaveBalance, LeaveType } from '../../../core/models/leave.model';
import { LeaveService } from '../../../core/services/leave.service';
import { NotificationService } from '../../../core/services/notification.service';
import { isWeekend, toIsoDate } from '../../../core/utils/date';
import { getErrorMessage, getFieldErrors } from '../../../core/utils/error-message';

@Component({
  selector: 'app-apply-leave',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  templateUrl: './apply-leave.html',
  styleUrl: './apply-leave.scss',
})
export class ApplyLeave implements OnInit {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly leaveService = inject(LeaveService);
  private readonly notification = inject(NotificationService);

  protected readonly today = new Date();
  protected readonly minEndDate = computed(() => this.form?.controls.startDate.value ?? this.today);

  protected readonly leaveTypes = signal<LeaveType[]>([]);
  protected readonly balances = signal<LeaveBalance[]>([]);
  protected readonly loadingReference = signal(true);
  protected readonly submitting = signal(false);

  protected readonly form = this.formBuilder.group(
    {
      leaveTypeId: this.formBuilder.control<number | null>(null, Validators.required),
      startDate: this.formBuilder.control<Date | null>(null, Validators.required),
      endDate: this.formBuilder.control<Date | null>(null, Validators.required),
      isHalfDay: false,
      halfDaySession: this.formBuilder.control<HalfDaySession | null>(null),
      reason: ['', [Validators.required, Validators.maxLength(500)]],
    },
    { validators: [dateRangeValidator, halfDaySingleDateValidator] },
  );

  protected readonly selectedBalance = computed(() => {
    const leaveTypeId = this.form.controls.leaveTypeId.value;
    return this.balances().find((balance) => balance.leaveTypeId === leaveTypeId) ?? null;
  });

  protected readonly previewDays = computed(() => this.calculatePreviewDays());

  ngOnInit(): void {
    this.loadReferenceData();

    this.form.controls.isHalfDay.valueChanges.subscribe((isHalfDay) => {
      if (isHalfDay) {
        this.form.controls.halfDaySession.setValue('FIRST_HALF');
        this.form.controls.endDate.setValue(this.form.controls.startDate.value);
      } else {
        this.form.controls.halfDaySession.setValue(null);
      }
    });
  }

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);

    this.leaveService
      .applyForLeave({
        leaveTypeId: value.leaveTypeId!,
        startDate: toIsoDate(value.startDate!),
        endDate: toIsoDate(value.endDate!),
        reason: value.reason.trim(),
        halfDaySession: value.isHalfDay ? value.halfDaySession : null,
      })
      .subscribe({
        next: () => {
          this.notification.success('Leave application submitted');
          this.form.reset({ isHalfDay: false, halfDaySession: null, reason: '' });
          this.loadBalances();
          this.submitting.set(false);
        },
        error: (error: unknown) => {
          this.applyFieldErrors(error);
          this.notification.error(getErrorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  private calculatePreviewDays(): number | null {
    const { startDate, endDate, isHalfDay } = this.form.getRawValue();
    if (!startDate || !endDate || startDate > endDate) {
      return null;
    }
    if (isHalfDay) {
      return 0.5;
    }

    let count = 0;
    for (let day = new Date(startDate); day <= endDate; day.setDate(day.getDate() + 1)) {
      if (!isWeekend(day)) {
        count += 1;
      }
    }
    return count;
  }

  private applyFieldErrors(error: unknown): void {
    const fieldErrors = getFieldErrors(error);
    for (const [field, message] of Object.entries(fieldErrors)) {
      this.form.get(field)?.setErrors({ server: message });
    }
  }

  private loadReferenceData(): void {
    this.loadingReference.set(true);
    this.leaveService.getLeaveTypes().subscribe({
      next: (types) => this.leaveTypes.set(types),
      error: (error: unknown) => this.notification.error(getErrorMessage(error)),
    });
    this.loadBalances(() => this.loadingReference.set(false));
  }

  private loadBalances(onComplete?: () => void): void {
    this.leaveService.getMyBalances().subscribe({
      next: (balances) => {
        this.balances.set(balances);
        onComplete?.();
      },
      error: (error: unknown) => {
        this.notification.error(getErrorMessage(error));
        onComplete?.();
      },
    });
  }
}

const dateRangeValidator: ValidatorFn = (control): ValidationErrors | null => {
  const startDate = control.get('startDate')?.value as Date | null;
  const endDate = control.get('endDate')?.value as Date | null;

  if (!startDate || !endDate) {
    return null;
  }
  return endDate < startDate ? { dateRange: true } : null;
};

const halfDaySingleDateValidator: ValidatorFn = (control): ValidationErrors | null => {
  const isHalfDay = control.get('isHalfDay')?.value as boolean;
  const startDate = control.get('startDate')?.value as Date | null;
  const endDate = control.get('endDate')?.value as Date | null;

  if (!isHalfDay || !startDate || !endDate) {
    return null;
  }
  return toIsoDate(startDate) !== toIsoDate(endDate) ? { halfDayRange: true } : null;
};