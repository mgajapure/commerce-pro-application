import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { SavedReportResponse, SavedReportRequest, ReportExecution, ReportType, ExportFormat } from '../../../core/models/analytics/analytics.model';

@Component({
  selector: 'app-saved-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent],
  templateUrl: './saved-reports.html',
  styleUrl: './saved-reports.scss'
})
export class SavedReportsPage implements OnInit, OnDestroy {
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  reports = signal<SavedReportResponse[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  showCreateModal = signal(false);
  runningId = signal<string | null>(null);
  lastExecution = signal<ReportExecution | null>(null);

  newReport: SavedReportRequest = { name: '', reportType: 'SALES_OVERVIEW', isPublic: false, defaultFormat: 'EXCEL' };

  reportTypes: { value: ReportType; label: string }[] = [
    { value: 'SALES_OVERVIEW', label: 'Sales Overview' }, { value: 'SALES_BY_DATE', label: 'Sales by Date' },
    { value: 'SALES_BY_PRODUCT', label: 'Sales by Product' }, { value: 'SALES_BY_CATEGORY', label: 'Sales by Category' },
    { value: 'SALES_BY_CHANNEL', label: 'Sales by Channel' }, { value: 'INVENTORY_STOCK_VALUE', label: 'Inventory Stock Value' },
    { value: 'INVENTORY_MOVEMENT', label: 'Inventory Movement' }, { value: 'INVENTORY_AGEING', label: 'Inventory Ageing' },
    { value: 'ORDER_SUMMARY', label: 'Order Summary' }, { value: 'CUSTOMER_LTV', label: 'Customer LTV' },
    { value: 'CUSTOMER_CHURN', label: 'Customer Churn' }, { value: 'CUSTOMER_COHORT', label: 'Customer Cohort' },
    { value: 'FINANCIAL_REVENUE', label: 'Financial Revenue' }, { value: 'FINANCIAL_MARGIN', label: 'Financial Margin' },
    { value: 'FINANCIAL_TAX_SUMMARY', label: 'Tax Summary' }, { value: 'SHIPPING_ON_TIME_DELIVERY', label: 'Shipping On-Time' },
    { value: 'SHIPPING_CARRIER_PERFORMANCE', label: 'Carrier Performance' }, { value: 'RETURN_RATE', label: 'Return Rate' }
  ];
  formats: { value: ExportFormat; label: string }[] = [
    { value: 'EXCEL', label: 'Excel (.xlsx)' }, { value: 'CSV', label: 'CSV' }, { value: 'JSON', label: 'JSON' }
  ];

  ngOnInit() { this.loadReports(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  loadReports() {
    this.isLoading.set(true);
    this.svc.listSavedReports().pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.reports.set(data.content); this.isLoading.set(false); },
      error: () => { this.error.set('Failed to load saved reports.'); this.isLoading.set(false); }
    });
  }

  createReport() {
    if (!this.newReport.name.trim()) { this.alertSvc.warning('Required', 'Please enter a report name.'); return; }
    this.svc.createSavedReport(this.newReport).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => {
        this.reports.update(list => [r, ...list]);
        this.showCreateModal.set(false);
        this.newReport = { name: '', reportType: 'SALES_OVERVIEW', isPublic: false, defaultFormat: 'EXCEL' };
        this.alertSvc.success('Report Saved', `"${r.name}" has been saved.`);
      },
      error: () => this.alertSvc.error('Save Failed', 'Could not save report.')
    });
  }

  runReport(id: string, name: string) {
    this.runningId.set(id);
    this.svc.runSavedReport(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: exec => {
        this.runningId.set(null);
        this.lastExecution.set(exec);
        if (exec.downloadUrl) {
          this.alertSvc.success('Report Ready', `${exec.fileName} — click to download.`);
        }
      },
      error: () => { this.runningId.set(null); this.alertSvc.error('Run Failed', 'Could not run report.'); }
    });
  }

  downloadLast() {
    const exec = this.lastExecution();
    if (exec?.downloadUrl) window.open(exec.downloadUrl, '_blank');
  }

  deleteReport(id: string) {
    this.alertSvc.confirm({ title: 'Delete Saved Report', message: 'Are you sure? This cannot be undone.', danger: true })
      .then(confirmed => {
        if (!confirmed) return;
        this.svc.deleteSavedReport(id).pipe(takeUntil(this.destroy$)).subscribe({
          next: () => { this.reports.update(list => list.filter(r => r.id !== id)); this.alertSvc.success('Deleted', 'Report removed.'); },
          error: () => this.alertSvc.error('Delete Failed', 'Could not delete report.')
        });
      });
  }

  reportTypeLabel(rt: ReportType): string {
    return this.reportTypes.find(t => t.value === rt)?.label ?? rt;
  }
  formatDate(d: string): string { return d ? new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'; }
}
