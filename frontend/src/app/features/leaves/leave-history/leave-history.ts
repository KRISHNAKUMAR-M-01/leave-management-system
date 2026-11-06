import { Component, inject, OnInit, signal } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LeaveHistoryFilter, LeaveRequestSummary } from '../../../core/models/leave.model';
import { LeaveService } from '../../../core/services/leave.service';
import { NotificationService } from '../../../core/services/notification.service';
import { getErrorMessage } from '../../../core/utils/error-message';
import { MatTableModule } from '@angular/material/table';
import { StatusBadge } from '../../../shared/status-badge/status-badge';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { LeaveStatus, LeaveType } from '../../../core/models/leave.model';
import { toIsoDate } from '../../../core/utils/date';
import { MatInputModule } from '@angular/material/input';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-leave-history',
  imports: [
    MatProgressSpinnerModule, 
    MatTableModule, 
    StatusBadge,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
  ],
  templateUrl: './leave-history.html',
  styleUrl: './leave-history.scss',
})
export class LeaveHistory implements OnInit {

  protected readonly displayedColumns = ['leaveType', 'dates', 'days', 'status', 'reason'];

  private readonly leaveService = inject(LeaveService);
  private readonly notification = inject(NotificationService);

  protected readonly requests = signal<LeaveRequestSummary[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly currentPage = signal(0);
  protected readonly loading = signal(true);

    private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly leaveTypes = signal<LeaveType[]>([]);

  protected readonly filterForm = this.formBuilder.group({
    status: this.formBuilder.control<LeaveStatus | null>(null),
    leaveTypeId: this.formBuilder.control<number | null>(null),
    fromDate: this.formBuilder.control<Date | null>(null),
    toDate: this.formBuilder.control<Date | null>(null),
  });

  private filter: LeaveHistoryFilter = {};

    ngOnInit(): void {
    this.leaveService.getLeaveTypes().subscribe((types) => this.leaveTypes.set(types));
    this.loadPage(0);
  }

  protected applyFilters(): void {
    const value = this.filterForm.getRawValue();
    this.filter = {
      status: value.status ?? undefined,
      leaveTypeId: value.leaveTypeId ?? undefined,
      fromDate: value.fromDate ? toIsoDate(value.fromDate) : undefined,
      toDate: value.toDate ? toIsoDate(value.toDate) : undefined,
    };
    this.loadPage(0);
  }

  protected clearFilters(): void {
    this.filterForm.reset();
    this.filter = {};
    this.loadPage(0);
  }

  protected loadPage(page: number): void {
    this.loading.set(true);

    this.leaveService
      .getMyLeaves({ ...this.filter, page, size: PAGE_SIZE })
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
}