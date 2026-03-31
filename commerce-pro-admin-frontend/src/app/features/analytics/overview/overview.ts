import { Component, OnInit, OnDestroy, inject, signal, computed, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { AnalyticsDashboard } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;

@Component({
  selector: 'app-analytics-overview',
  standalone: true,
  imports: [CommonModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './overview.html',
  styleUrl: './overview.scss'
})
export class AnalyticsOverview implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('revenueChart') revenueChartRef!: ElementRef<HTMLCanvasElement>;

  private analyticsService = inject(AnalyticsService);
  private alertService = inject(AlertService);
  private destroy$ = new Subject<void>();
  private revenueChart: any;

  readonly Math = Math;
  dashboard = signal<AnalyticsDashboard | null>(null);
  isLoading = signal(true);
  isExporting = signal(false);
  error = signal<string | null>(null);

  exportItems: DropdownItem[] = [
    { id: 'EXCEL', label: 'Export Excel', icon: 'filetype-xlsx' },
    { id: 'CSV',   label: 'Export CSV',   icon: 'filetype-csv'  }
  ];

  kpiCards = computed(() => {
    const d = this.dashboard();
    if (!d) return [];
    return [
      {
        title: 'Revenue MTD',
        value: this.formatCurrency(d.revenueMtd),
        sub: `Today: ${this.formatCurrency(d.revenueToday)}`,
        growth: d.revenueGrowthPct ?? 0,
        icon: 'currency-dollar',
        gradient: 'from-indigo-500 to-purple-600'
      },
      {
        title: 'Orders MTD',
        value: d.ordersMtd.toLocaleString(),
        sub: `Today: ${d.ordersToday}`,
        growth: 0,
        icon: 'cart-check',
        gradient: 'from-emerald-500 to-teal-600'
      },
      {
        title: 'Avg Order Value',
        value: this.formatCurrency(d.aovMtd),
        sub: 'Month to date',
        growth: 0,
        icon: 'receipt',
        gradient: 'from-amber-500 to-orange-600'
      },
      {
        title: 'New Customers',
        value: d.newCustomersMtd.toLocaleString(),
        sub: `Active: ${d.totalActiveCustomers.toLocaleString()}`,
        growth: 0,
        icon: 'person-plus',
        gradient: 'from-rose-500 to-pink-600'
      }
    ];
  });

  performanceMetrics = computed(() => {
    const d = this.dashboard();
    if (!d) return [];
    return [
      { label: 'Fulfilment Rate', value: d.fulfillmentRateMtd, icon: 'check2-circle', color: 'text-green-600', bg: 'bg-green-100', target: 95 },
      { label: 'On-Time Delivery', value: d.onTimeDeliveryRateMtd, icon: 'truck', color: 'text-blue-600', bg: 'bg-blue-100', target: 90 },
      { label: 'Cancellation Rate', value: d.cancellationRateMtd, icon: 'x-circle', color: 'text-red-600', bg: 'bg-red-100', target: 5, lowerBetter: true },
      { label: 'Return Rate', value: d.returnRateMtd, icon: 'arrow-return-left', color: 'text-yellow-600', bg: 'bg-yellow-100', target: 10, lowerBetter: true }
    ];
  });

  inventoryAlerts = computed(() => {
    const d = this.dashboard();
    if (!d) return [];
    const alerts = [];
    if (d.outOfStockSkus > 0) alerts.push({ type: 'error', msg: `${d.outOfStockSkus} SKUs are out of stock` });
    if (d.lowStockSkus > 0) alerts.push({ type: 'warning', msg: `${d.lowStockSkus} SKUs are running low` });
    return alerts;
  });

  ngOnInit(): void {
    this.loadDashboard();
  }

  ngAfterViewInit(): void {
    if (this.dashboard()) this.renderChart();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.revenueChart) this.revenueChart.destroy();
  }

  onExport(item: DropdownItem): void {
    this.isExporting.set(true);
    this.analyticsService.runAdHoc({ reportType: 'SALES_OVERVIEW', exportFormat: item.id as any, datePreset: 'THIS_MONTH' })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: exec => {
          this.isExporting.set(false);
          if (exec.downloadUrl) {
            window.open(exec.downloadUrl, '_blank');
            this.alertService.success('Export Ready', exec.fileName ?? 'File ready');
          }
        },
        error: () => {
          this.isExporting.set(false);
          this.alertService.error('Export Failed', 'Could not generate dashboard export.');
        }
      });
  }

  loadDashboard(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.analyticsService.getDashboard()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: data => {
          this.dashboard.set(data);
          this.isLoading.set(false);
          setTimeout(() => this.renderChart(), 50);
        },
        error: () => {
          this.error.set('Failed to load analytics dashboard. Please check your connection and try again.');
          this.isLoading.set(false);
          this.alertService.error('Load Failed', 'Could not fetch analytics dashboard data.');
        }
      });
  }

  private renderChart(): void {
    const d = this.dashboard();
    if (!d || !this.revenueChartRef) return;
    if (this.revenueChart) this.revenueChart.destroy();
    const ctx = this.revenueChartRef.nativeElement.getContext('2d');
    const labels = d.revenueLast30Days.map(p => p.period);
    const revenues = d.revenueLast30Days.map(p => p.revenue);

    this.revenueChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Revenue',
          data: revenues,
          borderColor: '#6366f1',
          backgroundColor: 'rgba(99,102,241,0.08)',
          fill: true,
          tension: 0.4,
          pointRadius: 0,
          pointHoverRadius: 5,
          borderWidth: 2.5
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, tooltip: { mode: 'index', intersect: false } },
        scales: {
          x: { grid: { display: false }, ticks: { maxTicksLimit: 8, font: { size: 11 } } },
          y: { grid: { color: 'rgba(0,0,0,0.04)' }, ticks: { callback: (v: number) => '$' + (v >= 1000 ? (v/1000).toFixed(0)+'k' : v) } }
        }
      }
    });
  }

  formatCurrency(val: number): string {
    if (!val && val !== 0) return '$0';
    if (val >= 1000000) return '$' + (val/1000000).toFixed(1) + 'M';
    if (val >= 1000) return '$' + (val/1000).toFixed(1) + 'K';
    return '$' + val.toFixed(0);
  }

  getMetricStatus(metric: any): string {
    const v = metric.value;
    const t = metric.target;
    if (metric.lowerBetter) return v <= t ? 'good' : v <= t * 1.5 ? 'warn' : 'bad';
    return v >= t ? 'good' : v >= t * 0.85 ? 'warn' : 'bad';
  }
}
