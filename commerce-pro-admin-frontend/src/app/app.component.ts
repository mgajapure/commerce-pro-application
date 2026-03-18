import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';

import { AuthService } from './core/services/auth/auth.service';
import { Header } from './shared/layout/header/header';
import { Sidebar } from './shared/layout/sidebar/sidebar';
import { AlertComponent } from './shared/components/alert/alert';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, Sidebar, Header, AlertComponent],
  template: `
    @if (isAuthRoute()) {
      <router-outlet></router-outlet>
    } @else {
      <div class="flex h-screen overflow-hidden bg-gray-50/50">
        <app-sidebar
          [isOpen]="sidebarOpen()"
          (close)="closeSidebar()"
          (collapseChange)="sidebarCollapsed.set($event)">
        </app-sidebar>

        <div class="flex-1 flex flex-col min-w-0 overflow-hidden transition-all duration-300 ease-in-out">
          <app-header
            (toggleSidebar)="toggleSidebar()"
            [sidebarOpen]="sidebarOpen()"
            [sidebarCollapsed]="sidebarCollapsed()">
          </app-header>

          <main class="flex-1 overflow-y-auto overflow-x-hidden">
            <div class="mx-auto">
              <router-outlet></router-outlet>
            </div>
          </main>
        </div>
      </div>
    }

    <!-- Global alert layer — toasts + confirm dialog rendered here once for the whole app -->
    <app-alert></app-alert>
  `
})
export class AppComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly sidebarOpen = signal(false);
  readonly sidebarCollapsed = signal(false);
  readonly currentUrl = signal(this.router.url);
  readonly isAuthRoute = computed(() => this.currentUrl().startsWith('/auth'));

  constructor() {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.currentUrl.set(event.urlAfterRedirects);
      }
    });

    if (!this.authService.isAuthenticated() && !this.router.url.startsWith('/auth')) {
      this.router.navigate(['/auth/login']);
    }
  }

  toggleSidebar(): void {
    const isMobile = window.innerWidth < 768;
    if (isMobile) {
      this.sidebarOpen.update(value => !value);
      return;
    }

    this.sidebarCollapsed.update(value => !value);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }
}
