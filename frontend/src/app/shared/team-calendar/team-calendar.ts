import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';

import { LeaveService } from '../../core/services/leave.service';
import { PublicHolidayService } from '../../core/services/public-holiday.service';
import { LeaveRequestSummary, LeaveStatus } from '../../core/models/leave.model';
import { PublicHoliday } from '../../core/models/public-holiday.model';
import { toIsoDate } from '../../core/utils/date';

export interface CalendarDay {
  date: Date;
  dateStr: string;
  dayNumber: number;
  isCurrentMonth: boolean;
  isToday: boolean;
  isWeekend: boolean;
  holiday?: PublicHoliday;
  leaves: LeaveRequestSummary[];
}

@Component({
  selector: 'app-team-calendar',
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './team-calendar.html',
  styleUrl: './team-calendar.scss',
})
export class TeamCalendar implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly holidayService = inject(PublicHolidayService);

  protected readonly loading = signal(false);
  protected readonly currentDate = signal(new Date());
  protected readonly selectedDate = signal<string>(toIsoDate(new Date()));
  protected readonly statusFilter = signal<'ALL' | 'APPROVED' | 'PENDING'>('ALL');

  protected readonly teamLeaves = signal<LeaveRequestSummary[]>([]);
  protected readonly holidays = signal<PublicHoliday[]>([]);

  protected readonly weekDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  protected readonly monthYearLabel = computed(() => {
    return this.currentDate().toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  });

  protected readonly filteredLeaves = computed(() => {
    const filter = this.statusFilter();
    const leaves = this.teamLeaves();
    if (filter === 'ALL') return leaves;
    return leaves.filter((l) => l.leaveStatus === filter);
  });

  // Calendar grid cells for the current month view (including leading and trailing days)
  protected readonly calendarDays = computed<CalendarDay[]>(() => {
    const curr = this.currentDate();
    const year = curr.getFullYear();
    const month = curr.getMonth();

    const todayStr = toIsoDate(new Date());
    const holidaysMap = new Map<string, PublicHoliday>();
    for (const h of this.holidays()) {
      holidaysMap.set(h.holidayDate, h);
    }

    const leaves = this.filteredLeaves();

    const firstDayOfMonth = new Date(year, month, 1);

    const lastDayOfMonth = new Date(year, month + 1, 0);

    const startDayOfWeek = (firstDayOfMonth.getDay() + 6) % 7;

    const days: CalendarDay[] = [];

    // Leading days from previous month
    for (let i = startDayOfWeek; i > 0; i--) {
      const d = new Date(year, month, 1 - i);
      const dStr = toIsoDate(d);
      days.push({
        date: d,
        dateStr: dStr,
        dayNumber: d.getDate(),
        isCurrentMonth: false,
        isToday: dStr === todayStr,
        isWeekend: d.getDay() === 0 || d.getDay() === 6,
        holiday: holidaysMap.get(dStr),
        leaves: leaves.filter((l) => l.startDate <= dStr && l.endDate >= dStr),
      });
    }

    // Days of current month
    for (let day = 1; day <= lastDayOfMonth.getDate(); day++) {
      const d = new Date(year, month, day);
      const dStr = toIsoDate(d);
      days.push({
        date: d,
        dateStr: dStr,
        dayNumber: day,
        isCurrentMonth: true,
        isToday: dStr === todayStr,
        isWeekend: d.getDay() === 0 || d.getDay() === 6,
        holiday: holidaysMap.get(dStr),
        leaves: leaves.filter((l) => l.startDate <= dStr && l.endDate >= dStr),
      });
    }

    // Trailing days from next month to complete the last week (multiple of 7)
    const remainingDays = (7 - (days.length % 7)) % 7;
    for (let i = 1; i <= remainingDays; i++) {
      const d = new Date(year, month + 1, i);
      const dStr = toIsoDate(d);
      days.push({
        date: d,
        dateStr: dStr,
        dayNumber: d.getDate(),
        isCurrentMonth: false,
        isToday: dStr === todayStr,
        isWeekend: d.getDay() === 0 || d.getDay() === 6,
        holiday: holidaysMap.get(dStr),
        leaves: leaves.filter((l) => l.startDate <= dStr && l.endDate >= dStr),
      });
    }

    return days;
  });

  /** Selected day data for the detail card */
  protected readonly selectedDayData = computed(() => {
    const sel = this.selectedDate();
    const days = this.calendarDays();
    return days.find((d) => d.dateStr === sel) || null;
  });

  /** Count of team members on leave this calendar month */
  protected readonly currentMonthLeaveCount = computed(() => {
    const leaves = this.teamLeaves();
    const curr = this.currentDate();
    const year = curr.getFullYear();
    const month = curr.getMonth();
    const firstStr = toIsoDate(new Date(year, month, 1));
    const lastStr = toIsoDate(new Date(year, month + 1, 0));

    return leaves.filter((l) => l.startDate <= lastStr && l.endDate >= firstStr).length;
  });

  ngOnInit(): void {
    this.loadCalendarData();
  }

  protected prevMonth(): void {
    const c = this.currentDate();
    this.currentDate.set(new Date(c.getFullYear(), c.getMonth() - 1, 1));
    this.loadCalendarData();
  }

  protected nextMonth(): void {
    const c = this.currentDate();
    this.currentDate.set(new Date(c.getFullYear(), c.getMonth() + 1, 1));
    this.loadCalendarData();
  }

  protected goToToday(): void {
    const today = new Date();
    this.currentDate.set(new Date(today.getFullYear(), today.getMonth(), 1));
    this.selectedDate.set(toIsoDate(today));
    this.loadCalendarData();
  }

  protected selectDate(dateStr: string): void {
    this.selectedDate.set(dateStr);
  }

  protected setFilter(filter: 'ALL' | 'APPROVED' | 'PENDING'): void {
    this.statusFilter.set(filter);
  }

  private loadCalendarData(): void {
    const curr = this.currentDate();
    const year = curr.getFullYear();
    const month = curr.getMonth();

    // Query from first day of month - 7 days to last day + 7 days to cover leading/trailing grid
    const fromDate = new Date(year, month, -6);
    const toDate = new Date(year, month + 1, 7);

    const fromStr = toIsoDate(fromDate);
    const toStr = toIsoDate(toDate);

    this.loading.set(true);
    forkJoin({
      leaves: this.leaveService.getTeamCalendar(fromStr, toStr),
      holidays: this.holidayService.getHolidays(year),
    }).subscribe({
      next: ({ leaves, holidays }) => {
        this.teamLeaves.set(leaves);
        this.holidays.set(holidays);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }
}

