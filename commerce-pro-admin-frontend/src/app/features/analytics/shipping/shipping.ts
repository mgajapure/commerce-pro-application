import { Component, OnInit, OnDestroy, inject, signal, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { ShippingReport, ReportFilter } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;

@Component({
  selector: 'app-shipping',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './shipping.html',
  styleUrl: './shipping.scss'
})
export class ShippingReportPage implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('carrierChart') carrierChartRef!: ElementRef<HTMLCanvasElement>;
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private carrierChart: any;
  readonly Math = Math;

  activeTab = signal<'summary' | 'carriers' | 'details'>('summary');
  isLoading = signal(false);
  error = signal<string | null>(null);
  report = signal<ShippingReport | null>(null);
  datePreset = signal('THIS_MONTH');

  datePresets = [
    { label: 'Last 30 days', value: 'LAST_30_DAYS' }, { label: 'This month', value: 'THIS_MONTH' },
    { label: 'Last month', value: 'LAST_MONTH' }, { label: 'This quarter', value: 'THIS_QUARTER' }
  ];
  exportItems: DropdownItem[] = [
    { id: 'EXCEL', label: 'Export Excel', icon: 'filetype-xlsx' }, { id: 'CSV', label: 'Export CSV', icon: 'filetype-csv' }
  ];
  tabs = [
    { id: 'summary' as const, label: 'Summary', icon: 'truck' },
    { id: 'carriers' as const, label: 'Carrier Performance', icon: 'bar-chart' },
    { id: 'details' as const, label: 'Shipment Details', icon: 'list-ul' }
  ];

  ngOnInit() { this.loadData(); }
  ngAfterViewInit() {}
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); if (this.carrierChart) this.carrierChart.destroy(); }
  setTab(t: 'summary' | 'carriers' | 'details') { this.activeTab.set(t); if (t === 'carriers') setTimeout(() => this.renderChart(), 80); }

  loadData() {
    this.isLoading.set(true); this.error.set(null);
    const filter: ReportFilter = { reportType: 'SHIPPING_ON_TIME_DELIVERY', datePreset: this.datePreset() as any };
    this.svc.getShippingReport(filter).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.report.set(data); this.isLoading.set(false); if (this.activeTab() === 'carriers') setTimeout(() => this.renderChart(), 80); },
      error: () => { this.error.set('Failed to load shipping data.'); this.alertSvc.error('Load Failed', 'Shipping report unavailable.'); this.isLoading.set(false); }
    });
  }

  onExport(item: DropdownItem) {
    this.svc.runAdHoc({ reportType: 'SHIPPING_ON_TIME_DELIVERY', exportFormat: item.id as any, datePreset: this.datePreset() as any })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: exec => { if (exec.downloadUrl) { window.open(exec.downloadUrl, '_blank'); this.alertSvc.success('Export Ready', exec.fileName ?? ''); } },
        error: () => this.alertSvc.error('Export Failed', 'Could not generate export.')
      });
  }

  private renderChart() {
    const r = this.report();
    if (!r?.carrierPerformance?.length || !this.carrierChartRef) return;
    if (this.carrierChart) this.carrierChart.destroy();
    const ctx = this.carrierChartRef.nativeElement.getContext('2d');
    const carriers = r.carrierPerformance.slice(0, 8);
    this.carrierChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: carriers.map(c => c.carrierName),
        datasets: [
          { label: 'On-Time %', data: carriers.map(c => c.onTimeRatePct), backgroundColor: 'rgba(16,185,129,0.75)', borderRadius: 4 },
          { label: 'Total Shipments', data: carriers.map(c => c.totalShipments), backgroundColor: 'rgba(99,102,241,0.5)', borderRadius: 4, yAxisID: 'y1' }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: true, position: 'top' } },
        scales: {
          x: { grid: { display: false } },
          y: { max: 100, ticks: { callback: (v: number) => v + '%' } },
          y1: { position: 'right', grid: { display: false } }
        }
      }
    });
  }

  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(v);
  }
  onTimeBadge(pct: number): string {
    return pct >= 90 ? 'bg-green-100 text-green-800' : pct >= 75 ? 'bg-yellow-100 text-yellow-800' : 'bg-red-100 text-red-800';
  }
  statusBadge(s: string): string {
    const m: Record<string, string> = {
      DELIVERED: 'bg-green-100 text-green-800', IN_TRANSIT: 'bg-blue-100 text-blue-800',
      PICKED_UP: 'bg-indigo-100 text-indigo-800', OUT_FOR_DELIVERY: 'bg-purple-100 text-purple-800',
      LOST: 'bg-red-100 text-red-800', RETURNED_TO_SENDER: 'bg-orange-100 text-orange-800',
      EXCEPTION: 'bg-red-100 text-red-800', LABEL_CREATED: 'bg-gray-100 text-gray-800'
    };
    return m[s] ?? 'bg-gray-100 text-gray-800';
  }
}
