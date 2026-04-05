import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SecurityService, MfaPolicy } from '../security.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-two-factor-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './two-factor.html',
  styles: [`:host { display: block; }`]
})
export class TwoFactorAdmin implements OnInit, OnDestroy {
  private securityService = inject(SecurityService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  isLoading = signal(true);
  isSaving = signal(false);

  requireForAllAdmins = signal(false);
  gracePeriodDays = signal(14);
  allowTotp = signal(true);
  allowSms = signal(true);
  allowEmail = signal(true);
  totalUsers = signal(0);
  mfaEnabledUsers = signal(0);

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'MFA Enforcement', content: 'When enabled, all admin users must set up multi-factor authentication. A grace period gives users time to comply.' },
    { title: 'TOTP', content: 'Time-based One-Time Password apps like Google Authenticator or Authy. Most secure option.' },
    { title: 'SMS', content: 'One-time codes sent via text message. Convenient but less secure than TOTP.' },
    { title: 'Email', content: 'One-time codes sent to the user\'s email. Good fallback option.' }
  ];

  mfaPercentage = computed(() => {
    const total = this.totalUsers();
    if (total === 0) return 0;
    return Math.round((this.mfaEnabledUsers() / total) * 100);
  });

  ngOnInit() {
    this.securityService.getMfaPolicy().pipe(takeUntil(this.destroy$)).subscribe(policy => {
      this.requireForAllAdmins.set(policy.requireForAllAdmins);
      this.gracePeriodDays.set(policy.gracePeriodDays);
      this.allowTotp.set(policy.allowedMethods.totp);
      this.allowSms.set(policy.allowedMethods.sms);
      this.allowEmail.set(policy.allowedMethods.email);
      this.totalUsers.set(policy.stats.totalUsers);
      this.mfaEnabledUsers.set(policy.stats.mfaEnabledUsers);
      this.isLoading.set(false);
    });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  save() {
    this.isSaving.set(true);
    const policy: MfaPolicy = {
      requireForAllAdmins: this.requireForAllAdmins(),
      gracePeriodDays: this.gracePeriodDays(),
      allowedMethods: { totp: this.allowTotp(), sms: this.allowSms(), email: this.allowEmail() },
      stats: { totalUsers: this.totalUsers(), mfaEnabledUsers: this.mfaEnabledUsers() }
    };

    this.securityService.updateMfaPolicy(policy).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.isSaving.set(false); this.alertSvc.success('MFA Policy Updated'); },
      error: () => { this.isSaving.set(false); this.alertSvc.error('Failed', 'Could not update MFA policy'); }
    });
  }
}
