import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SettingsService } from '../settings.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-general-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './general.html',
  styles: [`:host { display: block; }`]
})
export class GeneralSettings implements OnInit, OnDestroy {
  private settingsService = inject(SettingsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  loading = signal(true);
  saving = signal(false);

  siteName = signal('');
  tagline = signal('');
  timezone = signal('UTC');
  dateFormat = signal('MM/DD/YYYY');
  defaultLanguage = signal('en');
  maintenanceMode = signal(false);
  maintenanceMessage = signal('');

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Site Information', content: 'Configure the name and tagline that appear across your storefront and admin panel.' },
    { title: 'Regional Settings', content: 'Set the timezone, date format, and default language for your platform.' },
    { title: 'Maintenance Mode', content: 'When enabled, visitors see a maintenance page instead of your store. Admin access is not affected.' }
  ];

  timezones = ['UTC', 'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles',
    'Europe/London', 'Europe/Paris', 'Europe/Berlin', 'Asia/Tokyo', 'Asia/Shanghai', 'Asia/Kolkata',
    'Australia/Sydney', 'Pacific/Auckland', 'America/Sao_Paulo'];

  dateFormats = ['MM/DD/YYYY', 'DD/MM/YYYY', 'YYYY-MM-DD', 'DD-MMM-YYYY', 'MMM DD, YYYY'];

  languages = [
    { code: 'en', name: 'English' }, { code: 'es', name: 'Spanish' }, { code: 'fr', name: 'French' },
    { code: 'de', name: 'German' }, { code: 'pt', name: 'Portuguese' }, { code: 'ja', name: 'Japanese' },
    { code: 'zh', name: 'Chinese' }, { code: 'ko', name: 'Korean' }, { code: 'ar', name: 'Arabic' }, { code: 'hi', name: 'Hindi' }
  ];

  ngOnInit(): void {
    this.settingsService.getGeneralSettings().pipe(takeUntil(this.destroy$)).subscribe(data => {
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

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  save(): void {
    this.saving.set(true);
    const payload = {
      siteName: this.siteName(), tagline: this.tagline(), timezone: this.timezone(),
      dateFormat: this.dateFormat(), defaultLanguage: this.defaultLanguage(),
      maintenanceMode: this.maintenanceMode(), maintenanceMessage: this.maintenanceMessage()
    };

    this.settingsService.updateGeneralSettings(payload).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.saving.set(false); this.alertSvc.success('Settings Saved', 'General settings updated successfully'); },
      error: () => { this.saving.set(false); this.alertSvc.error('Failed', 'Could not save settings'); }
    });
  }
}
