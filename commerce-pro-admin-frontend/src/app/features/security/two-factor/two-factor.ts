import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SecurityService, MfaPolicy } from '../security.service';

@Component({
  selector: 'app-two-factor-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './two-factor.html',
  styles: [`:host { display: block; }`]
})
export class TwoFactorAdmin implements OnInit {
  private securityService = inject(SecurityService);

  isLoading = signal(true);
  isSaving = signal(false);
  successMessage = signal('');
  errorMessage = signal('');

  requireForAllAdmins = signal(false);
  gracePeriodDays = signal(14);
  allowTotp = signal(true);
  allowSms = signal(true);
  allowEmail = signal(true);
  totalUsers = signal(0);
  mfaEnabledUsers = signal(0);

  mfaPercentage = computed(() => {
    const total = this.totalUsers();
    if (total === 0) return 0;
    return Math.round((this.mfaEnabledUsers() / total) * 100);
  });

  ngOnInit() {
    this.securityService.getMfaPolicy().subscribe(policy => {
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

  save() {
    this.isSaving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const policy: MfaPolicy = {
      requireForAllAdmins: this.requireForAllAdmins(),
      gracePeriodDays: this.gracePeriodDays(),
      allowedMethods: {
        totp: this.allowTotp(),
        sms: this.allowSms(),
        email: this.allowEmail()
      },
      stats: {
        totalUsers: this.totalUsers(),
        mfaEnabledUsers: this.mfaEnabledUsers()
      }
    };

    this.securityService.updateMfaPolicy(policy).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.successMessage.set('MFA policy updated successfully.');
      },
      error: () => {
        this.isSaving.set(false);
        this.errorMessage.set('Failed to update MFA policy. Please try again.');
      }
    });
  }
}
