import { Component, signal, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SecurityService, SecuritySettings as SecuritySettingsModel } from '../security.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-security-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './settings.html',
  styles: [`:host { display: block; }`]
})
export class SecuritySettings implements OnInit, OnDestroy {
  private securityService = inject(SecurityService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  isLoading = signal(true);
  isSaving = signal(false);

  // Password policy
  minLength = signal(8);
  requireUppercase = signal(true);
  requireNumbers = signal(true);
  requireSpecialChars = signal(false);
  expiryDays = signal(90);

  // Session config
  timeoutMinutes = signal(30);
  maxConcurrentSessions = signal(3);

  // Login config
  maxAttempts = signal(5);
  lockoutDurationMinutes = signal(15);

  // IP whitelist
  ipWhitelist = signal<string[]>([]);
  newIp = signal('');

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Password Policy', content: 'Configure minimum password requirements to ensure account security. Require uppercase, numbers, and special characters.' },
    { title: 'Session Management', content: 'Set session timeout and concurrent session limits. Shorter timeouts improve security but may inconvenience users.' },
    { title: 'Login Protection', content: 'Configure maximum login attempts before account lockout. This prevents brute-force attacks.' },
    { title: 'IP Whitelist', content: 'Restrict admin access to specific IP addresses. Leave empty to allow all IPs.' }
  ];

  ngOnInit() {
    this.securityService.getSecuritySettings().pipe(takeUntil(this.destroy$)).subscribe(settings => {
      this.minLength.set(settings.passwordPolicy.minLength);
      this.requireUppercase.set(settings.passwordPolicy.requireUppercase);
      this.requireNumbers.set(settings.passwordPolicy.requireNumbers);
      this.requireSpecialChars.set(settings.passwordPolicy.requireSpecialChars);
      this.expiryDays.set(settings.passwordPolicy.expiryDays);
      this.timeoutMinutes.set(settings.sessionConfig.timeoutMinutes);
      this.maxConcurrentSessions.set(settings.sessionConfig.maxConcurrentSessions);
      this.maxAttempts.set(settings.loginConfig.maxAttempts);
      this.lockoutDurationMinutes.set(settings.loginConfig.lockoutDurationMinutes);
      this.ipWhitelist.set(settings.ipWhitelist);
      this.isLoading.set(false);
    });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  addIp() {
    const ip = this.newIp().trim();
    if (ip && !this.ipWhitelist().includes(ip)) {
      this.ipWhitelist.update(list => [...list, ip]);
      this.newIp.set('');
    }
  }

  removeIp(ip: string) {
    this.ipWhitelist.update(list => list.filter(i => i !== ip));
  }

  save() {
    this.isSaving.set(true);
    const settings: SecuritySettingsModel = {
      passwordPolicy: {
        minLength: this.minLength(),
        requireUppercase: this.requireUppercase(),
        requireNumbers: this.requireNumbers(),
        requireSpecialChars: this.requireSpecialChars(),
        expiryDays: this.expiryDays()
      },
      sessionConfig: {
        timeoutMinutes: this.timeoutMinutes(),
        maxConcurrentSessions: this.maxConcurrentSessions()
      },
      loginConfig: {
        maxAttempts: this.maxAttempts(),
        lockoutDurationMinutes: this.lockoutDurationMinutes()
      },
      ipWhitelist: this.ipWhitelist()
    };

    this.securityService.updateSecuritySettings(settings).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.alertSvc.success('Settings Saved', 'Security settings updated successfully');
      },
      error: () => {
        this.isSaving.set(false);
        this.alertSvc.error('Failed', 'Could not save security settings');
      }
    });
  }
}
