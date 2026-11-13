import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LeaveRequestSummary } from '../../../core/models/leave.model';
import { LeaveService } from '../../../core/services/leave.service';
import { NotificationService } from '../../../core/services/notification.service';
import { getErrorMessage } from '../../../core/utils/error-message';
import { ConfirmDialog } from '../../../shared/confirm-dialog/confirm-dialog';
import { RejectDialog } from '../../../shared/reject-dialog/reject-dialog';

@Component({
  selector: 'app-pending-approvals',
  imports: [
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './pending-approvals.html',
  styleUrl: './pending-approvals.scss',
})
export class PendingApprovals implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  protected readonly requests = signal<LeaveRequestSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly processingId = signal<number | null>(null);

  protected readonly displayedColumns = [
    'employeeName',
    'leaveType',
    'dates',
    'days',
    'reason',
    'actions',
  ];

  ngOnInit(): void {
    this.loadRequests();
  }

  protected loadRequests(): void {
    this.loading.set(true);
    this.leaveService.getPendingRequests().subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.notification.error(getErrorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected approve(request: LeaveRequestSummary): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          title: 'Approve leave request',
          message: `Approve leave for ${request.employeeName} (${request.totalDays} day(s))?`,
          confirmLabel: 'Approve',
          destructive: false,
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;

        this.processingId.set(request.id);
        this.leaveService.approveLeave(request.id).subscribe({
          next: () => {
            this.notification.success(`Leave request approved for ${request.employeeName}`);
            this.processingId.set(null);
            this.loadRequests();
          },
          error: (error: unknown) => {
            this.notification.error(getErrorMessage(error));
            this.processingId.set(null);
          },
        });
      });
  }

  protected reject(request: LeaveRequestSummary): void {
    this.dialog
      .open(RejectDialog, {
        data: {
          title: 'Reject Leave Request',
          employeeName: request.employeeName,
        },
      })
      .afterClosed()
      .subscribe((comment: string | undefined) => {
        if (!comment) return;

        this.processingId.set(request.id);
        this.leaveService.rejectLeave(request.id, comment).subscribe({
          next: () => {
            this.notification.success(`Leave request rejected for ${request.employeeName}`);
            this.processingId.set(null);
            this.loadRequests();
          },
          error: (error: unknown) => {
            this.notification.error(getErrorMessage(error));
            this.processingId.set(null);
          },
        });
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
