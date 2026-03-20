import { Component, OnInit, OnDestroy, inject, signal, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { ReturnReport, ReportFilter } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;

@Component({
  selector: 'app-returns',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './returns.html',
  styleUrl: './returns.scss'
})
export class ReturnsReportPage implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('reasonChart') reasonChartRef!: ElementRef<HTMLCanvasElement>;
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private reasonChart: any;
  readonly Math = Math;

  activeTab = signal<'summary' | 'products' | 'details'>('summary');
  isLoading = signal(false);
  error = signal<string | null>(null);
  report = signal<ReturnReport | null>(null);
  datePreset = signal('THIS_MONTH');

  datePresets = [
    { label: 'Last 30 days', value: 'LAST_30_DAYS' }, { label: 'This month', value: 'THIS_MONTH' },
    { label: 'Last month', value: 'LAST_MONTH' }, { label: 'This quarter', value: 'THIS_QUARTER' }
  ];
  exportItems: DropdownItem[] = [
    { id: 'EXCEL', label: 'Export Excel', icon: 'filetype-xlsx' }, { id: 'CSV', label: 'Export CSV', icon: 'filetype-csv' }
  ];
  tabs = [
    { id: 'summary' as const, label: 'Summary', icon: 'arrow-return-left' },
    { id: 'products' as const, label: 'By Product', icon: 'box' },
    { id: 'details' as const, label: 'Detail Rows', icon: 'list-ul' }
  ];

  ngOnInit() { this.loadData(); }
  ngAfterViewInit() {}
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); if (this.reasonChart) this.reasonChart.destroy(); }
  setTab(t: 'summary' | 'products' | 'details') { this.activeTab.set(t); if (t === 'summary') setTimeout(() => this.renderChart(), 80); }

  loadData() {
    this.isLoading.set(true); this.error.set(null);
    const filter: ReportFilter = { reportType: 'RETURN_RATE', datePreset: this.datePreset() as any };
    this.svc.getReturnReport(filter).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.report.set(data); this.isLoading.set(false); if (this.activeTab() === 'summary') setTimeout(() => this.renderChart(), 80); },
      error: () => { this.error.set('Failed to load return data.'); this.alertSvc.error('Load Failed', 'Returns report unavailable.'); this.isLoading.set(false); }
    });
  }

  onExport(item: DropdownItem) {
    this.svc.runAdHoc({ reportType: 'RETURN_RATE', exportFormat: item.id as any, datePreset: this.datePreset() as any })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: exec => { if (exec.downloadUrl) { window.open(exec.downloadUrl, '_blank'); this.alertSvc.success('Export Ready', exec.fileName ?? ''); } },
        error: () => this.alertSvc.error('Export Failed', 'Could not generate export.')
      });
  }

  private renderChart() {
    const r = this.report();
    if (!r?.ordersByReason || !this.reasonChartRef) return;
    if (this.reasonChart) this.reasonChart.destroy();
    const ctx = this.reasonChartRef.nativeElement.getContext('2d');
    const entries = Object.entries(r.ordersByReason).sort((a,b) => (b[1] as number) - (a[1] as number)).slice(0, 8);
    this.reasonChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: entries.map(([k]) => k),
        datasets: [{ label: 'Returns', data: entries.map(([,v]) => v),
          backgroundColor: ['#ef4444','#f59e0b','#8b5cf6','#06b6d4','#ec4899','#6366f1','#10b981','#f97316'],
          borderRadius: 5 }]
      },
      options: {
        indexAxis: 'y', responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { x: { grid: { color: 'rgba(0,0,0,0.04)' } }, y: { grid: { display: false } } }
      }
    });
  }

  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(v);
  }
  rateClass(pct: number): string {
    return pct >= 20 ? 'text-red-600' : pct >= 10 ? 'text-yellow-600' : 'text-green-600';
  }
  rateBg(pct: number): string {
    return pct >= 20 ? 'bg-red-100 text-red-800' : pct >= 10 ? 'bg-yellow-100 text-yellow-800' : 'bg-green-100 text-green-800';
  }
}
