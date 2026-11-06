import { Component, computed, inject, signal } from '@angular/core';
import { BreakpointObserver } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd, Router } from '@angular/router';
import { map, filter } from 'rxjs';
import { ROLE_LABELS } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { NAV_ITEMS } from '../navigation';
import { MatDividerModule } from '@angular/material/divider';

const MOBILE_QUERY = '(max-width: 900px)';
const MAX_INITIALS = 2;

@Component({
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule
  ],
  selector: 'app-shell',
  styleUrl: './shell.scss',
  templateUrl: './shell.html',
})
export class Shell {
  private readonly authService = inject(AuthService);
  private readonly breakpointObserver = inject(BreakpointObserver);

  protected readonly user = this.authService.currentUser;

  protected readonly navItems = computed(() => NAV_ITEMS.filter((item) => this.authService.hasRole(...item.roles)),);

    private readonly router = inject(Router);

  protected readonly pageTitle = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(() => this.resolvePageTitle()),
    ),
    { initialValue: this.resolvePageTitle() },
  );

  private resolvePageTitle(): string {
    return NAV_ITEMS.find((item) => this.router.url.startsWith(item.path))?.label ?? '';
  }

  protected readonly roleLabel = computed(() => {
    const role = this.user()?.role;
    return role ? ROLE_LABELS[role] : '';
  })

  protected readonly initials = computed(() => {
    const name = this.user()?.fullName ?? '';
    return name.split(' ')
    .filter(Boolean)
    .slice(0, MAX_INITIALS)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
  });

  protected readonly isMobile = toSignal(
    this.breakpointObserver.observe(MOBILE_QUERY).pipe(map((state) => state.matches)),
    { initialValue: this.breakpointObserver.isMatched(MOBILE_QUERY)},
  );

  protected readonly collapsed = signal(false);
  protected readonly mobileOpen = signal(false);

  protected toggleSidebar(): void {
    if (this.isMobile()) {
      this.mobileOpen.update((open) => !open);
    } else {
      this.collapsed.update((isCollapsed) => !isCollapsed);
    }
  }

  protected closeMobileSidebar(): void {
    this.mobileOpen.set(false);
  }

  protected logout():void {
    this.authService.logout();
  }

}
