import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SettingsService } from '../settings.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-store-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './store.html',
  styles: [`:host { display: block; }`]
})
export class StoreSettings implements OnInit, OnDestroy {
  private settingsService = inject(SettingsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  loading = signal(true);
  saving = signal(false);

  storeName = signal('');
  description = signal('');
  street = signal('');
  city = signal('');
  state = signal('');
  zip = signal('');
  country = signal('');
  phone = signal('');
  email = signal('');
  logoUrl = signal('');
  currency = signal('USD');
  taxIncluded = signal(false);
  weightUnit = signal('kg');
  dimensionUnit = signal('cm');

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Basic Information', content: 'Set your store name, description, contact info, and logo URL that appear on your storefront.' },
    { title: 'Address', content: 'Your store\'s physical address used for shipping calculations and legal compliance.' },
    { title: 'Commerce Settings', content: 'Configure currency, weight/dimension units, and tax display preferences for your products.' }
  ];

  currencies = [
    { code: 'USD', name: 'US Dollar ($)' },
    { code: 'EUR', name: 'Euro (\u20AC)' },
    { code: 'GBP', name: 'British Pound (\u00A3)' },
    { code: 'JPY', name: 'Japanese Yen (\u00A5)' },
    { code: 'CAD', name: 'Canadian Dollar (C$)' },
    { code: 'AUD', name: 'Australian Dollar (A$)' },
    { code: 'INR', name: 'Indian Rupee (\u20B9)' },
    { code: 'BRL', name: 'Brazilian Real (R$)' },
    { code: 'CNY', name: 'Chinese Yuan (\u00A5)' },
    { code: 'MXN', name: 'Mexican Peso (MX$)' }
  ];

  weightUnits = ['kg', 'g', 'lb', 'oz'];
  dimensionUnits = ['cm', 'mm', 'in', 'ft', 'm'];

  ngOnInit(): void {
    this.settingsService.getStoreSettings().pipe(takeUntil(this.destroy$)).subscribe(data => {
      if (data) {
        this.storeName.set(data.storeName ?? '');
        this.description.set(data.description ?? '');
        this.street.set(data.street ?? '');
        this.city.set(data.city ?? '');
        this.state.set(data.state ?? '');
        this.zip.set(data.zip ?? '');
        this.country.set(data.country ?? '');
        this.phone.set(data.phone ?? '');
        this.email.set(data.email ?? '');
        this.logoUrl.set(data.logoUrl ?? '');
        this.currency.set(data.currency ?? 'USD');
        this.taxIncluded.set(data.taxIncluded ?? false);
        this.weightUnit.set(data.weightUnit ?? 'kg');
        this.dimensionUnit.set(data.dimensionUnit ?? 'cm');
      }
      this.loading.set(false);
    });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  save(): void {
    this.saving.set(true);
    const payload = {
      storeName: this.storeName(), description: this.description(),
      street: this.street(), city: this.city(), state: this.state(),
      zip: this.zip(), country: this.country(), phone: this.phone(),
      email: this.email(), logoUrl: this.logoUrl(), currency: this.currency(),
      taxIncluded: this.taxIncluded(), weightUnit: this.weightUnit(),
      dimensionUnit: this.dimensionUnit()
    };

    this.settingsService.updateStoreSettings(payload).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.saving.set(false); this.alertSvc.success('Settings Saved', 'Store settings updated successfully'); },
      error: () => { this.saving.set(false); this.alertSvc.error('Failed', 'Could not save store settings'); }
    });
  }
}
