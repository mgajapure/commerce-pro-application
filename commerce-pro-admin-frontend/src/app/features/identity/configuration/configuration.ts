import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';

import { SuperAdminConfigView } from '../../../core/models/identity';
import { IdentityService } from '../../../core/services/identity/identity.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';

@Component({
  selector: 'app-identity-configuration',
  standalone: true,
  imports: [CommonModule, HelpSidebar],
  templateUrl: './configuration.html',
  styles: [`:host { display: block; }`]
})
export class IdentityConfiguration implements OnInit, OnDestroy {
  private readonly svc = inject(IdentityService);
  private readonly alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  config = signal<SuperAdminConfigView | null>(null);
  loading = signal(true);
  reloading = signal(false);

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Security Policy', content: 'Controls MFA requirements, session timeouts, and concurrent session limits for the identity system.' },
    { title: 'Configuration Management', content: 'Controls system-level capabilities like deleting system roles, modifying own roles, and minimum super admin count.' },
    { title: 'Reload Configuration', content: 'Reloads the configuration from the backend. Use this after making changes to the server-side configuration.' }
  ];

  ngOnInit() { this.loadConfiguration(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  loadConfiguration() {
    this.loading.set(true);
    this.svc.getSuperAdminConfiguration().pipe(takeUntil(this.destroy$)).subscribe(value => {
      this.config.set(value);
      this.loading.set(false);
    });
  }

  reloadConfiguration() {
    this.reloading.set(true);
    this.svc.reloadConfiguration().pipe(takeUntil(this.destroy$)).subscribe(success => {
      this.reloading.set(false);
      if (success) {
        this.alertSvc.success('Configuration Reloaded', 'Identity configuration refreshed from server');
        this.loadConfiguration();
      } else {
        this.alertSvc.error('Failed', 'Could not reload configuration');
      }
    });
  }
}
