import { Component, OnInit, OnDestroy, inject, signal, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { FinancialReport, ReportFilter } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;
type FinTab = 'summary' | 'margin' | 'tax';

@Component({
  selector: 'app-financial',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './financial.html',
  styleUrl: './financial.scss'
})
export class Financial implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('revenueChart') revenueChartRef!: ElementRef<HTMLCanvasElement>;
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private revenueChart: any;
  readonly Math = Math;

  activeTab = signal<FinTab>('summary');
  isLoading = signal(false);
  error = signal<string | null>(null);
  report = signal<FinancialReport | null>(null);
  datePreset = signal('THIS_MONTH');
  groupBy = signal('DAY');

  datePresets = [
    { label: 'This month', value: 'THIS_MONTH' }, { label: 'Last month', value: 'LAST_MONTH' },
    { label: 'This quarter', value: 'THIS_QUARTER' }, { label: 'This year', value: 'THIS_YEAR' }, { label: 'Last year', value: 'LAST_YEAR' }
  ];
  groupByOptions = [
    { label: 'Daily', value: 'DAY' }, { label: 'Weekly', value: 'WEEK' },
    { label: 'Monthly', value: 'MONTH' }, { label: 'Quarterly', value: 'QUARTER' }
  ];
  exportItems: DropdownItem[] = [
    { id: 'EXCEL', label: 'Export Excel', icon: 'filetype-xlsx' },
    { id: 'CSV', label: 'Export CSV', icon: 'filetype-csv' }
  ];
  tabs = [
    { id: 'summary' as FinTab, label: 'P&L Summary', icon: 'cash-stack' },
    { id: 'margin' as FinTab, label: 'Margin Analysis', icon: 'percent' },
    { id: 'tax' as FinTab, label: 'Tax Summary', icon: 'receipt-cutoff' }
  ];

  ngOnInit() { this.loadData(); }
  ngAfterViewInit() {}
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); if (this.revenueChart) this.revenueChart.destroy(); }

  setTab(t: FinTab) { this.activeTab.set(t); if (t === 'summary') { setTimeout(() => this.renderChart(), 80); } }

  loadData() {
    this.isLoading.set(true); this.error.set(null);
    const filter: ReportFilter = { reportType: 'FINANCIAL_REVENUE', datePreset: this.datePreset() as any, groupBy: this.groupBy() as any };
    this.svc.getFinancialReport(filter).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.report.set(data); this.isLoading.set(false); if (this.activeTab() === 'summary') setTimeout(() => this.renderChart(), 80); },
      error: () => { this.error.set('Failed to load financial data.'); this.alertSvc.error('Load Failed', 'Financial report unavailable.'); this.isLoading.set(false); }
    });
  }

  onExport(item: DropdownItem) {
    this.svc.runAdHoc({ reportType: 'FINANCIAL_REVENUE', exportFormat: item.id as any, datePreset: this.datePreset() as any, groupBy: this.groupBy() as any })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: exec => { if (exec.downloadUrl) { window.open(exec.downloadUrl, '_blank'); this.alertSvc.success('Export Ready', exec.fileName ?? ''); } },
        error: () => this.alertSvc.error('Export Failed', 'Could not generate export.')
      });
  }

  private renderChart() {
    const r = this.report();
    if (!r?.revenueTrend?.length || !this.revenueChartRef) return;
    if (this.revenueChart) this.revenueChart.destroy();
    const ctx = this.revenueChartRef.nativeElement.getContext('2d');
    const trend = r.revenueTrend;
    this.revenueChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: trend.map(t => t.period),
        datasets: [
          { label: 'Gross Revenue', data: trend.map(t => t.grossRevenue), backgroundColor: 'rgba(99,102,241,0.75)', borderRadius: 4, stack: 'a' },
          { label: 'COGS', data: trend.map(t => t.cogs), backgroundColor: 'rgba(239,68,68,0.65)', borderRadius: 4, stack: 'a' },
          { label: 'Gross Profit', data: trend.map(t => t.grossProfit), type: 'line', borderColor: '#10b981', backgroundColor: 'transparent', borderWidth: 2, tension: 0.3, yAxisID: 'y', pointRadius: 3 }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: true, position: 'top' } },
        scales: {
          x: { grid: { display: false }, ticks: { maxTicksLimit: 12, font: { size: 10 } } },
          y: { grid: { color: 'rgba(0,0,0,0.04)' }, ticks: { callback: (v: number) => '$' + (v >= 1000 ? (v/1000).toFixed(0)+'k' : v) } }
        }
      }
    });
  }

  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(v);
  }
  pct(v: number): string { return v != null ? (v | 0) + '%' : '—'; }
  growthClass(g: number | null | undefined): string {
    if (g == null) return 'bg-gray-100 text-gray-600';
    return g >= 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700';
  }
}
