import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly username = signal('');
  readonly password = signal('');
  readonly mfaCode = signal('');
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly year = new Date().getFullYear();

  submit(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.authService.login({
      username: this.username().trim(),
      password: this.password(),
      mfaCode: this.mfaCode().trim() || undefined
    }).subscribe(success => {
      this.isLoading.set(false);

      if (!success) {
        this.errorMessage.set('Invalid credentials. Check your username, password, and MFA code.');
        return;
      }

      if (this.authService.getMustChangePassword()) {
        this.router.navigate(['/auth/change-password']);
        return;
      }

      const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '/dashboard';
      this.router.navigateByUrl(redirect);
    });
  }
}
