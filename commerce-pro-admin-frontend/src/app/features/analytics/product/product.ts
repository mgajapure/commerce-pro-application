import { Component, OnInit, OnDestroy, inject, signal, computed, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { SalesReport, SalesRow, ReportFilter, GroupBy } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;
type ProdTab = 'by-revenue' | 'by-units' | 'top-products';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './product.html',
  styleUrl: './product.scss'
})
export class Product implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('barChart') barChartRef!: ElementRef<HTMLCanvasElement>;

  private svc      = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private barChart: any;
  readonly Math = Math;

  activeTab  = signal<ProdTab>('by-revenue');
  isLoading  = signal(false);
  error      = signal<string | null>(null);
  report     = signal<SalesReport | null>(null);
  datePreset = signal('THIS_MONTH');
  searchQuery = signal('');

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
    { id: 'CSV',   label: 'Export CSV',   icon: 'filetype-csv'  },
    { id: 'PDF',   label: 'Export PDF',   icon: 'filetype-pdf'  },
    { id: 'JSON',  label: 'Export JSON',  icon: 'braces'        }
  ];

  tabs: { id: ProdTab; label: string; icon: string }[] = [
    { id: 'by-revenue',   label: 'By Revenue',    icon: 'currency-dollar' },
    { id: 'by-units',     label: 'By Units Sold', icon: 'box-seam'        },
    { id: 'top-products', label: 'Top Products',  icon: 'trophy'          }
  ];

  // ── Summary KPIs ──────────────────────────────────────────────────────────
  summaryCards = computed(() => {
    const r = this.report();
    if (!r) return [];
    return [
      {
        label:  'Total Revenue',
        value:  this.fmt(r.totalRevenue),
        icon:   'currency-dollar',
        bg:     'bg-indigo-100',
        ic:     'text-indigo-600',
        growth: r.revenueChangePct
      },
      {
        label:  'Total Orders',
        value:  (r.totalOrders ?? 0).toLocaleString(),
        icon:   'cart-check',
        bg:     'bg-blue-100',
        ic:     'text-blue-600',
        growth: r.ordersChangePct
      },
      {
        label:  'Total Units Sold',
        value:  (r.totalUnits ?? 0).toLocaleString(),
        icon:   'box-seam',
        bg:     'bg-emerald-100',
        ic:     'text-emerald-600',
        growth: null
      },
      {
        label:  'Avg Order Value',
        value:  this.fmt(r.averageOrderValue ?? 0),
        icon:   'receipt',
        bg:     'bg-purple-100',
        ic:     'text-purple-600',
        growth: null
      }
    ];
  });

  // ── Filtered / sorted row views ───────────────────────────────────────────
  byRevenue = computed(() => {
    const rows = this.report()?.rows ?? [];
    const q = this.searchQuery().toLowerCase();
    const filtered = q ? rows.filter(r => r.dimension.toLowerCase().includes(q)) : rows;
    return [...filtered].sort((a, b) => (+b.revenue) - (+a.revenue));
  });

  byUnits = computed(() => {
    const rows = this.report()?.rows ?? [];
    const q = this.searchQuery().toLowerCase();
    const filtered = q ? rows.filter(r => r.dimension.toLowerCase().includes(q)) : rows;
    return [...filtered].sort((a, b) => b.units - a.units);
  });

  ngOnInit()        { this.loadData(); }
  ngAfterViewInit() {}
  ngOnDestroy()     { this.destroy$.next(); this.destroy$.complete(); if (this.barChart) this.barChart.destroy(); }

  setTab(t: ProdTab): void {
    this.activeTab.set(t);
    if (t === 'top-products') setTimeout(() => this.renderChart(), 80);
  }

  loadData(): void {
    this.isLoading.set(true);
    this.error.set(null);
    const filter: ReportFilter = { reportType: 'SALES_BY_PRODUCT', datePreset: this.datePreset() as any };
    this.svc.getSalesByProduct(filter).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => {
        this.report.set(data);
        this.isLoading.set(false);
        if (this.activeTab() === 'top-products') setTimeout(() => this.renderChart(), 80);
      },
      error: () => {
        this.error.set('Failed to load product performance data.');
        this.alertSvc.error('Load Failed', 'Product report unavailable.');
        this.isLoading.set(false);
      }
    });
  }

  onExport(item: DropdownItem): void {
    this.svc.runAdHoc({ reportType: 'SALES_BY_PRODUCT', exportFormat: item.id as any, datePreset: this.datePreset() as any })
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

  private renderChart(): void {
    const products = this.report()?.topProducts?.slice(0, 10);
    if (!products?.length || !this.barChartRef) return;
    if (this.barChart) this.barChart.destroy();
    const ctx = this.barChartRef.nativeElement.getContext('2d');
    this.barChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: products.map(p => p.productName.length > 20 ? p.productName.substring(0, 20) + '…' : p.productName),
        datasets: [
          {
            label: 'Revenue',
            data: products.map(p => p.revenue),
            backgroundColor: 'rgba(99,102,241,0.8)',
            borderRadius: 5,
            yAxisID: 'y'
          },
          {
            label: 'Units Sold',
            data: products.map(p => p.unitsSold),
            type: 'line',
            borderColor: '#10b981',
            backgroundColor: 'transparent',
            borderWidth: 2,
            tension: 0.3,
            pointRadius: 4,
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: true, position: 'top' } },
        scales: {
          x: { grid: { display: false }, ticks: { font: { size: 10 }, maxRotation: 35 } },
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

  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(v);
  }
  growthClass(g: number | null | undefined): string {
    if (g == null) return 'bg-gray-100 text-gray-600';
    return g >= 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700';
  }
  revenueOf(r: SalesRow): number {
    return r.revenue as any;
  }
}
