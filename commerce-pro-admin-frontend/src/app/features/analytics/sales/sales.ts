import { Component, OnInit, OnDestroy, inject, signal, computed, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Observable, Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { SalesReport, SalesOverview, ReportFilter, GroupBy } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;
type SalesTab = 'overview' | 'by-date' | 'by-product' | 'by-category' | 'by-channel';

@Component({
  selector: 'app-sales',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './sales.html',
  styleUrl: './sales.scss'
})
export class Sales implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('trendChart') trendChartRef!: ElementRef<HTMLCanvasElement>;
  private analyticsService = inject(AnalyticsService);
  private alertService = inject(AlertService);
  private destroy$ = new Subject<void>();
  private trendChart: any;
  readonly Math = Math;

  activeTab = signal<SalesTab>('overview');
  isLoading = signal(false);
  error = signal<string | null>(null);
  overview = signal<SalesOverview | null>(null);
  report = signal<SalesReport | null>(null);
  datePreset = signal<string>('THIS_MONTH');
  dateFrom = signal('');
  dateTo = signal('');
  groupBy = signal<GroupBy>('DAY');

  datePresets = [
    { label: 'Today', value: 'TODAY' }, { label: 'Yesterday', value: 'YESTERDAY' },
    { label: 'Last 7 days', value: 'LAST_7_DAYS' }, { label: 'Last 30 days', value: 'LAST_30_DAYS' },
    { label: 'This month', value: 'THIS_MONTH' }, { label: 'Last month', value: 'LAST_MONTH' },
    { label: 'This quarter', value: 'THIS_QUARTER' }, { label: 'This year', value: 'THIS_YEAR' },
    { label: 'Custom range', value: 'CUSTOM' }
  ];
  groupByOptions: { label: string; value: GroupBy }[] = [
    { label: 'Daily', value: 'DAY' }, { label: 'Weekly', value: 'WEEK' },
    { label: 'Monthly', value: 'MONTH' }, { label: 'Quarterly', value: 'QUARTER' }, { label: 'Yearly', value: 'YEAR' }
  ];
  exportItems: DropdownItem[] = [
    { id: 'EXCEL', label: 'Export Excel', icon: 'filetype-xlsx' },
    { id: 'CSV', label: 'Export CSV', icon: 'filetype-csv' },
    { id: 'JSON', label: 'Export JSON', icon: 'braces' }
  ];
  tabs: { id: SalesTab; label: string; icon: string }[] = [
    { id: 'overview', label: 'Overview', icon: 'bar-chart' },
    { id: 'by-date', label: 'By Date', icon: 'calendar3' },
    { id: 'by-product', label: 'By Product', icon: 'box-seam' },
    { id: 'by-category', label: 'By Category', icon: 'tag' },
    { id: 'by-channel', label: 'By Channel', icon: 'diagram-3' }
  ];

  summaryCards = computed(() => {
    const o = this.overview(); const r = this.report();
    const src: any = this.activeTab() === 'overview' ? o : r;
    if (!src) return [];
    return [
      { label: 'Total Revenue', value: this.fmt(src.totalRevenue ?? 0), icon: 'currency-dollar', bg: 'bg-indigo-100', ic: 'text-indigo-600', growth: src.revenueGrowthPct ?? src.revenueChangePct },
      { label: 'Total Orders', value: (src.totalOrders ?? 0).toLocaleString(), icon: 'cart-check', bg: 'bg-blue-100', ic: 'text-blue-600', growth: src.orderGrowthPct ?? src.ordersChangePct },
      { label: 'Avg Order Value', value: this.fmt(src.averageOrderValue ?? 0), icon: 'receipt', bg: 'bg-emerald-100', ic: 'text-emerald-600', growth: src.aovGrowthPct },
      { label: 'Net Revenue', value: this.fmt(src.netRevenue ?? src.totalRevenue ?? 0), icon: 'graph-up', bg: 'bg-purple-100', ic: 'text-purple-600', growth: null }
    ];
  });

  ngOnInit(): void { this.loadData(); }
  ngAfterViewInit(): void {}
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); if (this.trendChart) this.trendChart.destroy(); }

  setTab(tab: SalesTab): void { this.activeTab.set(tab); this.loadData(); }

  loadData(): void {
    this.isLoading.set(true); this.error.set(null);
    const filter = this.buildFilter();
    const tab = this.activeTab();
    const obs: Observable<any> = tab === 'overview' ? this.analyticsService.getSalesOverview(filter)
      : tab === 'by-date' ? this.analyticsService.getSalesByDate(filter)
      : tab === 'by-product' ? this.analyticsService.getSalesByProduct(filter)
      : tab === 'by-category' ? this.analyticsService.getSalesByCategory(filter)
      : this.analyticsService.getSalesByChannel(filter);

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: (data: any) => {
        if (tab === 'overview') { this.overview.set(data); setTimeout(() => this.renderChart(), 80); }
        else this.report.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load sales data.'); this.alertService.error('Load Failed', 'Sales report unavailable.'); this.isLoading.set(false);
      }
    });
  }

  onExport(item: DropdownItem): void {
    const filter = { ...this.buildFilter(), exportFormat: item.id as any,
      reportType: this.activeTab() === 'by-product' ? 'SALES_BY_PRODUCT' as any
        : this.activeTab() === 'by-category' ? 'SALES_BY_CATEGORY' as any
        : this.activeTab() === 'by-channel' ? 'SALES_BY_CHANNEL' as any
        : 'SALES_BY_DATE' as any
    };
    this.analyticsService.runAdHoc(filter).pipe(takeUntil(this.destroy$)).subscribe({
      next: exec => { if (exec.downloadUrl) { window.open(exec.downloadUrl, '_blank'); this.alertService.success('Export Ready', exec.fileName ?? 'File ready'); } },
      error: () => this.alertService.error('Export Failed', 'Could not generate export.')
    });
  }

  private buildFilter(): ReportFilter {
    return {
      reportType: 'SALES_OVERVIEW',
      datePreset: this.datePreset() !== 'CUSTOM' ? (this.datePreset() as any) : undefined,
      dateFrom: this.datePreset() === 'CUSTOM' ? this.dateFrom() : undefined,
      dateTo: this.datePreset() === 'CUSTOM' ? this.dateTo() : undefined,
      groupBy: this.groupBy()
    };
  }

  private renderChart(): void {
    const trend = this.overview()?.trend;
    if (!trend || !this.trendChartRef) return;
    if (this.trendChart) this.trendChart.destroy();
    const ctx = this.trendChartRef.nativeElement.getContext('2d');
    this.trendChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: trend.map(p => p.period),
        datasets: [
          { label: 'Revenue', data: trend.map(p => p.revenue), backgroundColor: 'rgba(99,102,241,0.75)', borderRadius: 5, yAxisID: 'y' },
          { label: 'Orders', data: trend.map(p => p.orders), type: 'line', borderColor: '#10b981', backgroundColor: 'transparent', borderWidth: 2, tension: 0.3, yAxisID: 'y1', pointRadius: 3 }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: true, position: 'top' } },
        scales: {
          x: { grid: { display: false }, ticks: { maxTicksLimit: 12, font: { size: 10 } } },
          y: { position: 'left', grid: { color: 'rgba(0,0,0,0.04)' }, ticks: { callback: (v: number) => '$' + (v >= 1000 ? (v/1000).toFixed(0)+'k' : v) } },
          y1: { position: 'right', grid: { display: false }, ticks: { font: { size: 10 } } }
        }
      }
    });
  }

  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(v);
  }
  growthClass(g: number | null | undefined): string {
    if (g == null) return 'bg-gray-100 text-gray-600';
    return g >= 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700';
  }
}
