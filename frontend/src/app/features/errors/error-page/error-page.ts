import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink, MatButtonModule],
  selector: 'app-error-page',
  styleUrl: './error-page.scss',
  templateUrl: './error-page.html',
})
export class ErrorPage {

  private readonly route = inject(ActivatedRoute);

  protected readonly code = this.route.snapshot.data['code'] as string;
  protected readonly title = this.route.snapshot.data['title'] as string;
  protected readonly message = this.route.snapshot.data['message'] as string;
}
