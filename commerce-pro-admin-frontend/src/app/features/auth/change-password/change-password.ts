import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './change-password.html',
  styleUrl: './change-password.scss'
})
export class ChangePassword {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentPassword = signal('');
  readonly newPassword = signal('');
  readonly confirmPassword = signal('');
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  submit(): void {
    if (this.newPassword() !== this.confirmPassword()) {
      this.errorMessage.set('New passwords do not match.');
      return;
    }

    if (this.newPassword().length < 8) {
      this.errorMessage.set('New password must be at least 8 characters.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.authService.changePassword({
      currentPassword: this.currentPassword(),
      newPassword: this.newPassword()
    }).subscribe(success => {
      this.isLoading.set(false);

      if (!success) {
        this.errorMessage.set('Failed to change password. Check your current password and try again.');
        return;
      }

      this.successMessage.set('Password changed successfully.');
      this.currentPassword.set('');
      this.newPassword.set('');
      this.confirmPassword.set('');

      setTimeout(() => this.router.navigate(['/dashboard']), 1500);
    });
  }

  cancel(): void {
    this.router.navigate(['/dashboard']);
  }
}
