import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SettingsService } from '../settings.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-notification-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './notifications.html',
  styles: [`:host { display: block; }`]
})
export class NotificationSettings implements OnInit, OnDestroy {
  private svc = inject(SettingsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  loading = signal(true);
  saving = signal(false);

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

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Email Notifications', content: 'Configure SMTP settings and sender information for transactional emails.' },
    { title: 'SMS Notifications', content: 'Enable SMS notifications with providers like Twilio, Nexmo, or AWS SNS.' },
    { title: 'Push Notifications', content: 'Enable browser and mobile push notifications for real-time alerts.' },
    { title: 'Templates', content: 'Toggle which notification templates are active for automatic sending.' }
  ];

  templateList = [
    { key: 'orderConfirmation', label: 'Order Confirmation', icon: 'bi-bag-check' },
    { key: 'shippingUpdate', label: 'Shipping Update', icon: 'bi-truck' },
    { key: 'passwordReset', label: 'Password Reset', icon: 'bi-key' },
    { key: 'welcomeEmail', label: 'Welcome Email', icon: 'bi-envelope-heart' },
  ];

  ngOnInit() {
    this.svc.getNotificationSettings().pipe(takeUntil(this.destroy$)).subscribe(data => {
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

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  toggleTemplate(key: string) {
    this.templates.update(t => ({ ...t, [key]: !t[key] }));
  }

  save() {
    this.saving.set(true);
    const payload = {
      emailEnabled: this.emailEnabled(), emailFrom: this.emailFrom(),
      emailFromName: this.emailFromName(), smtpHost: this.smtpHost(),
      smtpPort: this.smtpPort(), smtpUseTls: this.smtpUseTls(),
      smsEnabled: this.smsEnabled(), smsProvider: this.smsProvider(),
      pushEnabled: this.pushEnabled(), templates: this.templates()
    };

    this.svc.updateNotificationSettings(payload).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.saving.set(false); this.alertSvc.success('Settings Saved', 'Notification settings updated successfully'); },
      error: () => { this.saving.set(false); this.alertSvc.error('Failed', 'Could not save notification settings'); }
    });
  }
}
