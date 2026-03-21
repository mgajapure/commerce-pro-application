import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { ScheduledReportResponse, ScheduledReportRequest, SavedReportResponse, ScheduleFrequency, ExportFormat } from '../../../core/models/analytics/analytics.model';

@Component({
  selector: 'app-scheduled-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent],
  templateUrl: './scheduled-reports.html',
  styleUrl: './scheduled-reports.scss'
})
export class ScheduledReportsPage implements OnInit, OnDestroy {
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  schedules = signal<ScheduledReportResponse[]>([]);
  savedReports = signal<SavedReportResponse[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  showCreateModal = signal(false);
  runningId = signal<string | null>(null);

  newSchedule: ScheduledReportRequest = {
    savedReportId: '', name: '', frequency: 'DAILY', recipientEmails: '', format: 'EXCEL', isActive: true
  };

  frequencies: { value: ScheduleFrequency; label: string }[] = [
    { value: 'DAILY', label: 'Daily' }, { value: 'WEEKLY', label: 'Weekly' },
    { value: 'MONTHLY', label: 'Monthly' }, { value: 'QUARTERLY', label: 'Quarterly' }
  ];
  formats: { value: ExportFormat; label: string }[] = [
    { value: 'EXCEL', label: 'Excel (.xlsx)' }, { value: 'CSV', label: 'CSV' }, { value: 'JSON', label: 'JSON' }
  ];

  ngOnInit() { this.loadData(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  loadData() {
    this.isLoading.set(true);
    this.svc.listScheduledReports().pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.schedules.set(data.content); this.isLoading.set(false); },
      error: () => { this.error.set('Failed to load schedules.'); this.isLoading.set(false); }
    });
    this.svc.listSavedReports().pipe(takeUntil(this.destroy$)).subscribe({
      next: data => this.savedReports.set(data.content)
    });
  }

  createSchedule() {
    if (!this.newSchedule.savedReportId || !this.newSchedule.name || !this.newSchedule.recipientEmails) {
      this.alertSvc.warning('Required fields', 'Please fill in all required fields.'); return;
    }
    this.svc.createScheduledReport(this.newSchedule).pipe(takeUntil(this.destroy$)).subscribe({
      next: s => {
        this.schedules.update(list => [s, ...list]);
        this.showCreateModal.set(false);
        this.newSchedule = { savedReportId: '', name: '', frequency: 'DAILY', recipientEmails: '', format: 'EXCEL', isActive: true };
        this.alertSvc.success('Scheduled', `"${s.name}" will run ${s.frequency.toLowerCase()}.`);
      },
      error: () => this.alertSvc.error('Failed', 'Could not create schedule.')
    });
  }

  toggle(id: string) {
    this.svc.toggleScheduledReport(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: updated => {
        this.schedules.update(list => list.map(s => s.id === id ? updated : s));
        this.alertSvc.info('Updated', updated.isActive ? 'Schedule activated.' : 'Schedule paused.');
      },
      error: () => this.alertSvc.error('Failed', 'Could not toggle schedule.')
    });
  }

  runNow(id: string) {
    this.runningId.set(id);
    this.svc.runScheduledNow(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: exec => {
        this.runningId.set(null);
        if (exec.downloadUrl) { window.open(exec.downloadUrl, '_blank'); }
        this.alertSvc.success('Report Run', exec.fileName ?? 'Report generated.');
      },
      error: () => { this.runningId.set(null); this.alertSvc.error('Failed', 'Could not run report.'); }
    });
  }

  deleteSchedule(id: string) {
    this.alertSvc.confirm({ title: 'Delete Schedule', message: 'Stop this scheduled report?', danger: true }).then(ok => {
      if (!ok) return;
      this.svc.deleteScheduledReport(id).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => { this.schedules.update(list => list.filter(s => s.id !== id)); this.alertSvc.success('Deleted', 'Schedule removed.'); },
        error: () => this.alertSvc.error('Failed', 'Could not delete.')
      });
    });
  }

  formatDate(d: string | undefined): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
  statusBadge(active: boolean): string { return active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'; }
  lastRunBadge(status: string | undefined): string {
    if (!status) return 'bg-gray-100 text-gray-600';
    return status === 'COMPLETED' ? 'bg-green-100 text-green-800' : status === 'FAILED' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800';
  }
}
