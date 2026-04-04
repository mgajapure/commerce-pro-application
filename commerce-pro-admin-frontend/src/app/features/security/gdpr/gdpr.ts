import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SecurityService, GdprSettings } from '../security.service';

@Component({
  selector: 'app-gdpr-privacy',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gdpr.html',
  styles: [':host { display: block; }']
})
export class GdprPrivacy implements OnInit {
  private svc = inject(SecurityService);

  loading = signal(true);
  saving = signal(false);
  success = signal('');
  error = signal('');

  settings = signal<GdprSettings>({
    dataRetention: { retentionDays: 365, autoAnonymize: false },
    consentManagement: { cookieConsentRequired: true, marketingConsent: false },
    dataSubjectRights: { allowExport: true, allowDeletion: true, processingDays: 30 },
    cookieSettings: { analytics: true, marketing: false, functional: true }
  });

  ngOnInit() {
    this.svc.getGdprSettings().subscribe(s => {
      this.settings.set(s);
      this.loading.set(false);
    });
  }

  updateRetention(field: string, value: any) {
    this.settings.update(s => ({ ...s, dataRetention: { ...s.dataRetention, [field]: value } }));
  }

  updateConsent(field: string, value: any) {
    this.settings.update(s => ({ ...s, consentManagement: { ...s.consentManagement, [field]: value } }));
  }

  updateRights(field: string, value: any) {
    this.settings.update(s => ({ ...s, dataSubjectRights: { ...s.dataSubjectRights, [field]: value } }));
  }

  updateCookies(field: string, value: any) {
    this.settings.update(s => ({ ...s, cookieSettings: { ...s.cookieSettings, [field]: value } }));
  }

  save() {
    this.saving.set(true);
    this.success.set('');
    this.error.set('');
    this.svc.updateGdprSettings(this.settings()).subscribe(res => {
      this.saving.set(false);
      if (res) this.success.set('GDPR settings saved successfully.');
      else this.error.set('Failed to save GDPR settings.');
    });
  }
}
