import { Component, input } from '@angular/core';
import { LeaveStatus } from '../../core/models/leave.model';

@Component({
  imports: [],
  selector: 'app-status-badge',
  styleUrl: './status-badge.scss',
  templateUrl: './status-badge.html',
})
export class StatusBadge {
  readonly status = input.required<LeaveStatus>();
}
