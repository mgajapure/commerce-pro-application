import { Component, signal, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SecurityService, GdprSettings } from '../security.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-gdpr-privacy',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './gdpr.html',
  styles: [':host { display: block; }']
})
export class GdprPrivacy implements OnInit, OnDestroy {
  private svc = inject(SecurityService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  loading = signal(true);
  saving = signal(false);

  settings = signal<GdprSettings>({
    dataRetention: { retentionDays: 365, autoAnonymize: false },
    consentManagement: { cookieConsentRequired: true, marketingConsent: false },
    dataSubjectRights: { allowExport: true, allowDeletion: true, processingDays: 30 },
    cookieSettings: { analytics: true, marketing: false, functional: true }
  });

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Data Retention', content: 'Configure how long personal data is stored. Auto-anonymize replaces personal data with anonymous identifiers after the retention period.' },
    { title: 'Consent Management', content: 'Manage cookie consent requirements. When enabled, users must explicitly consent before cookies are set.' },
    { title: 'Data Subject Rights', content: 'GDPR grants individuals the right to export and delete their data. Configure processing timeframes to meet compliance requirements.' },
    { title: 'Cookie Settings', content: 'Control which types of cookies are enabled by default. Users can override these in cookie consent dialogs.' }
  ];

  ngOnInit() {
    this.svc.getGdprSettings().pipe(takeUntil(this.destroy$)).subscribe(s => {
      this.settings.set(s);
      this.loading.set(false);
    });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

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
    this.svc.updateGdprSettings(this.settings()).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.saving.set(false);
      if (res) this.alertSvc.success('GDPR Settings Saved');
      else this.alertSvc.error('Failed', 'Could not save GDPR settings');
    });
  }
}
