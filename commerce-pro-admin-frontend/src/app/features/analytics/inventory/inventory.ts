import { Component, OnInit, OnDestroy, inject, signal, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Observable, Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics/analytics.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { InventoryReport, ReportFilter } from '../../../core/models/analytics/analytics.model';

declare var Chart: any;
type InvTab = 'stock-value' | 'movement' | 'ageing';

@Component({
  selector: 'app-inventory-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AlertComponent, Dropdown],
  templateUrl: './inventory.html',
  styleUrl: './inventory.scss'
})
export class Inventory implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  private svc = inject(AnalyticsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private statusChart: any;
  readonly Math = Math;

  activeTab = signal<InvTab>('stock-value');
  isLoading = signal(false);
  error = signal<string | null>(null);
  report = signal<InventoryReport | null>(null);
  warehouseFilter = signal('');
  datePreset = signal('THIS_MONTH');
  searchQuery = signal('');

  datePresets = [
    { label: 'Last 7 days', value: 'LAST_7_DAYS' }, { label: 'This month', value: 'THIS_MONTH' },
    { label: 'Last month', value: 'LAST_MONTH' }, { label: 'This quarter', value: 'THIS_QUARTER' },
    { label: 'This year', value: 'THIS_YEAR' }
  ];
  exportItems: DropdownItem[] = [
    { id: 'EXCEL', label: 'Export Excel', icon: 'filetype-xlsx' },
    { id: 'CSV',   label: 'Export CSV',   icon: 'filetype-csv'  },
    { id: 'PDF',   label: 'Export PDF',   icon: 'filetype-pdf'  }
  ];
  tabs = [
    { id: 'stock-value' as InvTab, label: 'Stock Value', icon: 'currency-dollar' },
    { id: 'movement' as InvTab, label: 'Movements', icon: 'arrow-left-right' },
    { id: 'ageing' as InvTab, label: 'Ageing', icon: 'clock-history' }
  ];
  ageingColors: Record<string, string> = {
    '0-30': 'bg-green-100 text-green-800', '31-60': 'bg-yellow-100 text-yellow-800',
    '61-90': 'bg-orange-100 text-orange-800', '90+': 'bg-red-100 text-red-800'
  };
  movementColors: Record<string, string> = {
    'IN': 'bg-green-100 text-green-800', 'OUT': 'bg-red-100 text-red-800',
    'ADJUSTMENT': 'bg-blue-100 text-blue-800', 'RETURN': 'bg-purple-100 text-purple-800',
    'TRANSFER_IN': 'bg-teal-100 text-teal-800', 'TRANSFER_OUT': 'bg-orange-100 text-orange-800',
    'DAMAGED': 'bg-gray-100 text-gray-800'
  };

  ngOnInit() { this.loadData(); }
  ngAfterViewInit() {}
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); if (this.statusChart) this.statusChart.destroy(); }

  setTab(t: InvTab) { this.activeTab.set(t); this.loadData(); }

  loadData() {
    this.isLoading.set(true); this.error.set(null);
    const filter: ReportFilter = {
      reportType: this.activeTab() === 'stock-value' ? 'INVENTORY_STOCK_VALUE'
        : this.activeTab() === 'movement' ? 'INVENTORY_MOVEMENT' : 'INVENTORY_AGEING',
      datePreset: this.datePreset() as any
    };
    const obs: Observable<any> = this.activeTab() === 'stock-value' ? this.svc.getInventoryStockValue(filter)
      : this.activeTab() === 'movement' ? this.svc.getInventoryMovement(filter)
      : this.svc.getInventoryAgeing(filter);
    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.report.set(data); this.isLoading.set(false); setTimeout(() => this.renderChart(), 80); },
      error: () => { this.error.set('Failed to load inventory data.'); this.alertSvc.error('Load Failed', 'Inventory report unavailable.'); this.isLoading.set(false); }
    });
  }

  onExport(item: DropdownItem) {
    const reportType = this.activeTab() === 'stock-value' ? 'INVENTORY_STOCK_VALUE'
      : this.activeTab() === 'movement' ? 'INVENTORY_MOVEMENT' : 'INVENTORY_AGEING';
    this.svc.runAdHoc({ reportType: reportType as any, exportFormat: item.id as any, datePreset: this.datePreset() as any })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: exec => { if (exec.downloadUrl) { window.open(exec.downloadUrl, '_blank'); this.alertSvc.success('Export Ready', exec.fileName ?? ''); } },
        error: () => this.alertSvc.error('Export Failed', 'Could not generate export.')
      });
  }

  private renderChart() {
    const r = this.report();
    if (!r || this.activeTab() !== 'stock-value' || !this.statusChartRef) return;
    if (this.statusChart) this.statusChart.destroy();
    const ctx = this.statusChartRef.nativeElement.getContext('2d');
    this.statusChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['In Stock', 'Low Stock', 'Out of Stock', 'Overstock'],
        datasets: [{ data: [r.inStockSkus, r.lowStockSkus, r.outOfStockSkus, r.overstockSkus],
          backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#6366f1'], borderWidth: 0 }]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, cutout: '65%' }
    });
  }

  filteredStockRows() {
    const rows = this.report()?.stockValueRows ?? [];
    const q = this.searchQuery().toLowerCase();
    if (!q) return rows;
    return rows.filter(r => r.productName.toLowerCase().includes(q) || r.sku.toLowerCase().includes(q));
  }

  filteredMovementRows() {
    const rows = this.report()?.movementRows ?? [];
    const q = this.searchQuery().toLowerCase();
    if (!q) return rows;
    return rows.filter(r => r.productName.toLowerCase().includes(q) || r.movementType.toLowerCase().includes(q));
  }

  filteredAgeingRows() {
    const rows = this.report()?.ageingRows ?? [];
    const q = this.searchQuery().toLowerCase();
    if (!q) return rows;
    return rows.filter(r => r.productName.toLowerCase().includes(q) || r.ageingBucket.toLowerCase().includes(q));
  }

  fmt(v: number): string {
    if (!v && v !== 0) return '$0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(v);
  }
  statusBadge(s: string): string {
    return s === 'IN_STOCK' ? 'bg-green-100 text-green-800' : s === 'LOW_STOCK' ? 'bg-yellow-100 text-yellow-800'
      : s === 'OUT_OF_STOCK' ? 'bg-red-100 text-red-800' : s === 'OVERSTOCK' ? 'bg-indigo-100 text-indigo-800' : 'bg-gray-100 text-gray-800';
  }
  statusLabel(s: string): string {
    return s === 'IN_STOCK' ? 'In Stock' : s === 'LOW_STOCK' ? 'Low Stock' : s === 'OUT_OF_STOCK' ? 'Out of Stock' : s === 'OVERSTOCK' ? 'Overstock' : s;
  }
}
