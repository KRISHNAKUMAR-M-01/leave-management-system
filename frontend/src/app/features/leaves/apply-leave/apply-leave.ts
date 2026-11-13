import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs';
import {
  FormGroupDirective,
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
import { AuthService } from '../../../core/services/auth.service';
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
  private readonly authService = inject(AuthService);
  private readonly notification = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly today = new Date(new Date().setHours(0, 0, 0, 0));

  protected readonly leaveTypes = signal<LeaveType[]>([]);
  protected readonly balances = signal<LeaveBalance[]>([]);
  protected readonly loadingReference = signal(true);
  protected readonly submitting = signal(false);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly fileError = signal<string | null>(null);

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

  private readonly formValue = toSignal(
    this.form.valueChanges.pipe(startWith(this.form.getRawValue())),
    { initialValue: this.form.getRawValue() },
  );

  protected readonly minEndDate = computed(() => this.formValue().startDate ?? this.today);

  protected readonly selectedBalance = computed(() => {
    const leaveTypeId = this.formValue().leaveTypeId;
    return this.balances().find((balance) => balance.leaveTypeId === leaveTypeId) ?? null;
  });

  protected readonly previewDays = computed(() => {
    const { startDate, endDate, isHalfDay } = this.formValue();
    return this.calculatePreviewDays(startDate ?? null, endDate ?? null, Boolean(isHalfDay));
  });

  protected readonly requiresMedicalDocument = computed(() => {
    const typeId = this.formValue().leaveTypeId;
    const leaveType = this.leaveTypes().find((t) => t.id === typeId);
    if (!leaveType) {
      return false;
    }
    const name = leaveType.leaveTypeName.trim().toLowerCase();
    const days = this.previewDays();
    if (name === 'sick' && days !== null && days >= 3) {
      return true;
    }
    if (name === 'maternity') {
      return true;
    }
    return false;
  });

  ngOnInit(): void {
    this.loadReferenceData();

    this.form.controls.isHalfDay.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((isHalfDay) => {
        if (isHalfDay) {
          this.form.controls.halfDaySession.setValue('FIRST_HALF');
          this.form.controls.endDate.setValue(this.form.controls.startDate.value);
        } else {
          this.form.controls.halfDaySession.setValue(null);
        }
      });

    this.form.controls.startDate.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((startDate) => {
        if (this.form.controls.isHalfDay.value) {
          this.form.controls.endDate.setValue(startDate);
        }
      });
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (!file) {
      return;
    }

    const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png'];
    if (!allowedTypes.includes(file.type)) {
      this.selectedFile.set(null);
      this.fileError.set('Only PDF, JPG, or PNG files are allowed');
      input.value = '';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.selectedFile.set(null);
      this.fileError.set('File must be 5 MB or smaller');
      input.value = '';
      return;
    }

    this.fileError.set(null);
    this.selectedFile.set(file);
  }

  protected clearFile(input?: HTMLInputElement): void {
    this.selectedFile.set(null);
    this.fileError.set(null);
    if (input) {
      input.value = '';
    }
  }

  protected submit(formDirective?: FormGroupDirective): void {
    if (this.submitting()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.requiresMedicalDocument() && !this.selectedFile()) {
      this.fileError.set('A medical document is required for sick leave of 3 or more days');
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);

    this.leaveService
      .applyForLeave(
        {
          leaveTypeId: value.leaveTypeId!,
          startDate: toIsoDate(value.startDate!),
          endDate: toIsoDate(value.endDate!),
          reason: value.reason.trim(),
          halfDaySession: value.isHalfDay ? value.halfDaySession : null,
        },
        this.requiresMedicalDocument() ? this.selectedFile() : null,
      )
      .subscribe({
        next: () => {
          this.notification.success('Leave application submitted');
          this.clearFile();
          if (formDirective) {
            formDirective.resetForm({ isHalfDay: false, halfDaySession: null, reason: '' });
          } else {
            this.form.reset({ isHalfDay: false, halfDaySession: null, reason: '' });
          }
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

  private calculatePreviewDays(
    startDate: Date | null,
    endDate: Date | null,
    isHalfDay: boolean,
  ): number | null {
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
    const userGender = this.authService.currentUser()?.gender?.toUpperCase();
    this.leaveService.getLeaveTypes().subscribe({
      next: (types) => {
        const filtered = types.filter((t) => {
          const name = t.leaveTypeName.trim().toUpperCase();
          if (userGender === 'MALE' && name === 'MATERNITY') {
            return false;
          }
          if (userGender === 'FEMALE' && name === 'PATERNITY') {
            return false;
          }
          return true;
        });
        this.leaveTypes.set(filtered);
      },
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
