import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../settings.service';

@Component({
  selector: 'app-store-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './store.html',
  styles: [`:host { display: block; }`]
})
export class StoreSettings implements OnInit {
  private settingsService = inject(SettingsService);

  loading = signal(true);
  saving = signal(false);
  success = signal(false);
  error = signal<string | null>(null);

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

  currencies = signal([
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
  ]);

  weightUnits = signal(['kg', 'g', 'lb', 'oz']);
  dimensionUnits = signal(['cm', 'mm', 'in', 'ft', 'm']);

  ngOnInit(): void {
    this.settingsService.getStoreSettings().subscribe(data => {
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

  save(): void {
    this.saving.set(true);
    this.success.set(false);
    this.error.set(null);

    const payload = {
      storeName: this.storeName(),
      description: this.description(),
      street: this.street(),
      city: this.city(),
      state: this.state(),
      zip: this.zip(),
      country: this.country(),
      phone: this.phone(),
      email: this.email(),
      logoUrl: this.logoUrl(),
      currency: this.currency(),
      taxIncluded: this.taxIncluded(),
      weightUnit: this.weightUnit(),
      dimensionUnit: this.dimensionUnit()
    };

    this.settingsService.updateStoreSettings(payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(true);
        setTimeout(() => this.success.set(false), 3000);
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Failed to save store settings. Please try again.');
      }
    });
  }
}
