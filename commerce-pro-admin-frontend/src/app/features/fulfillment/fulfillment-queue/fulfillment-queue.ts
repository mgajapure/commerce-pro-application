import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FulfillmentService } from '../../../core/services/fulfillment/fulfillment.service';
import { FulfillmentQueueItem, FulfillmentStats, CreatePickListRequest } from '../../../core/models/fulfillment/fulfillment.model';

@Component({
  selector: 'app-fulfillment-queue',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './fulfillment-queue.html',
  styleUrl: './fulfillment-queue.scss'
})
export class FulfillmentQueue implements OnInit {
  private svc = inject(FulfillmentService);

  queue      = signal<FulfillmentQueueItem[]>([]);
  stats      = signal<FulfillmentStats | null>(null);
  isLoading  = signal(false);
  error      = signal<string | null>(null);
  selected   = signal<string[]>([]);

  showGenModal = signal(false);
  generating   = signal(false);
  genSuccess   = signal<string | null>(null);
  genError     = signal<string | null>(null);

  searchQuery  = signal('');
  filterStatus = signal('');

  filtered = computed(() => {
    let items = this.queue();
    const q = this.searchQuery().toLowerCase();
    if (q) items = items.filter(o =>
      o.orderNumber.toLowerCase().includes(q) ||
      o.customerName.toLowerCase().includes(q) ||
      o.customerEmail.toLowerCase().includes(q));
    if (this.filterStatus()) items = items.filter(o => o.status === this.filterStatus());
    return items;
  });

  displayStats = computed(() => {
    const s = this.stats();
    if (!s) return [];
    return [
      { label: 'Ready to Pick',   value: s.queueSize,              icon: 'box-seam',      bg: 'bg-indigo-100',  ic: 'text-indigo-600' },
      { label: 'Generated Lists', value: s.generatedPickLists,     icon: 'list-task',     bg: 'bg-yellow-100',  ic: 'text-yellow-600' },
      { label: 'In Progress',     value: s.inProgressPickLists,    icon: 'person-walking', bg: 'bg-blue-100',   ic: 'text-blue-600' },
      { label: 'In Transit',      value: s.inTransitShipments,     icon: 'truck',         bg: 'bg-purple-100',  ic: 'text-purple-600' },
    ];
  });

  ngOnInit() { this.load(); }

  load() {
    this.isLoading.set(true);
    this.svc.getQueue().subscribe({ next: q => { this.queue.set(q); this.isLoading.set(false); }, error: () => this.isLoading.set(false) });
    this.svc.getStats().subscribe(s => this.stats.set(s));
  }

  toggleSelect(id: string) {
    this.selected.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);
  }
  isSelected(id: string) { return this.selected().includes(id); }
  isAllSelected() { return this.filtered().length > 0 && this.filtered().every(o => this.isSelected(o.orderId)); }
  toggleAll() {
    if (this.isAllSelected()) this.selected.set([]);
    else this.selected.set(this.filtered().map(o => o.orderId));
  }

  openGenModal() { this.showGenModal.set(true); this.genSuccess.set(null); this.genError.set(null); }
  closeGenModal() { this.showGenModal.set(false); }

  generatePickList() {
    const ids = this.selected().length ? this.selected() : this.filtered().map(o => o.orderId);
    if (!ids.length) return;
    this.generating.set(true);
    this.genError.set(null);
    const req: CreatePickListRequest = { orderIds: ids, warehouseName: 'Main Warehouse' };
    this.svc.generatePickList(req).subscribe({
      next: pl => {
        this.generating.set(false);
        this.genSuccess.set(pl.pickListNumber);
        this.selected.set([]);
        this.load();
      },
      error: err => { this.generating.set(false); this.genError.set('Failed to generate pick list'); }
    });
  }

  getFulfillmentBadge(s: string): string {
    const m: Record<string, string> = {
      UNFULFILLED: 'bg-red-100 text-red-700',
      PARTIALLY_FULFILLED: 'bg-yellow-100 text-yellow-700',
      FULFILLED: 'bg-green-100 text-green-700',
    };
    return m[s] ?? 'bg-gray-100 text-gray-700';
  }

  getStatusLabel(s: string): string {
    const m: Record<string, string> = {
      CONFIRMED: 'Confirmed', PROCESSING: 'Processing',
      UNFULFILLED: 'Unfulfilled', PARTIALLY_FULFILLED: 'Partial',
    };
    return m[s] ?? s;
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const h = Math.floor(diff / 3600000), d = Math.floor(diff / 86400000);
    if (h < 24) return `${h}h ago`;
    return `${d}d ago`;
  }
}
