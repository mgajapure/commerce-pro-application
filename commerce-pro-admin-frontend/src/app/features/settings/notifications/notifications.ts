import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../settings.service';

@Component({
  selector: 'app-notification-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './notifications.html',
  styles: [`:host { display: block; }`]
})
export class NotificationSettings implements OnInit {
  private svc = inject(SettingsService);

  loading = signal(true);
  saving = signal(false);
  success = signal(false);
  error = signal<string | null>(null);

  emailEnabled = signal(true);
  emailFrom = signal('');
  emailFromName = signal('');
  smtpHost = signal('');
  smtpPort = signal(587);
  smtpUseTls = signal(true);

  smsEnabled = signal(false);
  smsProvider = signal('twilio');
  pushEnabled = signal(false);

  templates = signal<Record<string, boolean>>({
    orderConfirmation: true,
    shippingUpdate: true,
    passwordReset: true,
    welcomeEmail: true
  });

  templateList = [
    { key: 'orderConfirmation', label: 'Order Confirmation' },
    { key: 'shippingUpdate', label: 'Shipping Update' },
    { key: 'passwordReset', label: 'Password Reset' },
    { key: 'welcomeEmail', label: 'Welcome Email' },
  ];

  ngOnInit() {
    this.svc.getNotificationSettings().subscribe(data => {
      if (data) {
        this.emailEnabled.set(data.emailEnabled ?? true);
        this.emailFrom.set(data.emailFrom ?? '');
        this.emailFromName.set(data.emailFromName ?? '');
        this.smtpHost.set(data.smtpHost ?? '');
        this.smtpPort.set(data.smtpPort ?? 587);
        this.smtpUseTls.set(data.smtpUseTls ?? true);
        this.smsEnabled.set(data.smsEnabled ?? false);
        this.smsProvider.set(data.smsProvider ?? 'twilio');
        this.pushEnabled.set(data.pushEnabled ?? false);
        if (data.templates) this.templates.set(data.templates);
      }
      this.loading.set(false);
    });
  }

  toggleTemplate(key: string) {
    this.templates.update(t => ({ ...t, [key]: !t[key] }));
  }

  save() {
    this.saving.set(true);
    this.success.set(false);
    this.error.set(null);

    const payload = {
      emailEnabled: this.emailEnabled(),
      emailFrom: this.emailFrom(),
      emailFromName: this.emailFromName(),
      smtpHost: this.smtpHost(),
      smtpPort: this.smtpPort(),
      smtpUseTls: this.smtpUseTls(),
      smsEnabled: this.smsEnabled(),
      smsProvider: this.smsProvider(),
      pushEnabled: this.pushEnabled(),
      templates: this.templates()
    };

    this.svc.updateNotificationSettings(payload).subscribe({
      next: () => { this.saving.set(false); this.success.set(true); setTimeout(() => this.success.set(false), 3000); },
      error: () => { this.saving.set(false); this.error.set('Failed to save notification settings.'); }
    });
  }
}
