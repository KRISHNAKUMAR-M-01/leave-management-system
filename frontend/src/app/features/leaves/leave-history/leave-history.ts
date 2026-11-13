import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LeaveHistoryFilter, LeaveRequestSummary } from '../../../core/models/leave.model';
import { LeaveService } from '../../../core/services/leave.service';
import { AuthService } from '../../../core/services/auth.service';
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
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialog } from '../../../shared/confirm-dialog/confirm-dialog';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-leave-history',
  imports: [
    DatePipe,
    MatProgressSpinnerModule,
    MatTableModule,
    StatusBadge,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatPaginatorModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './leave-history.html',
  styleUrl: './leave-history.scss',
})
export class LeaveHistory implements OnInit {
  protected readonly displayedColumns = [
    'leaveType',
    'dates',
    'days',
    'status',
    'reason',
    'actions',
  ];
  protected readonly PAGE_SIZE = PAGE_SIZE;

  private readonly leaveService = inject(LeaveService);
  private readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly dialog = inject(MatDialog);

  protected readonly requests = signal<LeaveRequestSummary[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly currentPage = signal(0);
  protected readonly loading = signal(true);
  protected readonly cancellingId = signal<number | null>(null);
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

  protected onPageChange(event: PageEvent): void {
    this.loadPage(event.pageIndex);
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

  protected canCancel(row: LeaveRequestSummary): boolean {
    if (row.leaveStatus === 'PENDING') {
      return true;
    }
    if (row.leaveStatus === 'APPROVED') {
      const today = toIsoDate(new Date());
      return row.startDate > today;
    }
    return false;
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

  protected cancelRequest(row: LeaveRequestSummary): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          title: 'Cancel Leave Request',
          message: `Are you sure you want to cancel your ${row.leaveTypeName} leave (${row.totalDays} day(s))?`,
          confirmLabel: 'Cancel Leave',
          destructive: true,
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;

        this.cancellingId.set(row.id);
        this.leaveService.cancelLeave(row.id).subscribe({
          next: () => {
            this.notification.success('Leave request cancelled successfully');
            this.cancellingId.set(null);
            this.loadPage(this.currentPage());
          },
          error: (error: unknown) => {
            this.notification.error(getErrorMessage(error));
            this.cancellingId.set(null);
          },
        });
      });
  }
}
