import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ShipmentService } from '../../../core/services/fulfillment/shipment.service';
import { ShipmentSummary, ShipmentDetail } from '../../../core/models/fulfillment/fulfillment.model';

@Component({
  selector: 'app-tracking',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './tracking.html',
  styleUrl: './tracking.scss'
})
export class Tracking implements OnInit {
  private svc = inject(ShipmentService);

  activeShipments = signal<ShipmentSummary[]>([]);
  isLoading       = signal(false);
  selectedDetail  = signal<ShipmentDetail | null>(null);
  detailLoading   = signal(false);

  searchQuery = signal('');
  filterStatus = signal('IN_TRANSIT');

  filtered = computed(() => {
    const q = this.searchQuery().toLowerCase();
    let items = this.activeShipments();
    if (q) items = items.filter(s =>
      s.shipmentNumber.toLowerCase().includes(q) ||
      (s.trackingNumber ?? '').toLowerCase().includes(q) ||
      s.orderNumber.toLowerCase().includes(q) ||
      s.recipientName.toLowerCase().includes(q));
    return items;
  });

  statusGroups = computed(() => {
    const all = this.activeShipments();
    return {
      inTransit:       all.filter(s => s.status === 'IN_TRANSIT').length,
      outForDelivery:  all.filter(s => s.status === 'OUT_FOR_DELIVERY').length,
      exceptions:      all.filter(s => s.status === 'EXCEPTION' || s.status === 'LOST').length,
      attempted:       all.filter(s => s.status === 'ATTEMPTED_DELIVERY').length,
    };
  });

  quickFilters = [
    { label: 'Active',          value: '' },
    { label: 'In Transit',      value: 'IN_TRANSIT' },
    { label: 'Out for Delivery',value: 'OUT_FOR_DELIVERY' },
    { label: 'Exceptions',      value: 'EXCEPTION' },
    { label: 'Attempted',       value: 'ATTEMPTED_DELIVERY' },
    { label: 'Returned',        value: 'RETURNED_TO_SENDER' },
    { label: 'Lost',            value: 'LOST' },
  ];

  ngOnInit() { this.loadActive(); }

  loadActive() {
    this.isLoading.set(true);
    const f = { status: this.filterStatus() || undefined };
    this.svc.getShipments(f, { page: 0, size: 100, sort: 'createdAt', direction: 'desc' }).subscribe({
      next: page => { this.activeShipments.set(page.content); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  openDetail(id: string) {
    this.detailLoading.set(true);
    this.svc.getShipment(id).subscribe(s => { this.selectedDetail.set(s); this.detailLoading.set(false); });
  }
  closeDetail() { this.selectedDetail.set(null); }

  applyFilter(v: string) { this.filterStatus.set(v); this.loadActive(); }

  getStatusConfig(s: string): { badge: string; label: string; dot: string; icon: string } {
    const m: Record<string, { badge: string; label: string; dot: string; icon: string }> = {
      LABEL_CREATED:      { badge: 'bg-gray-100 text-gray-700',    label: 'Label Created',    dot: 'bg-gray-400',   icon: 'tag' },
      PICKED_UP:          { badge: 'bg-blue-100 text-blue-700',    label: 'Picked Up',        dot: 'bg-blue-500',   icon: 'box' },
      IN_TRANSIT:         { badge: 'bg-indigo-100 text-indigo-700',label: 'In Transit',       dot: 'bg-indigo-500', icon: 'truck' },
      OUT_FOR_DELIVERY:   { badge: 'bg-purple-100 text-purple-700',label: 'Out for Delivery', dot: 'bg-purple-500', icon: 'person-walking' },
      DELIVERED:          { badge: 'bg-green-100 text-green-700',  label: 'Delivered',        dot: 'bg-green-500',  icon: 'check-circle' },
      ATTEMPTED_DELIVERY: { badge: 'bg-yellow-100 text-yellow-700',label: 'Attempted',        dot: 'bg-yellow-500', icon: 'arrow-clockwise' },
      RETURNED_TO_SENDER: { badge: 'bg-orange-100 text-orange-700',label: 'Returned',         dot: 'bg-orange-500', icon: 'arrow-return-left' },
      LOST:               { badge: 'bg-red-100 text-red-700',      label: 'Lost',             dot: 'bg-red-500',    icon: 'question-circle' },
      EXCEPTION:          { badge: 'bg-red-100 text-red-800',      label: 'Exception',        dot: 'bg-red-600',    icon: 'exclamation-triangle' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', label: s, dot: 'bg-gray-400', icon: 'dash' };
  }

  isOverdue(s: ShipmentSummary): boolean {
    if (!s.estimatedDeliveryDate || s.status === 'DELIVERED') return false;
    return new Date(s.estimatedDeliveryDate) < new Date();
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }
  formatDateShort(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso));
  }
  daysUntil(iso: string): number {
    return Math.ceil((new Date(iso).getTime() - Date.now()) / 86400000);
  }
}
