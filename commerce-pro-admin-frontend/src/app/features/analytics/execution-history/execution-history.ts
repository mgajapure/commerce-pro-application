import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { ReportExecution } from '../../../core/models/analytics/analytics.model';

@Component({
  selector: 'app-execution-history',
  standalone: true,
  imports: [CommonModule, RouterModule, AlertComponent],
  templateUrl: './execution-history.html',
  styleUrl: './execution-history.scss'
})
export class ExecutionHistoryPage implements OnInit, OnDestroy {
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  executions = signal<ReportExecution[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);

  ngOnInit() { this.loadHistory(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  loadHistory() {
    this.isLoading.set(true);
    this.svc.listExecutions(0, 50).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.executions.set(data.content); this.isLoading.set(false); },
      error: () => { this.error.set('Failed to load execution history.'); this.isLoading.set(false); }
    });
  }

  download(exec: ReportExecution) {
    if (exec.downloadUrl) {
      window.open(exec.downloadUrl, '_blank');
      this.alertSvc.success('Downloading', exec.fileName ?? '');
    } else {
      this.alertSvc.warning('Not Available', 'No download URL for this execution.');
    }
  }

  statusBadge(s: string): string {
    return s === 'COMPLETED' ? 'bg-green-100 text-green-800' : s === 'FAILED' ? 'bg-red-100 text-red-800'
      : s === 'RUNNING' ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800';
  }
  formatDate(d: string | undefined): string {
    if (!d) return '—';
    return new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
  formatSize(bytes: number | undefined): string {
    if (!bytes) return '—';
    if (bytes > 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB';
    if (bytes > 1024) return (bytes / 1024).toFixed(0) + ' KB';
    return bytes + ' B';
  }
}
