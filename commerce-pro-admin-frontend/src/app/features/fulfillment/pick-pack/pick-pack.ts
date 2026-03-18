import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FulfillmentService } from '../../../core/services/fulfillment/fulfillment.service';
import { PickListSummary, PickList, UpdatePickItemRequest } from '../../../core/models/fulfillment/fulfillment.model';
import { PageParams } from '../../../core/models/common';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-pick-pack',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './pick-pack.html',
  styleUrl: './pick-pack.scss'
})
export class PickPack implements OnInit {
  private svc      = inject(FulfillmentService);
  private alertSvc = inject(AlertService);

  pickLists     = signal<PickListSummary[]>([]);
  isLoading     = signal(false);
  totalElements = signal(0);
  totalPages    = signal(0);
  currentPage   = signal(1);
  itemsPerPage  = signal(20);

  selectedPickList = signal<PickList | null>(null);
  detailLoading    = signal(false);

  filterStatus = signal('');
  searchQuery  = signal('');

  editedQtys = signal<Record<string, number>>({});
  saving     = signal(false);
  successMsg = signal<string | null>(null);
  errorMsg   = signal<string | null>(null);

  quickFilters = [
    { label: 'All',         value: '' },
    { label: 'Generated',   value: 'GENERATED' },
    { label: 'Assigned',    value: 'ASSIGNED' },
    { label: 'In Progress', value: 'IN_PROGRESS' },
    { label: 'Completed',   value: 'COMPLETED' },
    { label: 'Cancelled',   value: 'CANCELLED' },
  ];

  ngOnInit() { this.loadPickLists(); }

  loadPickLists() {
    this.isLoading.set(true);
    const pp: PageParams = { page: this.currentPage() - 1, size: this.itemsPerPage(), sort: 'createdAt', direction: 'desc' };
    const filter = { search: this.searchQuery() || undefined, status: this.filterStatus() || undefined };
    this.svc.getPickLists(filter, pp).subscribe({
      next: page => { this.pickLists.set(page.content); this.totalElements.set(page.totalElements); this.totalPages.set(page.totalPages); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  openDetail(id: string) {
    this.detailLoading.set(true);
    this.editedQtys.set({});
    this.successMsg.set(null);
    this.errorMsg.set(null);
    this.svc.getPickList(id).subscribe(pl => {
      this.selectedPickList.set(pl);
      const qtys: Record<string, number> = {};
      pl.items.forEach(i => qtys[i.id] = i.quantityPicked);
      this.editedQtys.set(qtys);
      this.detailLoading.set(false);
    });
  }

  closeDetail() { this.selectedPickList.set(null); }

  setQty(itemId: string, val: number) { this.editedQtys.update(q => ({ ...q, [itemId]: val })); }

  markAllPicked() {
    const pl = this.selectedPickList();
    if (!pl) return;
    const qtys: Record<string, number> = {};
    pl.items.forEach(i => qtys[i.id] = i.quantityRequired);
    this.editedQtys.set(qtys);
  }

  savePickItems() {
    const pl = this.selectedPickList();
    if (!pl) return;
    this.saving.set(true);
    this.errorMsg.set(null);
    let done = 0;
    pl.items.forEach(item => {
      const qty = this.editedQtys()[item.id] ?? item.quantityPicked;
      this.svc.updatePickItem(pl.id, item.id, { quantityPicked: qty } as UpdatePickItemRequest).subscribe({
        next: () => { done++; if (done === pl.items.length) { this.saving.set(false); this.successMsg.set('Items saved'); this.alertSvc.success('Progress saved', 'Pick quantities updated successfully.'); this.openDetail(pl.id); } },
        error: () => { this.saving.set(false); this.errorMsg.set('Failed to save some items'); this.alertSvc.error('Failed to save items', 'Some quantities could not be updated.'); }
      });
    });
  }

  startPickList(id: string) { this.svc.startPickList(id).subscribe(() => { this.openDetail(id); this.loadPickLists(); }); }

  completePickList(id: string) {
    this.alertSvc.confirm({
      title: 'Complete Pick List',
      message: 'This will update inventory and order fulfillment status for all picked items.',
      confirmLabel: 'Complete',
      danger: false
    }).then(ok => {
      if (!ok) return;
      this.svc.completePickList(id).subscribe({
        next: () => { this.closeDetail(); this.loadPickLists(); this.alertSvc.success('Pick list completed', 'Inventory and order status updated.'); },
        error: () => this.alertSvc.error('Failed to complete pick list')
      });
    });
  }

  cancelPickList(id: string) {
    this.alertSvc.confirm({
      title: 'Cancel Pick List',
      message: 'Are you sure you want to cancel this pick list? This cannot be undone.',
      confirmLabel: 'Cancel Pick List',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.svc.cancelPickList(id).subscribe({
        next: () => { this.closeDetail(); this.loadPickLists(); this.alertSvc.success('Pick list cancelled'); },
        error: () => this.alertSvc.error('Failed to cancel pick list')
      });
    });
  }

  applyQuickFilter(s: string) { this.filterStatus.set(s); this.currentPage.set(1); this.loadPickLists(); }

  getStatusConfig(s: string) {
    const m: Record<string, { badge: string; label: string; dot: string }> = {
      GENERATED:   { badge: 'bg-gray-100 text-gray-700',    label: 'Generated',   dot: 'bg-gray-400' },
      ASSIGNED:    { badge: 'bg-blue-100 text-blue-700',    label: 'Assigned',    dot: 'bg-blue-500' },
      IN_PROGRESS: { badge: 'bg-yellow-100 text-yellow-700',label: 'In Progress', dot: 'bg-yellow-500' },
      COMPLETED:   { badge: 'bg-green-100 text-green-700',  label: 'Completed',   dot: 'bg-green-500' },
      CANCELLED:   { badge: 'bg-red-100 text-red-700',      label: 'Cancelled',   dot: 'bg-red-500' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', label: s, dot: 'bg-gray-400' };
  }

  canStart(s: string)    { return s === 'GENERATED' || s === 'ASSIGNED'; }
  canComplete(s: string) { return s === 'IN_PROGRESS' || s === 'ASSIGNED'; }
  canCancel(s: string)   { return s === 'GENERATED' || s === 'ASSIGNED'; }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }
}
