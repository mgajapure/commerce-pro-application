import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { ReportFilter, ReportType, ExportFormat, ReportExecution } from '../../../core/models/analytics/analytics.model';

@Component({
  selector: 'app-custom-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './custom-reports.html',
  styleUrl: './custom-reports.scss'
})
export class CustomReports implements OnInit, OnDestroy {
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  isRunning = signal(false);
  execution = signal<ReportExecution | null>(null);
  error = signal<string | null>(null);

  filter: ReportFilter = {
    reportType: 'SALES_BY_DATE',
    datePreset: 'THIS_MONTH',
    groupBy: 'DAY',
    exportFormat: 'EXCEL'
  };

  reportTypes: { value: ReportType; label: string; group: string }[] = [
    { value: 'SALES_OVERVIEW', label: 'Sales Overview', group: 'Sales' },
    { value: 'SALES_BY_DATE', label: 'Sales by Date', group: 'Sales' },
    { value: 'SALES_BY_PRODUCT', label: 'Sales by Product', group: 'Sales' },
    { value: 'SALES_BY_CATEGORY', label: 'Sales by Category', group: 'Sales' },
    { value: 'SALES_BY_CHANNEL', label: 'Sales by Channel', group: 'Sales' },
    { value: 'INVENTORY_STOCK_VALUE', label: 'Stock Value', group: 'Inventory' },
    { value: 'INVENTORY_MOVEMENT', label: 'Stock Movements', group: 'Inventory' },
    { value: 'INVENTORY_AGEING', label: 'Inventory Ageing', group: 'Inventory' },
    { value: 'ORDER_SUMMARY', label: 'Order Summary', group: 'Orders' },
    { value: 'ORDER_FULFILLMENT_RATE', label: 'Fulfillment Rate', group: 'Orders' },
    { value: 'ORDER_CANCELLATION_RATE', label: 'Cancellation Rate', group: 'Orders' },
    { value: 'CUSTOMER_LTV', label: 'Customer LTV', group: 'Customers' },
    { value: 'CUSTOMER_COHORT', label: 'Customer Cohort', group: 'Customers' },
    { value: 'CUSTOMER_CHURN', label: 'Customer Churn', group: 'Customers' },
    { value: 'FINANCIAL_REVENUE', label: 'Revenue & P&L', group: 'Financial' },
    { value: 'FINANCIAL_MARGIN', label: 'Margin Analysis', group: 'Financial' },
    { value: 'FINANCIAL_TAX_SUMMARY', label: 'Tax Summary', group: 'Financial' },
    { value: 'SHIPPING_ON_TIME_DELIVERY', label: 'On-Time Delivery', group: 'Shipping' },
    { value: 'SHIPPING_CARRIER_PERFORMANCE', label: 'Carrier Performance', group: 'Shipping' },
    { value: 'RETURN_RATE', label: 'Return Rate', group: 'Returns' },
    { value: 'RETURN_REASON_ANALYSIS', label: 'Return Reasons', group: 'Returns' },
  ];
  datePresets = [
    { label: 'Today', value: 'TODAY' }, { label: 'Yesterday', value: 'YESTERDAY' },
    { label: 'Last 7 days', value: 'LAST_7_DAYS' }, { label: 'Last 30 days', value: 'LAST_30_DAYS' },
    { label: 'This month', value: 'THIS_MONTH' }, { label: 'Last month', value: 'LAST_MONTH' },
    { label: 'This quarter', value: 'THIS_QUARTER' }, { label: 'This year', value: 'THIS_YEAR' },
    { label: 'Last year', value: 'LAST_YEAR' }, { label: 'Custom range', value: 'CUSTOM' }
  ];
  groupByOptions = [
    { label: 'Daily', value: 'DAY' }, { label: 'Weekly', value: 'WEEK' },
    { label: 'Monthly', value: 'MONTH' }, { label: 'Quarterly', value: 'QUARTER' }, { label: 'Yearly', value: 'YEAR' }
  ];
  formats: { value: ExportFormat; label: string }[] = [
    { value: 'EXCEL', label: 'Excel (.xlsx)' }, { value: 'CSV', label: 'CSV (.csv)' }, { value: 'JSON', label: 'JSON' }
  ];

  groupedReportTypes(): { group: string; types: { value: ReportType; label: string }[] }[] {
    const groups: Record<string, { value: ReportType; label: string }[]> = {};
    for (const t of this.reportTypes) {
      if (!groups[t.group]) groups[t.group] = [];
      groups[t.group].push({ value: t.value, label: t.label });
    }
    return Object.entries(groups).map(([group, types]) => ({ group, types }));
  }

  ngOnInit() {}
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  runReport() {
    this.isRunning.set(true); this.error.set(null); this.execution.set(null);
    this.svc.runAdHoc({ ...this.filter }).pipe(takeUntil(this.destroy$)).subscribe({
      next: exec => {
        this.execution.set(exec); this.isRunning.set(false);
        if (exec.downloadUrl) this.alertSvc.success('Report Ready', `${exec.fileName} — click Download.`);
        else if (exec.status === 'FAILED') this.alertSvc.error('Report Failed', exec.errorMessage ?? 'Unknown error');
      },
      error: () => { this.error.set('Failed to run report.'); this.alertSvc.error('Failed', 'Could not run report.'); this.isRunning.set(false); }
    });
  }

  download() {
    const exec = this.execution();
    if (exec?.downloadUrl) window.open(exec.downloadUrl, '_blank');
  }

  saveReport() {
    const name = prompt('Report name:');
    if (!name) return;
    this.svc.createSavedReport({ name, reportType: this.filter.reportType, filterParams: this.filter, defaultFormat: this.filter.exportFormat ?? 'EXCEL' })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: () => this.alertSvc.success('Saved', `"${name}" saved to Saved Reports.`),
        error: () => this.alertSvc.error('Failed', 'Could not save report.')
      });
  }

  formatSize(bytes: number | undefined): string {
    if (!bytes) return '';
    if (bytes > 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB';
    return (bytes / 1024).toFixed(0) + ' KB';
  }
}
