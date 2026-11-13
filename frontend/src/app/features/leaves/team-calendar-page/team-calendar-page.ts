import { Component } from '@angular/core';
import { TeamCalendar } from '../../../shared/team-calendar/team-calendar';

@Component({
  selector: 'app-team-calendar-page',
  imports: [TeamCalendar],
  templateUrl: './team-calendar-page.html',
  styleUrl: './team-calendar-page.scss',
})
export class TeamCalendarPage {}

