import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { MfaSetupResponse } from '../../../core/models/auth';
import { AuthService } from '../../../core/services/auth/auth.service';

type SecurityView = 'overview' | 'change-password' | 'mfa-setup' | 'mfa-disable';

@Component({
  selector: 'app-account-security',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './security.html',
  styleUrl: './security.scss'
})
export class AccountSecurity implements OnInit {
  private readonly authService = inject(AuthService);

  readonly session = this.authService.session;
  readonly isMfaEnabled = computed(() => {
    const authorities = this.session()?.authorities ?? [];
    return authorities.includes('mfa:enabled') || false;
  });

  readonly activeView = signal<SecurityView>('overview');
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  // Change password
  readonly currentPassword = signal('');
  readonly newPassword = signal('');
  readonly confirmPassword = signal('');

  // MFA setup
  readonly mfaSetupData = signal<MfaSetupResponse | null>(null);
  readonly mfaVerifyCode = signal('');

  // MFA disable
  readonly mfaDisablePassword = signal('');
  readonly mfaDisableCode = signal('');

  // Track MFA state locally after successful operations
  readonly mfaEnabledLocally = signal(false);

  readonly encodeURIComponent = encodeURIComponent;

  ngOnInit(): void {
    // No initial loading needed — data comes from session
  }

  showView(view: SecurityView): void {
    this.activeView.set(view);
    this.clearMessages();
  }

  changePassword(): void {
    if (this.newPassword() !== this.confirmPassword()) {
      this.errorMessage.set('New passwords do not match.');
      return;
    }
    if (this.newPassword().length < 8) {
      this.errorMessage.set('New password must be at least 8 characters.');
      return;
    }

    this.isLoading.set(true);
    this.clearMessages();

    this.authService.changePassword({
      currentPassword: this.currentPassword(),
      newPassword: this.newPassword()
    }).subscribe(success => {
      this.isLoading.set(false);
      if (!success) {
        this.errorMessage.set('Failed to change password. Ensure your current password is correct.');
        return;
      }
      this.successMessage.set('Password changed successfully.');
      this.currentPassword.set('');
      this.newPassword.set('');
      this.confirmPassword.set('');
      setTimeout(() => this.activeView.set('overview'), 1500);
    });
  }

  initMfaSetup(): void {
    this.isLoading.set(true);
    this.clearMessages();
    this.mfaSetupData.set(null);

    this.authService.setupMfa().subscribe(data => {
      this.isLoading.set(false);
      if (!data) {
        this.errorMessage.set('Failed to initialize MFA setup. Try again.');
        return;
      }
      this.mfaSetupData.set(data);
      this.activeView.set('mfa-setup');
    });
  }

  enableMfa(): void {
    if (!this.mfaVerifyCode().trim()) {
      this.errorMessage.set('Enter the 6-digit code from your authenticator app.');
      return;
    }

    this.isLoading.set(true);
    this.clearMessages();

    this.authService.enableMfa({ code: this.mfaVerifyCode().trim() }).subscribe(success => {
      this.isLoading.set(false);
      if (!success) {
        this.errorMessage.set('Invalid code. Verify the code in your authenticator app and try again.');
        return;
      }
      this.mfaEnabledLocally.set(true);
      this.mfaVerifyCode.set('');
      this.mfaSetupData.set(null);
      this.successMessage.set('MFA enabled successfully. Your account is now protected with two-factor authentication.');
      setTimeout(() => this.activeView.set('overview'), 2000);
    });
  }

  disableMfa(): void {
    if (!this.mfaDisablePassword().trim() || !this.mfaDisableCode().trim()) {
      this.errorMessage.set('Enter your password and the 6-digit MFA code to disable MFA.');
      return;
    }

    this.isLoading.set(true);
    this.clearMessages();

    this.authService.disableMfa({
      password: this.mfaDisablePassword(),
      code: this.mfaDisableCode().trim()
    }).subscribe(success => {
      this.isLoading.set(false);
      if (!success) {
        this.errorMessage.set('Failed to disable MFA. Check your password and MFA code.');
        return;
      }
      this.mfaEnabledLocally.set(false);
      this.mfaDisablePassword.set('');
      this.mfaDisableCode.set('');
      this.successMessage.set('MFA disabled successfully.');
      setTimeout(() => this.activeView.set('overview'), 1500);
    });
  }

  private clearMessages(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
  }
}
