import { Component, inject, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Department } from '../../core/models/department.model';
import { Employee } from '../../core/models/employee.model';
import { DepartmentService } from '../../core/services/department.service';
import { EmployeeService } from '../../core/services/employee.service';
import { getFieldErrors } from '../../core/utils/error-message';
import { parseLocalDate, toIsoDate } from '../../core/utils/date';

export interface EmployeeFormDialogData {
  employee: Employee | null;
}

@Component({
  selector: 'app-employee-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './employee-form-dialog.html',
  styleUrl: './employee-form-dialog.scss',
})
export class EmployeeFormDialog implements OnInit {
  protected readonly data = inject<EmployeeFormDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EmployeeFormDialog>);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly departmentService = inject(DepartmentService);
  private readonly employeeService = inject(EmployeeService);

  protected readonly isEditMode = this.data.employee !== null;
  protected readonly roles = ['ADMIN', 'MANAGER', 'EMPLOYEE'];

  protected readonly departments = signal<Department[]>([]);
  protected readonly managers = signal<Employee[]>([]);
  protected readonly submitting = signal(false);

  protected readonly form = this.formBuilder.group({
    fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.pattern(/@codeboard\.com$/i),
        Validators.maxLength(150),
      ],
    ],
    password: [''],
    phone: ['', [Validators.required, Validators.pattern(/^[6-9][0-9]{9}$/)]],
    departmentId: this.formBuilder.control<number | null>(null, Validators.required),
    designation: ['', [Validators.required, Validators.maxLength(50)]],
    joiningDate: this.formBuilder.control<Date | null>(null, Validators.required),
    roleName: this.formBuilder.control<string | null>(null, Validators.required),
    managerId: this.formBuilder.control<number | null>(null),
  });

  ngOnInit(): void {
    this.departmentService.getAll().subscribe((departments) => this.departments.set(departments));
    this.loadManagers();

    if (this.isEditMode) {
      this.form.controls.password.clearValidators();
      this.patchFromExisting(this.data.employee!);
    } else {
      this.form.controls.password.setValidators([
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(64),
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,64}$/),
      ]);
    }

    this.form.controls.roleName.valueChanges.subscribe((role) =>
      this.updateManagerValidators(role),
    );
  }

  private patchFromExisting(employee: Employee): void {
    this.form.patchValue({
      fullName: employee.fullName,
      email: employee.email,
      phone: employee.phone,
      departmentId: employee.departmentId,
      designation: employee.designation,
      joiningDate: parseLocalDate(employee.joiningDate),
      roleName: employee.roleName,
      managerId: employee.managerId,
    });
  }

  private loadManagers(): void {
    this.employeeService
      .search({ roleName: 'MANAGER', active: true, size: 100 })
      .subscribe((page) => this.managers.set(page.content));
  }

  private updateManagerValidators(role: string | null): void {
    const managerControl = this.form.controls.managerId;
    if (role === 'EMPLOYEE') {
      managerControl.setValidators(Validators.required);
    } else {
      managerControl.setValidators(null);
      managerControl.setValue(null);
    }
    managerControl.updateValueAndValidity();
  }

  protected submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);

    const payload = {
      fullName: value.fullName.trim(),
      email: value.email.trim(),
      phone: value.phone,
      departmentId: value.departmentId!,
      designation: value.designation.trim(),
      joiningDate: toIsoDate(value.joiningDate!),
      roleName: value.roleName!,
      managerId: value.managerId,
      ...(this.isEditMode ? {} : { password: value.password }),
    };

    const request$ = this.isEditMode
      ? this.employeeService.update(this.data.employee!.id, payload)
      : this.employeeService.create(payload);

    request$.subscribe({
      next: (employee) => {
        this.submitting.set(false);
        this.dialogRef.close(employee);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.applyFieldErrors(error);
      },
    });
  }

  private applyFieldErrors(error: unknown): void {
    const fieldErrors = getFieldErrors(error);
    for (const [field, message] of Object.entries(fieldErrors)) {
      this.form.get(field)?.setErrors({ server: message });
    }
  }
}
