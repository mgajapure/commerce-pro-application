import { Component, signal, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SecurityService, SsoConfiguration } from '../security.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-sso-config',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './sso.html',
  styles: [':host { display: block; }']
})
export class SsoConfig implements OnInit, OnDestroy {
  private svc = inject(SecurityService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  loading = signal(true);
  saving = signal(false);
  testing = signal(false);

  config = signal<SsoConfiguration>({
    enabled: false, provider: 'SAML', clientId: '', clientSecret: '',
    issuerUrl: '', callbackUrl: '', allowedDomains: [], autoProvisionUsers: false
  });

  newDomain = signal('');

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'What is SSO?', content: 'Single Sign-On allows users to authenticate using your organization\'s identity provider instead of separate credentials.' },
    { title: 'Providers', content: 'SAML 2.0 is the most common enterprise SSO protocol. OAuth 2.0 and OpenID Connect are popular for web applications.' },
    { title: 'Allowed Domains', content: 'Restrict SSO to specific email domains. Only users with matching domains can authenticate via SSO.' },
    { title: 'Auto-Provisioning', content: 'When enabled, user accounts are automatically created on first SSO login without manual setup.' }
  ];

  ngOnInit() {
    this.svc.getSsoConfig().pipe(takeUntil(this.destroy$)).subscribe(c => {
      this.config.set(c);
      this.loading.set(false);
    });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  updateField(field: keyof SsoConfiguration, value: any) {
    this.config.update(c => ({ ...c, [field]: value }));
  }

  addDomain() {
    const d = this.newDomain().trim();
    if (d && !this.config().allowedDomains.includes(d)) {
      this.config.update(c => ({ ...c, allowedDomains: [...c.allowedDomains, d] }));
      this.newDomain.set('');
    }
  }

  removeDomain(domain: string) {
    this.config.update(c => ({ ...c, allowedDomains: c.allowedDomains.filter(d => d !== domain) }));
  }

  save() {
    this.saving.set(true);
    this.svc.updateSsoConfig(this.config()).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.saving.set(false);
      if (res) this.alertSvc.success('SSO Configuration Saved');
      else this.alertSvc.error('Failed', 'Could not save SSO configuration');
    });
  }

  testConnection() {
    this.testing.set(true);
    this.svc.testSsoConnection(this.config()).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.testing.set(false);
      if (res.success) this.alertSvc.success('Connection Successful', 'SSO provider is reachable');
      else this.alertSvc.error('Connection Failed', res.message);
    });
  }
}
