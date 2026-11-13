import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { LeaveService } from '../../core/services/leave.service';
import { Employee } from '../../core/models/employee.model';
import { NotificationService } from '../../core/services/notification.service';
import { getErrorMessage } from '../../core/utils/error-message';

@Component({
  selector: 'app-team-members-dialog',
  imports: [
    CommonModule,
    DatePipe,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './team-members-dialog.html',
  styleUrl: './team-members-dialog.scss',
})
export class TeamMembersDialog implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly notification = inject(NotificationService);
  protected readonly dialogRef = inject(MatDialogRef<TeamMembersDialog>);

  protected readonly loading = signal(true);
  protected readonly teamMembers = signal<Employee[]>([]);

  ngOnInit(): void {
    this.leaveService.getTeamMembers().subscribe({
      next: (members) => {
        this.teamMembers.set(members);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.notification.error(getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }
}

