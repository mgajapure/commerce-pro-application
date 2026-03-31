import { Component, OnInit, OnDestroy, inject, signal, computed, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { SalesReport, SalesRow, ReportFilter } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;
type TrafficTab = 'overview' | 'channels';

// Human-friendly labels + brand colours for known order channels
const CHANNEL_META: Record<string, { label: string; color: string; icon: string; badge: string }> = {
  STOREFRONT:  { label: 'Online Store',  color: 'rgba(99,102,241,0.8)',  icon: 'shop',         badge: 'bg-indigo-100 text-indigo-800' },
  MOBILE_APP:  { label: 'Mobile App',   color: 'rgba(16,185,129,0.8)',  icon: 'phone',        badge: 'bg-emerald-100 text-emerald-800' },
  API:         { label: 'API / Headless',color: 'rgba(245,158,11,0.8)',  icon: 'code-slash',   badge: 'bg-amber-100 text-amber-800' },
  MANUAL:      { label: 'Manual Entry', color: 'rgba(139,92,246,0.8)',  icon: 'person-gear',  badge: 'bg-purple-100 text-purple-800' },
  IMPORT:      { label: 'Bulk Import',  color: 'rgba(6,182,212,0.8)',   icon: 'upload',       badge: 'bg-cyan-100 text-cyan-800' },
  MARKETPLACE: { label: 'Marketplace',  color: 'rgba(236,72,153,0.8)',  icon: 'grid',         badge: 'bg-pink-100 text-pink-800' },
};

const CHART_COLORS = [
  'rgba(99,102,241,0.8)', 'rgba(16,185,129,0.8)', 'rgba(245,158,11,0.8)',
  'rgba(139,92,246,0.8)', 'rgba(6,182,212,0.8)',  'rgba(236,72,153,0.8)',
  'rgba(239,68,68,0.8)',  'rgba(249,115,22,0.8)'
];

@Component({
  selector: 'app-traffic',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './traffic.html',
  styleUrl: './traffic.scss'
})
export class Traffic implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('donutChart') donutChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('barChart')   barChartRef!:   ElementRef<HTMLCanvasElement>;

  private svc      = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private donutChart: any;
  private barChart:   any;
  readonly Math = Math;

  activeTab  = signal<TrafficTab>('overview');
  isLoading  = signal(false);
  error      = signal<string | null>(null);
  report     = signal<SalesReport | null>(null);
  datePreset = signal('THIS_MONTH');

  datePresets = [
    { label: 'Today',         value: 'TODAY'        },
    { label: 'Last 7 days',   value: 'LAST_7_DAYS'  },
    { label: 'Last 30 days',  value: 'LAST_30_DAYS' },
    { label: 'This month',    value: 'THIS_MONTH'   },
    { label: 'Last month',    value: 'LAST_MONTH'   },
    { label: 'This quarter',  value: 'THIS_QUARTER' },
    { label: 'This year',     value: 'THIS_YEAR'    }
  ];

  exportItems: DropdownItem[] = [
    { id: 'EXCEL', label: 'Export Excel', icon: 'filetype-xlsx' },
    { id: 'CSV',   label: 'Export CSV',   icon: 'filetype-csv'  }
  ];

  tabs: { id: TrafficTab; label: string; icon: string }[] = [
    { id: 'overview',  label: 'Channel Overview', icon: 'diagram-3'  },
    { id: 'channels',  label: 'Channel Detail',   icon: 'bar-chart-line' }
  ];

  // ── Summary KPIs ──────────────────────────────────────────────────────────
  summaryCards = computed(() => {
    const r = this.report();
    if (!r) return [];
    const topRow = r.rows?.reduce((best, row) =>
      (row.revenue as any > (best?.revenue as any ?? 0)) ? row : best, null as SalesRow | null);
    return [
      {
        label: 'Total Channels',
        value: (r.rows?.length ?? 0).toString(),
        icon:  'diagram-3',
        bg:    'bg-indigo-100',
        ic:    'text-indigo-600'
      },
      {
        label: 'Total Revenue',
        value: this.fmt(r.totalRevenue ?? 0),
        icon:  'currency-dollar',
        bg:    'bg-emerald-100',
        ic:    'text-emerald-600'
      },
      {
        label: 'Total Orders',
        value: (r.totalOrders ?? 0).toLocaleString(),
        icon:  'cart-check',
        bg:    'bg-blue-100',
        ic:    'text-blue-600'
      },
      {
        label: 'Top Channel',
        value: topRow ? channelLabel(topRow.dimension) : '—',
        icon:  'trophy',
        bg:    'bg-yellow-100',
        ic:    'text-yellow-600'
      }
    ];
  });

  // ── Rows sorted by revenue ─────────────────────────────────────────────────
  sortedRows = computed(() =>
    [...(this.report()?.rows ?? [])].sort((a, b) => (b.revenue as any) - (a.revenue as any))
  );

  ngOnInit()        { this.loadData(); }
  ngAfterViewInit() {}
  ngOnDestroy()     {
    this.destroy$.next(); this.destroy$.complete();
    if (this.donutChart) this.donutChart.destroy();
    if (this.barChart)   this.barChart.destroy();
  }

  setTab(t: TrafficTab): void {
    this.activeTab.set(t);
    if (t === 'overview')  setTimeout(() => this.renderDonut(), 80);
    if (t === 'channels')  setTimeout(() => this.renderBar(), 80);
  }

  loadData(): void {
    this.isLoading.set(true);
    this.error.set(null);
    const filter: ReportFilter = { reportType: 'SALES_BY_CHANNEL', datePreset: this.datePreset() as any };
    this.svc.getSalesByChannel(filter).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => {
        this.report.set(data);
        this.isLoading.set(false);
        if (this.activeTab() === 'overview') setTimeout(() => this.renderDonut(), 80);
        if (this.activeTab() === 'channels') setTimeout(() => this.renderBar(), 80);
      },
      error: () => {
        this.error.set('Failed to load channel data.');
        this.alertSvc.error('Load Failed', 'Traffic report unavailable.');
        this.isLoading.set(false);
      }
    });
  }

  onExport(item: DropdownItem): void {
    this.svc.runAdHoc({ reportType: 'SALES_BY_CHANNEL', exportFormat: item.id as any, datePreset: this.datePreset() as any })
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

  private renderDonut(): void {
    const rows = this.report()?.rows;
    if (!rows?.length || !this.donutChartRef) return;
    if (this.donutChart) this.donutChart.destroy();
    const ctx = this.donutChartRef.nativeElement.getContext('2d');
    this.donutChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: rows.map(r => channelLabel(r.dimension)),
        datasets: [{
          data: rows.map(r => r.revenue),
          backgroundColor: rows.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]),
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } },
          tooltip: {
            callbacks: {
              label: (ctx: any) => {
                const total = (ctx.dataset.data as number[]).reduce((s, v) => s + v, 0);
                const pct   = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : 0;
                return ` ${ctx.label}: $${ctx.parsed.toLocaleString()} (${pct}%)`;
              }
            }
          }
        },
        cutout: '60%'
      }
    });
  }

  private renderBar(): void {
    const rows = this.sortedRows();
    if (!rows.length || !this.barChartRef) return;
    if (this.barChart) this.barChart.destroy();
    const ctx = this.barChartRef.nativeElement.getContext('2d');
    this.barChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: rows.map(r => channelLabel(r.dimension)),
        datasets: [
          {
            label: 'Revenue',
            data: rows.map(r => r.revenue),
            backgroundColor: rows.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]),
            borderRadius: 6,
            yAxisID: 'y'
          },
          {
            label: 'Orders',
            data: rows.map(r => r.orders),
            type: 'line',
            borderColor: '#6366f1',
            backgroundColor: 'transparent',
            borderWidth: 2,
            tension: 0.3,
            pointRadius: 5,
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: true, position: 'top' } },
        scales: {
          x: { grid: { display: false } },
          y: {
            position: 'left',
            grid: { color: 'rgba(0,0,0,0.04)' },
            ticks: { callback: (v: number) => '$' + (v >= 1000 ? (v / 1000).toFixed(0) + 'k' : v) }
          },
          y1: { position: 'right', grid: { display: false }, ticks: { font: { size: 10 } } }
        }
      }
    });
  }

  channelBadge(key: string): string {
    return CHANNEL_META[key]?.badge ?? 'bg-gray-100 text-gray-700';
  }
  channelIcon(key: string): string {
    return CHANNEL_META[key]?.icon ?? 'globe';
  }
  channelLabel(key: string): string {
    return channelLabel(key);
  }

  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(v);
  }
}

function channelLabel(key: string): string {
  return CHANNEL_META[key]?.label ?? key;
}
