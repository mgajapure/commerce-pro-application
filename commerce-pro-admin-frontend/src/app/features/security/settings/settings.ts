import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SecurityService, SecuritySettings as SecuritySettingsModel } from '../security.service';

@Component({
  selector: 'app-security-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.html',
  styles: [`:host { display: block; }`]
})
export class SecuritySettings implements OnInit {
  private securityService = inject(SecurityService);

  isLoading = signal(true);
  isSaving = signal(false);
  successMessage = signal('');
  errorMessage = signal('');

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

  ngOnInit() {
    this.securityService.getSecuritySettings().subscribe(settings => {
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
    this.successMessage.set('');
    this.errorMessage.set('');

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

    this.securityService.updateSecuritySettings(settings).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.successMessage.set('Security settings saved successfully.');
      },
      error: () => {
        this.isSaving.set(false);
        this.errorMessage.set('Failed to save security settings. Please try again.');
      }
    });
  }
}
