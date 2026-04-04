import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../settings.service';

@Component({
  selector: 'app-general-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './general.html',
  styles: [`:host { display: block; }`]
})
export class GeneralSettings implements OnInit {
  private settingsService = inject(SettingsService);

  loading = signal(true);
  saving = signal(false);
  success = signal(false);
  error = signal<string | null>(null);

  siteName = signal('');
  tagline = signal('');
  timezone = signal('UTC');
  dateFormat = signal('MM/DD/YYYY');
  defaultLanguage = signal('en');
  maintenanceMode = signal(false);
  maintenanceMessage = signal('');

  timezones = signal([
    'UTC', 'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles',
    'America/Anchorage', 'Pacific/Honolulu', 'America/Toronto', 'America/Vancouver',
    'Europe/London', 'Europe/Paris', 'Europe/Berlin', 'Europe/Moscow',
    'Asia/Tokyo', 'Asia/Shanghai', 'Asia/Kolkata', 'Asia/Dubai',
    'Australia/Sydney', 'Australia/Melbourne', 'Pacific/Auckland',
    'America/Sao_Paulo', 'Africa/Johannesburg', 'Africa/Cairo'
  ]);

  dateFormats = signal([
    'MM/DD/YYYY', 'DD/MM/YYYY', 'YYYY-MM-DD', 'DD-MMM-YYYY', 'MMM DD, YYYY'
  ]);

  languages = signal([
    { code: 'en', name: 'English' },
    { code: 'es', name: 'Spanish' },
    { code: 'fr', name: 'French' },
    { code: 'de', name: 'German' },
    { code: 'pt', name: 'Portuguese' },
    { code: 'ja', name: 'Japanese' },
    { code: 'zh', name: 'Chinese (Simplified)' },
    { code: 'ko', name: 'Korean' },
    { code: 'ar', name: 'Arabic' },
    { code: 'hi', name: 'Hindi' }
  ]);

  ngOnInit(): void {
    this.settingsService.getGeneralSettings().subscribe(data => {
      if (data) {
        this.siteName.set(data.siteName ?? '');
        this.tagline.set(data.tagline ?? '');
        this.timezone.set(data.timezone ?? 'UTC');
        this.dateFormat.set(data.dateFormat ?? 'MM/DD/YYYY');
        this.defaultLanguage.set(data.defaultLanguage ?? 'en');
        this.maintenanceMode.set(data.maintenanceMode ?? false);
        this.maintenanceMessage.set(data.maintenanceMessage ?? '');
      }
      this.loading.set(false);
    });
  }

  save(): void {
    this.saving.set(true);
    this.success.set(false);
    this.error.set(null);

    const payload = {
      siteName: this.siteName(),
      tagline: this.tagline(),
      timezone: this.timezone(),
      dateFormat: this.dateFormat(),
      defaultLanguage: this.defaultLanguage(),
      maintenanceMode: this.maintenanceMode(),
      maintenanceMessage: this.maintenanceMessage()
    };

    this.settingsService.updateGeneralSettings(payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(true);
        setTimeout(() => this.success.set(false), 3000);
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Failed to save settings. Please try again.');
      }
    });
  }
}
