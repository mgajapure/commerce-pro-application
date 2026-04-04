import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SecurityService, SsoConfiguration } from '../security.service';

@Component({
  selector: 'app-sso-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sso.html',
  styles: [':host { display: block; }']
})
export class SsoConfig implements OnInit {
  private svc = inject(SecurityService);

  loading = signal(true);
  saving = signal(false);
  success = signal('');
  error = signal('');

  config = signal<SsoConfiguration>({
    enabled: false,
    provider: 'SAML',
    clientId: '',
    clientSecret: '',
    issuerUrl: '',
    callbackUrl: '',
    allowedDomains: [],
    autoProvisionUsers: false
  });

  newDomain = signal('');

  ngOnInit() {
    this.svc.getSsoConfig().subscribe(c => {
      this.config.set(c);
      this.loading.set(false);
    });
  }

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
    this.success.set('');
    this.error.set('');
    this.svc.updateSsoConfig(this.config()).subscribe(res => {
      this.saving.set(false);
      if (res) this.success.set('SSO configuration saved successfully.');
      else this.error.set('Failed to save SSO configuration.');
    });
  }

  testConnection() {
    this.success.set('');
    this.error.set('');
    this.svc.testSsoConnection(this.config()).subscribe(res => {
      if (res.success) this.success.set('Connection test successful!');
      else this.error.set(res.message || 'Connection test failed.');
    });
  }
}
