import { Component, OnInit, OnDestroy, inject, signal, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { OrderReport, ReportFilter } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;
type ConvTab = 'funnel' | 'trend' | 'details';

@Component({
  selector: 'app-conversion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './conversion.html',
  styleUrl: './conversion.scss'
})
export class Conversion implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('funnelChart') funnelChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('trendChart')  trendChartRef!:  ElementRef<HTMLCanvasElement>;

  private svc      = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private funnelChart: any;
  private trendChart:  any;
  readonly Math = Math;

  activeTab  = signal<ConvTab>('funnel');
  isLoading  = signal(false);
  error      = signal<string | null>(null);
  report     = signal<OrderReport | null>(null);
  datePreset = signal('THIS_MONTH');

  datePresets = [
    { label: 'Last 7 days',   value: 'LAST_7_DAYS'   },
    { label: 'Last 30 days',  value: 'LAST_30_DAYS'  },
    { label: 'This month',    value: 'THIS_MONTH'    },
    { label: 'Last month',    value: 'LAST_MONTH'    },
    { label: 'This quarter',  value: 'THIS_QUARTER'  },
    { label: 'This year',     value: 'THIS_YEAR'     }
  ];

  exportItems: DropdownItem[] = [
    { id: 'EXCEL', label: 'Export Excel', icon: 'filetype-xlsx' },
    { id: 'CSV',   label: 'Export CSV',   icon: 'filetype-csv'  }
  ];

  tabs: { id: ConvTab; label: string; icon: string }[] = [
    { id: 'funnel',  label: 'Order Funnel',    icon: 'funnel'        },
    { id: 'trend',   label: 'Trend',           icon: 'graph-up'      },
    { id: 'details', label: 'Order Details',   icon: 'list-ul'       }
  ];

  // ── Funnel stages derived from order statuses ─────────────────────────────
  get funnelStages(): { label: string; count: number; pct: number; color: string; icon: string }[] {
    const r = this.report();
    if (!r) return [];
    const total = r.totalOrders;
    if (total === 0) return [];

    const byStatus = r.ordersByStatus ?? {};
    const confirmed = (byStatus['CONFIRMED'] ?? 0) + (byStatus['PROCESSING'] ?? 0) +
                      (byStatus['SHIPPED'] ?? 0) + (byStatus['DELIVERED'] ?? 0) +
                      (byStatus['PARTIALLY_FULFILLED'] ?? 0);
    const shipped   = (byStatus['SHIPPED'] ?? 0) + (byStatus['DELIVERED'] ?? 0);
    const delivered = byStatus['DELIVERED'] ?? 0;
    const retained  = Math.max(0, delivered - r.returnedOrders);

    return [
      { label: 'Orders Placed',   count: total,     pct: 100,                         color: 'bg-indigo-500',  icon: 'cart-check'       },
      { label: 'Confirmed',       count: confirmed, pct: pct(confirmed, total),        color: 'bg-blue-500',    icon: 'check-circle'     },
      { label: 'Shipped',         count: shipped,   pct: pct(shipped, total),          color: 'bg-purple-500',  icon: 'truck'            },
      { label: 'Delivered',       count: delivered, pct: pct(delivered, total),        color: 'bg-teal-500',    icon: 'box-seam'         },
      { label: 'Retained (No Return)', count: retained, pct: pct(retained, total),     color: 'bg-emerald-500', icon: 'heart-fill'       }
    ];
  }

  // ── Summary KPI cards ─────────────────────────────────────────────────────
  get kpis(): { label: string; value: string; sub: string; badge: string; badgeColor: string }[] {
    const r = this.report();
    if (!r) return [];
    return [
      {
        label: 'Total Orders',
        value: r.totalOrders.toLocaleString(),
        sub:   'in period',
        badge: '',
        badgeColor: ''
      },
      {
        label: 'Fulfillment Rate',
        value: r.fulfillmentRatePct.toFixed(1) + '%',
        sub:   `${r.fulfilledOrders.toLocaleString()} fulfilled`,
        badge: r.fulfillmentRatePct >= 90 ? 'On Target' : r.fulfillmentRatePct >= 70 ? 'Moderate' : 'Below Target',
        badgeColor: r.fulfillmentRatePct >= 90 ? 'bg-green-100 text-green-800' : r.fulfillmentRatePct >= 70 ? 'bg-yellow-100 text-yellow-800' : 'bg-red-100 text-red-800'
      },
      {
        label: 'Cancellation Rate',
        value: r.cancellationRatePct.toFixed(1) + '%',
        sub:   `${r.cancelledOrders.toLocaleString()} cancelled`,
        badge: r.cancellationRatePct <= 5 ? 'Healthy' : r.cancellationRatePct <= 15 ? 'Moderate' : 'High',
        badgeColor: r.cancellationRatePct <= 5 ? 'bg-green-100 text-green-800' : r.cancellationRatePct <= 15 ? 'bg-yellow-100 text-yellow-800' : 'bg-red-100 text-red-800'
      },
      {
        label: 'Return Rate',
        value: r.returnRatePct.toFixed(1) + '%',
        sub:   `${r.returnedOrders.toLocaleString()} returned`,
        badge: r.returnRatePct <= 10 ? 'Healthy' : r.returnRatePct <= 20 ? 'Moderate' : 'High',
        badgeColor: r.returnRatePct <= 10 ? 'bg-green-100 text-green-800' : r.returnRatePct <= 20 ? 'bg-yellow-100 text-yellow-800' : 'bg-red-100 text-red-800'
      }
    ];
  }

  ngOnInit()        { this.loadData(); }
  ngAfterViewInit() {}
  ngOnDestroy()     {
    this.destroy$.next(); this.destroy$.complete();
    if (this.funnelChart) this.funnelChart.destroy();
    if (this.trendChart)  this.trendChart.destroy();
  }

  setTab(t: ConvTab): void {
    this.activeTab.set(t);
    if (t === 'funnel') setTimeout(() => this.renderFunnelChart(), 80);
    if (t === 'trend')  setTimeout(() => this.renderTrendChart(), 80);
  }

  loadData(): void {
    this.isLoading.set(true);
    this.error.set(null);
    const filter: ReportFilter = { reportType: 'ORDER_SUMMARY', datePreset: this.datePreset() as any };
    this.svc.getOrderReport(filter).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => {
        this.report.set(data);
        this.isLoading.set(false);
        if (this.activeTab() === 'funnel') setTimeout(() => this.renderFunnelChart(), 80);
        if (this.activeTab() === 'trend')  setTimeout(() => this.renderTrendChart(), 80);
      },
      error: () => {
        this.error.set('Failed to load conversion data.');
        this.alertSvc.error('Load Failed', 'Conversion report unavailable.');
        this.isLoading.set(false);
      }
    });
  }

  onExport(item: DropdownItem): void {
    this.svc.runAdHoc({ reportType: 'ORDER_SUMMARY', exportFormat: item.id as any, datePreset: this.datePreset() as any })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: exec => {
          if (exec.downloadUrl) {
            window.open(exec.downloadUrl, '_blank');
            this.alertSvc.success('Export Ready', exec.fileName ?? 'File ready');
          }
        },
        error: () => this.alertSvc.error('Export Failed', 'Could not generate export.')
      });
  }

  private renderFunnelChart(): void {
    const stages = this.funnelStages;
    if (!stages.length || !this.funnelChartRef) return;
    if (this.funnelChart) this.funnelChart.destroy();
    const ctx = this.funnelChartRef.nativeElement.getContext('2d');
    this.funnelChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: stages.map(s => s.label),
        datasets: [{
          label: 'Orders',
          data: stages.map(s => s.count),
          backgroundColor: ['rgba(99,102,241,0.8)', 'rgba(59,130,246,0.8)', 'rgba(139,92,246,0.8)', 'rgba(20,184,166,0.8)', 'rgba(16,185,129,0.8)'],
          borderRadius: 6,
          borderSkipped: false
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { color: 'rgba(0,0,0,0.04)' }, ticks: { font: { size: 11 } } },
          y: { grid: { display: false }, ticks: { font: { size: 12 } } }
        }
      }
    });
  }

  private renderTrendChart(): void {
    const trend = this.report()?.trend;
    if (!trend?.length || !this.trendChartRef) return;
    if (this.trendChart) this.trendChart.destroy();
    const ctx = this.trendChartRef.nativeElement.getContext('2d');
    this.trendChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: trend.map(t => t.period),
        datasets: [
          {
            label: 'Total Orders',
            data: trend.map(t => t.totalOrders),
            borderColor: '#6366f1',
            backgroundColor: 'rgba(99,102,241,0.1)',
            fill: true,
            tension: 0.4,
            pointRadius: 3,
            borderWidth: 2
          },
          {
            label: 'Fulfilled',
            data: trend.map(t => t.fulfilledOrders),
            borderColor: '#10b981',
            backgroundColor: 'transparent',
            fill: false,
            tension: 0.4,
            pointRadius: 3,
            borderWidth: 2
          },
          {
            label: 'Cancelled',
            data: trend.map(t => t.cancelledOrders),
            borderColor: '#ef4444',
            backgroundColor: 'transparent',
            fill: false,
            tension: 0.4,
            pointRadius: 3,
            borderWidth: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: true, position: 'top' } },
        scales: {
          x: { grid: { display: false }, ticks: { maxTicksLimit: 12, font: { size: 10 } } },
          y: { grid: { color: 'rgba(0,0,0,0.04)' }, ticks: { font: { size: 11 } } }
        }
      }
    });
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      DELIVERED: 'bg-green-100 text-green-800', SHIPPED: 'bg-purple-100 text-purple-800',
      CONFIRMED: 'bg-blue-100 text-blue-800', PROCESSING: 'bg-indigo-100 text-indigo-800',
      CANCELLED: 'bg-red-100 text-red-800', REFUNDED: 'bg-gray-100 text-gray-800',
      PENDING_PAYMENT: 'bg-yellow-100 text-yellow-800', ON_HOLD: 'bg-orange-100 text-orange-800'
    };
    return m[s] ?? 'bg-gray-100 text-gray-800';
  }

  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(v);
  }

  statusEntries(): [string, number][] {
    const r = this.report();
    if (!r?.ordersByStatus) return [];
    return Object.entries(r.ordersByStatus).sort((a, b) => (b[1] as number) - (a[1] as number));
  }
}

function pct(part: number, total: number): number {
  return total === 0 ? 0 : Math.round((part / total) * 1000) / 10;
}
