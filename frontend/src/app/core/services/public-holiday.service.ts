import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PublicHoliday, PublicHolidayRequest } from '../models/public-holiday.model';

@Injectable({ providedIn: 'root' })
export class PublicHolidayService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/public-holidays`;

  //Fetch public holidays, optionally filtered by the year
  getHolidays(year?: number): Observable<PublicHoliday[]> {
    let params = new HttpParams();
    if (year !== undefined && year !== null) {
      params = params.set('year', year.toString());
    }
    return this.http.get<PublicHoliday[]>(this.baseUrl, { params });
  }

  // Add a new public holiday
  addHoliday(request: PublicHolidayRequest): Observable<PublicHoliday> {
    return this.http.post<PublicHoliday>(this.baseUrl, request);
  }

  //Delete a public holiday using it ID
  deleteHoliday(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

