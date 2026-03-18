import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ShipmentService } from '../../../core/services/fulfillment/shipment.service';
import { ShipmentSummary, ShipmentStats } from '../../../core/models/fulfillment/fulfillment.model';
import { PageParams } from '../../../core/models/common';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-labels',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './labels.html',
  styleUrl: './labels.scss'
})
export class Labels implements OnInit {
  private svc      = inject(ShipmentService);
  private alertSvc = inject(AlertService);

  shipments     = signal<ShipmentSummary[]>([]);
  stats         = signal<ShipmentStats | null>(null);
  isLoading     = signal(false);
  totalElements = signal(0);
  totalPages    = signal(0);
  currentPage   = signal(1);

  searchQuery  = signal('');
  filterStatus = signal('LABEL_CREATED');
  selected     = signal<string[]>([]);

  filtered = computed(() => {
    const q = this.searchQuery().toLowerCase();
    if (!q) return this.shipments();
    return this.shipments().filter(s =>
      s.shipmentNumber.toLowerCase().includes(q) ||
      s.orderNumber.toLowerCase().includes(q) ||
      (s.trackingNumber ?? '').toLowerCase().includes(q));
  });

  quickFilters = [
    { label: 'Label Created', value: 'LABEL_CREATED' },
    { label: 'All Shipments', value: '' },
  ];

  ngOnInit() { this.load(); this.svc.getStats().subscribe(s => this.stats.set(s)); }

  load() {
    this.isLoading.set(true);
    const pp: PageParams = { page: this.currentPage() - 1, size: 50, sort: 'createdAt', direction: 'desc' };
    this.svc.getShipments({ status: this.filterStatus() || undefined }, pp).subscribe({
      next: page => { this.shipments.set(page.content); this.totalElements.set(page.totalElements); this.totalPages.set(page.totalPages); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  applyFilter(v: string) { this.filterStatus.set(v); this.currentPage.set(1); this.load(); }

  toggleSelect(id: string) { this.selected.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]); }
  isSelected(id: string) { return this.selected().includes(id); }
  isAllSelected() { return this.filtered().length > 0 && this.filtered().every(s => this.isSelected(s.id)); }
  toggleAll() {
    if (this.isAllSelected()) this.selected.set([]);
    else this.selected.set(this.filtered().map(s => s.id));
  }

  printSelected() {
    const count = this.selected().length || this.filtered().length;
    this.alertSvc.info('PDF Labels — Phase 2', `${count} label(s) selected. PDF generation via Apache PDFBox is coming in Phase 2.`);
  }

  getStatusConfig(s: string): { badge: string; label: string } {
    const m: Record<string, { badge: string; label: string }> = {
      LABEL_CREATED:    { badge: 'bg-gray-100 text-gray-700',    label: 'Label Created' },
      PICKED_UP:        { badge: 'bg-blue-100 text-blue-700',    label: 'Picked Up' },
      IN_TRANSIT:       { badge: 'bg-indigo-100 text-indigo-700',label: 'In Transit' },
      OUT_FOR_DELIVERY: { badge: 'bg-purple-100 text-purple-700',label: 'Out for Delivery' },
      DELIVERED:        { badge: 'bg-green-100 text-green-700',  label: 'Delivered' },
      EXCEPTION:        { badge: 'bg-red-100 text-red-700',      label: 'Exception' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', label: s };
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso));
  }
}
