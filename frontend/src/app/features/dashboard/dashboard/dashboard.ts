import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, KeyValuePipe, TitleCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { LeaveService } from '../../../core/services/leave.service';
import { PublicHolidayService } from '../../../core/services/public-holiday.service';
import { NotificationService } from '../../../core/services/notification.service';
import { getErrorMessage } from '../../../core/utils/error-message';
import { toIsoDate } from '../../../core/utils/date';
import { AdminDashboard, LeaveBalance, ManagerDashboard } from '../../../core/models/leave.model';
import { PublicHoliday } from '../../../core/models/public-holiday.model';
import { BalanceRing } from '../../../shared/balance-ring/balance-ring';
import { ConfirmDialog } from '../../../shared/confirm-dialog/confirm-dialog';
import { TeamMembersDialog } from '../../../shared/team-members-dialog/team-members-dialog';

@Component({
  selector: 'app-dashboard',
  imports: [
    DatePipe,
    KeyValuePipe,
    TitleCasePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    BalanceRing,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly leaveService = inject(LeaveService);
  private readonly holidayService = inject(PublicHolidayService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  protected readonly loading = signal(true);
  protected readonly generatingBalances = signal(false);

  protected readonly balances = signal<LeaveBalance[]>([]);
  protected readonly upcomingHolidays = signal<PublicHoliday[]>([]);

  protected readonly adminData = signal<AdminDashboard | null>(null);

  protected readonly managerData = signal<ManagerDashboard | null>(null);

  // Load dashboard data based on the Logged-in user's role
  ngOnInit(): void {
    const role = this.authService.currentUser()?.role;

    if (role === 'ADMIN') {
      this.loadAdminData();
    } else if (role === 'MANAGER') {
      this.loadManagerData();
    } else {
      this.loadEmployeeData();
    }
  }

  private filterUpcomingHolidays(holidays: PublicHoliday[]): void {
    const today = toIsoDate(new Date());
    const upcoming = holidays.filter((h) => h.holidayDate >= today).slice(0, 4);
    this.upcomingHolidays.set(upcoming.length > 0 ? upcoming : holidays.slice(-4));
  }

  private loadAdminData(): void {
    forkJoin({
      admin: this.leaveService.getAdminDashboard(),
      holidays: this.holidayService.getHolidays(new Date().getFullYear()),
    }).subscribe({
      next: ({ admin, holidays }) => {
        this.adminData.set(admin);
        this.filterUpcomingHolidays(holidays);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.notification.error(getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  private loadManagerData(): void {
    forkJoin({
      manager: this.leaveService.getManagerDashboard(),
      balances: this.leaveService.getMyBalances(),
      holidays: this.holidayService.getHolidays(new Date().getFullYear()),
    }).subscribe({
      next: ({ manager, balances, holidays }) => {
        this.managerData.set(manager);
        this.balances.set(balances);
        this.filterUpcomingHolidays(holidays);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.notification.error(getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  private loadEmployeeData(): void {
    forkJoin({
      balances: this.leaveService.getMyBalances(),
      holidays: this.holidayService.getHolidays(new Date().getFullYear()),
    }).subscribe({
      next: ({ balances, holidays }) => {
        this.balances.set(balances);
        this.filterUpcomingHolidays(holidays);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.notification.error(getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  protected openTeamMembers(): void {
    this.dialog.open(TeamMembersDialog, {
      width: '540px',
      maxWidth: '92vw',
    });
  }

  protected generateBalances(): void {
    const currentYear = new Date().getFullYear();
    this.dialog
      .open(ConfirmDialog, {
        data: {
          title: `Generate Yearly Balances (${currentYear})`,
          message: `Are you sure you want to initialize standard yearly leave balances for all active staff for ${currentYear}? Existing balances will not be overwritten.`,
          confirmLabel: 'Generate Balances',
          destructive: false,
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;

        this.generatingBalances.set(true);
        this.leaveService.generateYearlyBalances(currentYear).subscribe({
          next: (res) => {
            this.notification.success(
              res.created > 0
                ? `Created ${res.created} leave balance records for ${currentYear}.`
                : `All active staff already have balances for ${currentYear}.`,
            );
            this.generatingBalances.set(false);
          },
          error: (err: unknown) => {
            this.notification.error(getErrorMessage(err));
            this.generatingBalances.set(false);
          },
        });
      });
  }
}
