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

@Component({
  selector: 'app-orders-report',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './orders-report.html',
  styleUrl: './orders-report.scss'
})
export class OrdersReportPage implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private statusChart: any;
  readonly Math = Math;

  activeTab = signal<'summary' | 'trend' | 'details'>('summary');
  isLoading = signal(false);
  error = signal<string | null>(null);
  report = signal<OrderReport | null>(null);
  datePreset = signal('THIS_MONTH');

  datePresets = [
    { label: 'Last 30 days', value: 'LAST_30_DAYS' }, { label: 'This month', value: 'THIS_MONTH' },
    { label: 'Last month', value: 'LAST_MONTH' }, { label: 'This quarter', value: 'THIS_QUARTER' },
    { label: 'This year', value: 'THIS_YEAR' }
  ];
  exportItems: DropdownItem[] = [
    { id: 'EXCEL', label: 'Export Excel', icon: 'filetype-xlsx' },
    { id: 'CSV',   label: 'Export CSV',   icon: 'filetype-csv'  },
    { id: 'PDF',   label: 'Export PDF',   icon: 'filetype-pdf'  }
  ];
  tabs = [
    { id: 'summary' as const, label: 'Summary', icon: 'clipboard-data' },
    { id: 'trend' as const, label: 'Trend', icon: 'graph-up' },
    { id: 'details' as const, label: 'Order Details', icon: 'list-ul' }
  ];

  ngOnInit() { this.loadData(); }
  ngAfterViewInit() {}
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); if (this.statusChart) this.statusChart.destroy(); }
  setTab(t: 'summary' | 'trend' | 'details') { this.activeTab.set(t); if (t === 'summary') setTimeout(() => this.renderChart(), 80); }

  loadData() {
    this.isLoading.set(true); this.error.set(null);
    const filter: ReportFilter = { reportType: 'ORDER_SUMMARY', datePreset: this.datePreset() as any };
    this.svc.getOrderReport(filter).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.report.set(data); this.isLoading.set(false); if (this.activeTab() === 'summary') setTimeout(() => this.renderChart(), 80); },
      error: () => { this.error.set('Failed to load order data.'); this.alertSvc.error('Load Failed', 'Order report unavailable.'); this.isLoading.set(false); }
    });
  }

  onExport(item: DropdownItem) {
    this.svc.runAdHoc({ reportType: 'ORDER_SUMMARY', exportFormat: item.id as any, datePreset: this.datePreset() as any })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: exec => { if (exec.downloadUrl) { window.open(exec.downloadUrl, '_blank'); this.alertSvc.success('Export Ready', exec.fileName ?? ''); } },
        error: () => this.alertSvc.error('Export Failed', 'Could not generate export.')
      });
  }

  private renderChart() {
    const r = this.report();
    if (!r?.ordersByStatus || !this.statusChartRef) return;
    if (this.statusChart) this.statusChart.destroy();
    const ctx = this.statusChartRef.nativeElement.getContext('2d');
    const entries = Object.entries(r.ordersByStatus).sort((a,b) => (b[1] as number) - (a[1] as number));
    this.statusChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: entries.map(([k]) => k),
        datasets: [{ data: entries.map(([,v]) => v),
          backgroundColor: ['#6366f1','#10b981','#f59e0b','#ef4444','#8b5cf6','#06b6d4','#ec4899','#f97316','#84cc16','#14b8a6'],
          borderWidth: 0 }]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } }, cutout: '55%' }
    });
  }

  sourceEntries(): [string, number][] {
    const r = this.report();
    if (!r?.ordersBySource) return [];
    return Object.entries(r.ordersBySource).sort((a, b) => (b[1] as number) - (a[1] as number));
  }
  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(v);
  }
  rateClass(pct: number, lowerBetter = false): string {
    if (lowerBetter) return pct <= 5 ? 'text-green-600' : pct <= 15 ? 'text-yellow-600' : 'text-red-600';
    return pct >= 85 ? 'text-green-600' : pct >= 70 ? 'text-yellow-600' : 'text-red-600';
  }
  statusBadge(s: string): string {
    const m: Record<string,string> = {
      DELIVERED:'bg-green-100 text-green-800', SHIPPED:'bg-purple-100 text-purple-800',
      CONFIRMED:'bg-blue-100 text-blue-800', PROCESSING:'bg-indigo-100 text-indigo-800',
      CANCELLED:'bg-red-100 text-red-800', REFUNDED:'bg-gray-100 text-gray-800',
      PENDING_PAYMENT:'bg-yellow-100 text-yellow-800', ON_HOLD:'bg-orange-100 text-orange-800'
    };
    return m[s] ?? 'bg-gray-100 text-gray-800';
  }
}
