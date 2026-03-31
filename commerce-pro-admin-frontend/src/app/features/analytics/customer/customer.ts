import { Component, OnInit, OnDestroy, inject, signal, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Observable, Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { CustomerReport, ReportFilter } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;
type CustTab = 'ltv' | 'cohort' | 'churn';

@Component({
  selector: 'app-customer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './customer.html',
  styleUrl: './customer.scss'
})
export class Customer implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('tierChart') tierChartRef!: ElementRef<HTMLCanvasElement>;
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private tierChart: any;
  readonly Math = Math;

  activeTab = signal<CustTab>('ltv');
  isLoading = signal(false);
  error = signal<string | null>(null);
  report = signal<CustomerReport | null>(null);
  datePreset = signal('THIS_MONTH');
  searchQuery = signal('');

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
    { id: 'ltv' as CustTab, label: 'LTV & Segments', icon: 'people' },
    { id: 'cohort' as CustTab, label: 'Cohort Analysis', icon: 'grid' },
    { id: 'churn' as CustTab, label: 'Churn Risk', icon: 'person-dash' }
  ];
  tierColors: Record<string, string> = {
    'PLATINUM': 'bg-purple-100 text-purple-800', 'GOLD': 'bg-yellow-100 text-yellow-800',
    'SILVER': 'bg-gray-100 text-gray-800', 'REGULAR': 'bg-blue-50 text-blue-700'
  };
  segmentColors: Record<string, string> = {
    'High Value': 'bg-green-100 text-green-800', 'At Risk': 'bg-yellow-100 text-yellow-800',
    'Churned': 'bg-red-100 text-red-800', 'New': 'bg-blue-100 text-blue-800', 'Active': 'bg-emerald-100 text-emerald-800'
  };

  ngOnInit() { this.loadData(); }
  ngAfterViewInit() {}
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); if (this.tierChart) this.tierChart.destroy(); }

  setTab(t: CustTab) { this.activeTab.set(t); this.loadData(); }

  loadData() {
    this.isLoading.set(true); this.error.set(null);
    const filter: ReportFilter = { reportType: 'CUSTOMER_LTV', datePreset: this.datePreset() as any };
    const obs: Observable<any> = this.activeTab() === 'ltv' ? this.svc.getCustomerLtv(filter)
      : this.activeTab() === 'cohort' ? this.svc.getCustomerCohort(filter)
      : this.svc.getCustomerChurn(filter);
    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.report.set(data); this.isLoading.set(false); setTimeout(() => this.renderChart(), 80); },
      error: () => { this.error.set('Failed to load customer data.'); this.alertSvc.error('Load Failed', 'Customer report unavailable.'); this.isLoading.set(false); }
    });
  }

  onExport(item: DropdownItem) {
    this.svc.runAdHoc({ reportType: 'CUSTOMER_LTV', exportFormat: item.id as any, datePreset: this.datePreset() as any })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: exec => { if (exec.downloadUrl) { window.open(exec.downloadUrl, '_blank'); this.alertSvc.success('Export Ready', exec.fileName ?? ''); } },
        error: () => this.alertSvc.error('Export Failed', 'Could not generate export.')
      });
  }

  private renderChart() {
    const r = this.report();
    if (!r?.customersByTier || !this.tierChartRef || this.activeTab() !== 'ltv') return;
    if (this.tierChart) this.tierChart.destroy();
    const ctx = this.tierChartRef.nativeElement.getContext('2d');
    const entries = Object.entries(r.customersByTier);
    this.tierChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: entries.map(([k]) => k),
        datasets: [{ data: entries.map(([, v]) => v), backgroundColor: ['#8b5cf6', '#f59e0b', '#9ca3af', '#60a5fa'], borderWidth: 0 }]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, cutout: '65%' }
    });
  }

  filteredLtvRows() {
    const rows = this.report()?.ltvRows ?? [];
    const q = this.searchQuery().toLowerCase();
    return q ? rows.filter(r => r.customerName.toLowerCase().includes(q) || r.email.toLowerCase().includes(q)) : rows;
  }
  filteredChurnRows() {
    const rows = this.report()?.churnRows ?? [];
    const q = this.searchQuery().toLowerCase();
    return q ? rows.filter(r => r.customerName.toLowerCase().includes(q)) : rows;
  }

  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(v);
  }
  retentionColor(pct: number): string {
    if (pct >= 80) return 'bg-green-500'; if (pct >= 60) return 'bg-yellow-500';
    if (pct >= 40) return 'bg-orange-500'; return 'bg-red-500';
  }
  retentionBg(pct: number): string {
    if (pct >= 80) return 'bg-green-100 text-green-900'; if (pct >= 60) return 'bg-yellow-100 text-yellow-900';
    if (pct >= 40) return 'bg-orange-100 text-orange-900'; return 'bg-red-100 text-red-900';
  }
  churnRiskClass(risk: number): string {
    if (risk >= 75) return 'text-red-600'; if (risk >= 50) return 'text-orange-600'; return 'text-yellow-600';
  }
}
