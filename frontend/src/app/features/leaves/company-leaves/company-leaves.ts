import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, DatePipe } from '@angular/common';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { LeaveService } from '../../../core/services/leave.service';
import { EmployeeService } from '../../../core/services/employee.service';
import { NotificationService } from '../../../core/services/notification.service';
import { getErrorMessage } from '../../../core/utils/error-message';
import { toIsoDate } from '../../../core/utils/date';
import { LeaveBalance, LeaveHistoryFilter, LeaveRequestSummary, LeaveStatus, LeaveType } from '../../../core/models/leave.model';
import { Employee } from '../../../core/models/employee.model';
import { StatusBadge } from '../../../shared/status-badge/status-badge';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-company-leaves',
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatAutocompleteModule,
    StatusBadge,
  ],
  templateUrl: './company-leaves.html',
  styleUrl: './company-leaves.scss',
})
export class CompanyLeaves implements OnInit {
  protected readonly displayedColumns = [
    'employeeName',
    'leaveType',
    'dates',
    'days',
    'status',
    'reason',
  ];
  protected readonly PAGE_SIZE = PAGE_SIZE;

  private readonly leaveService = inject(LeaveService);
  private readonly employeeService = inject(EmployeeService);
  private readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  private readonly destroyRef = inject(DestroyRef);
  protected readonly loading = signal(true);
  protected readonly balancesLoading = signal(false);
  protected readonly requests = signal<LeaveRequestSummary[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly currentPage = signal(0);
  protected readonly leaveTypes = signal<LeaveType[]>([]);

  // Employee Autocomplete & Selection
  protected readonly employeeSearchControl = new FormControl<string | Employee>('');
  protected readonly employeeSuggestions = signal<Employee[]>([]);
  protected readonly selectedEmployee = signal<Employee | null>(null);
  protected readonly employeeBalances = signal<LeaveBalance[]>([]);

  // Summary counts for current filter / query
  protected readonly statusCounts = signal({
    total: 0,
    approved: 0,
    pending: 0,
    rejected: 0,
    cancelled: 0,
  });

  // Balance totals for selected employee
  protected readonly totalAllocated = computed(() =>
    this.employeeBalances().reduce((acc, b) => acc + b.totalDays, 0),
  );
  protected readonly totalUsed = computed(() =>
    this.employeeBalances().reduce((acc, b) => acc + b.usedDays, 0),
  );
  protected readonly totalRemaining = computed(() =>
    this.employeeBalances().reduce((acc, b) => acc + b.remainingDays, 0),
  );

  protected readonly filterForm = this.formBuilder.group({
    status: this.formBuilder.control<LeaveStatus | null>(null),
    leaveTypeId: this.formBuilder.control<number | null>(null),
    fromDate: this.formBuilder.control<Date | null>(null),
    toDate: this.formBuilder.control<Date | null>(null),
  });

  private filter: LeaveHistoryFilter = {};

  ngOnInit(): void {
    this.leaveService.getLeaveTypes().subscribe((types) => this.leaveTypes.set(types));
    this.loadAdminStats();
    this.loadPage(0);
    this.setupEmployeeAutocomplete();
  }

  private setupEmployeeAutocomplete(): void {
    this.employeeSearchControl.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((query) => {
          if (typeof query !== 'string' || !query.trim()) {
            return of([]);
          }
          return this.employeeService
            .search({ search: query.trim(), active: true, size: 5 })
            .pipe(
              map((res) => res.content),
              catchError(() => of([])),
            );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((employees) => {
        this.employeeSuggestions.set(employees);
      });
  }

  protected displayEmployeeFn(emp: Employee | string | null): string {
    if (!emp) return '';
    return typeof emp === 'string' ? emp : emp.fullName;
  }

  protected onEmployeeSelected(event: MatAutocompleteSelectedEvent): void {
    const emp: Employee = event.option.value;
    this.selectedEmployee.set(emp);
    this.filter.employeeId = emp.id;
    this.loadEmployeeBalances(emp.id);
    this.loadPage(0);
  }

  protected clearEmployee(): void {
    this.selectedEmployee.set(null);
    this.employeeSearchControl.setValue('');
    this.employeeBalances.set([]);
    delete this.filter.employeeId;
    this.loadPage(0);
  }

  private loadEmployeeBalances(employeeId: number): void {
    this.balancesLoading.set(true);
    this.leaveService.getEmployeeBalances(employeeId).subscribe({
      next: (balances) => {
        this.employeeBalances.set(balances);
        this.balancesLoading.set(false);
      },
      error: () => {
        this.employeeBalances.set([]);
        this.balancesLoading.set(false);
      },
    });
  }

  private loadAdminStats(): void {
    this.leaveService.getAdminDashboard().subscribe({
      next: (admin) => {
        const byStatus = admin.leavesByStatus || {};
        const pending = byStatus['PENDING'] || 0;
        const approved = byStatus['APPROVED'] || 0;
        const rejected = byStatus['REJECTED'] || 0;
        const cancelled = byStatus['CANCELLED'] || 0;
        const total = pending + approved + rejected + cancelled;

        this.statusCounts.set({
          total,
          approved,
          pending,
          rejected,
          cancelled,
        });
      },
      error: () => {},
    });
  }

  protected applyFilters(): void {
    const value = this.filterForm.getRawValue();
    this.filter = {
      ...this.filter,
      status: value.status ?? undefined,
      leaveTypeId: value.leaveTypeId ?? undefined,
      fromDate: value.fromDate ? toIsoDate(value.fromDate) : undefined,
      toDate: value.toDate ? toIsoDate(value.toDate) : undefined,
    };
    this.loadPage(0);
  }

  protected clearFilters(): void {
    this.filterForm.reset();
    const empId = this.filter.employeeId;
    this.filter = empId ? { employeeId: empId } : {};
    this.loadPage(0);
  }

  protected onPageChange(event: PageEvent): void {
    this.loadPage(event.pageIndex);
  }

  protected loadPage(page: number): void {
    this.loading.set(true);
    this.leaveService
      .getAllLeaves({ ...this.filter, page, size: PAGE_SIZE })
      .subscribe({
        next: (result) => {
          this.requests.set(result.content);
          this.totalElements.set(result.totalElements);
          this.currentPage.set(result.page);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.notification.error(getErrorMessage(error));
          this.loading.set(false);
        },
      });
  }

  protected viewMedicalDocument(id: number): void {
    this.leaveService.getMedicalDocument(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (error: unknown) => this.notification.error(getErrorMessage(error)),
    });
  }
}

