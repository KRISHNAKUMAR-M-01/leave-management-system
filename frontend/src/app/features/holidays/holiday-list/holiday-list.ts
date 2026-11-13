import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { PublicHolidayService } from '../../../core/services/public-holiday.service';
import { getErrorMessage } from '../../../core/utils/error-message';
import { parseLocalDate } from '../../../core/utils/date';
import { PublicHoliday, PublicHolidayRequest } from '../../../core/models/public-holiday.model';
import { HolidayFormDialog } from '../../../shared/holiday-form-dialog/holiday-form-dialog';
import { ConfirmDialog } from '../../../shared/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-holiday-list',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './holiday-list.html',
  styleUrl: './holiday-list.scss',
})
export class HolidayList implements OnInit {
  private readonly holidayService = inject(PublicHolidayService);
  private readonly notification = inject(NotificationService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  protected readonly isAdmin = computed(() => this.authService.hasRole('ADMIN'));
  protected readonly selectedYear = signal(new Date().getFullYear());
  protected readonly holidays = signal<PublicHoliday[]>([]);
  protected readonly loading = signal(true);

  // Generate a range of years: [currentYear - 1, currentYear, currentYear + 1]
  protected readonly availableYears = computed(() => {
    const current = new Date().getFullYear();
    return [current - 1, current, current + 1];
  });

  protected readonly displayedColumns = computed(() => {
    return this.isAdmin()
      ? ['holidayDate', 'dayOfWeek', 'holidayName', 'actions']
      : ['holidayDate', 'dayOfWeek', 'holidayName'];
  });

  // Load public holidays when the component initializes.
  ngOnInit(): void {
    this.loadHolidays();
  }

  // Update the selected year and reload the holidays.
  protected onYearChange(year: number): void {
    this.selectedYear.set(year);
    this.loadHolidays();
  }

  // Fetch public holidays for the selected year.
  protected loadHolidays(): void {
    this.loading.set(true);
    this.holidayService.getHolidays(this.selectedYear()).subscribe({
      next: (data) => {
        this.holidays.set(data);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.notification.error(getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  // Open the dialog to add a new public holiday.
  protected openAddDialog(): void {
    this.dialog
      .open(HolidayFormDialog, { width: '420px' })
      .afterClosed()
      .subscribe((result?: PublicHolidayRequest) => {
        if (!result) return;

        this.holidayService.addHoliday(result).subscribe({
          next: (created) => {
            this.notification.success(`Holiday "${created.holidayName}" added`);
            const holidayYear = parseLocalDate(created.holidayDate).getFullYear();
            if (holidayYear !== this.selectedYear()) {
              this.selectedYear.set(holidayYear);
            }
            this.loadHolidays();
          },
          error: (err: unknown) => {
            this.notification.error(getErrorMessage(err));
          },
        });
      });
  }

  // Delete the selected public holiday after confirmation.
  protected deleteHoliday(holiday: PublicHoliday): void {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          title: 'Delete Holiday',
          message: `Are you sure you want to delete "${holiday.holidayName}" on ${holiday.holidayDate}?`,
          confirmLabel: 'Delete',
          destructive: true,
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;

        this.holidayService.deleteHoliday(holiday.id).subscribe({
          next: () => {
            this.notification.success(`Holiday "${holiday.holidayName}" deleted`);
            this.loadHolidays();
          },
          error: (err: unknown) => {
            this.notification.error(getErrorMessage(err));
          },
        });
      });
  }
}

