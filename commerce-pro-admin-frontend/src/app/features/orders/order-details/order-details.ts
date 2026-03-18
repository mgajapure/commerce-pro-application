import { Component, signal, computed, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { OrderService } from '../../../core/services/order/order.service';
import { OrderResponse } from '../../../core/models/order/order.model';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, Dropdown],
  templateUrl: './order-details.html',
  styleUrl: './order-details.scss'
})
export class OrderDetails implements OnInit, OnDestroy {
  private route        = inject(ActivatedRoute);
  private router       = inject(Router);
  private orderService = inject(OrderService);
  private destroy$     = new Subject<void>();

  order         = signal<OrderResponse | null>(null);
  isLoading     = signal(true);
  actionLoading = signal(false);
  error         = signal<string | null>(null);
  activeTab     = signal<'details' | 'items' | 'timeline'>('details');
  toast         = signal('');

  // Modal state
  showHoldModal     = signal(false);
  showCancelModal   = signal(false);
  showTrackingModal = signal(false);
  holdReason        = signal('');
  cancelReason      = signal('');
  trackingNumber    = signal('');
  carrier           = signal('');

  tabs = [
    { id: 'details',  label: 'Order Details', icon: 'receipt' },
    { id: 'items',    label: 'Line Items',     icon: 'box-seam' },
    { id: 'timeline', label: 'Timeline',       icon: 'clock-history' }
  ];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  load(id: string): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.orderService.getOrderById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: o => { this.order.set(o); this.isLoading.set(false); },
        error: () => { this.error.set('Order not found'); this.isLoading.set(false); }
      });
  }

  reload(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  // ── Dynamic action items ──────────────────────────────────────────────────

  actionItems = computed((): DropdownItem[] => {
    const o = this.order();
    if (!o) return [];
    const items: DropdownItem[] = [
      { id: 'print', label: 'Print Invoice', icon: 'printer' },
      { id: 'email', label: 'Email Customer', icon: 'envelope' },
      { id: 'divider1', label: '', divider: true }
    ];
    if (['DRAFT', 'PENDING_PAYMENT'].includes(o.status))
      items.push({ id: 'confirm', label: 'Confirm Order', icon: 'check-circle' });
    if (o.status === 'CONFIRMED')
      items.push({ id: 'process', label: 'Mark Processing', icon: 'gear' });
    if (o.status === 'PROCESSING')
      items.push({ id: 'ship', label: 'Mark Shipped', icon: 'truck' });
    if (['SHIPPED', 'OUT_FOR_DELIVERY'].includes(o.status))
      items.push({ id: 'deliver', label: 'Mark Delivered', icon: 'house-check' });
    if (!['ON_HOLD', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'CLOSED'].includes(o.status))
      items.push({ id: 'hold', label: 'Place on Hold', icon: 'pause-circle' });
    if (o.status === 'ON_HOLD')
      items.push({ id: 'release', label: 'Release Hold', icon: 'play-circle' });
    if (['DELIVERED', 'REFUNDED'].includes(o.status))
      items.push({ id: 'close', label: 'Close Order', icon: 'lock' });
    if (['SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED'].includes(o.status))
      items.push({ id: 'tracking', label: 'Update Tracking', icon: 'geo-alt' });
    if (o.isCancellable) {
      items.push({ id: 'divider2', label: '', divider: true });
      items.push({ id: 'cancel', label: 'Cancel Order', icon: 'x-circle', danger: true });
    }
    return items;
  });

  // ── Actions ───────────────────────────────────────────────────────────────

  onAction(item: DropdownItem): void {
    const o = this.order();
    if (!o) return;
    this.actionLoading.set(true);

    const done = (msg: string) => {
      this.actionLoading.set(false);
      this.notify(msg);
      this.reload();
    };

    switch (item.id) {
      case 'confirm':  this.orderService.confirmOrder(o.id).subscribe(() => done('Order confirmed')); break;
      case 'process':  this.orderService.markProcessing(o.id).subscribe(() => done('Moved to processing')); break;
      case 'deliver':  this.orderService.markDelivered(o.id).subscribe(() => done('Marked as delivered')); break;
      case 'release':  this.orderService.releaseHold(o.id).subscribe(() => done('Hold released')); break;
      case 'close':    this.orderService.closeOrder(o.id).subscribe(() => done('Order closed')); break;
      case 'hold':     this.actionLoading.set(false); this.showHoldModal.set(true); break;
      case 'cancel':   this.actionLoading.set(false); this.showCancelModal.set(true); break;
      case 'ship':     this.actionLoading.set(false); this.showTrackingModal.set(true); break;
      case 'tracking': this.actionLoading.set(false); this.showTrackingModal.set(true); break;
      default:         this.actionLoading.set(false);
    }
  }

  submitHold(): void {
    const o = this.order();
    if (!o || !this.holdReason()) return;
    this.actionLoading.set(true);
    this.orderService.holdOrder(o.id, this.holdReason()).subscribe(() => {
      this.showHoldModal.set(false);
      this.holdReason.set('');
      this.actionLoading.set(false);
      this.notify('Order placed on hold');
      this.reload();
    });
  }

  submitCancel(): void {
    const o = this.order();
    if (!o || !this.cancelReason()) return;
    this.actionLoading.set(true);
    this.orderService.cancelOrder(o.id, this.cancelReason()).subscribe(() => {
      this.showCancelModal.set(false);
      this.cancelReason.set('');
      this.actionLoading.set(false);
      this.notify('Order cancelled');
      this.reload();
    });
  }

  submitTracking(): void {
    const o = this.order();
    if (!o || !this.trackingNumber() || !this.carrier()) return;
    this.actionLoading.set(true);
    const op$ = o.status === 'PROCESSING'
      ? this.orderService.markShipped(o.id, this.trackingNumber(), this.carrier())
      : this.orderService.updateTracking(o.id, this.trackingNumber(), this.carrier());
    op$.subscribe(() => {
      this.showTrackingModal.set(false);
      this.trackingNumber.set('');
      this.carrier.set('');
      this.actionLoading.set(false);
      this.notify(o.status === 'PROCESSING' ? 'Order marked as shipped' : 'Tracking updated');
      this.reload();
    });
  }

  private notify(msg: string): void {
    this.toast.set(msg);
    setTimeout(() => this.toast.set(''), 3500);
  }

  goBack(): void { this.router.navigate(['/orders/all']); }

  // ── Display helpers ───────────────────────────────────────────────────────

  getStatusConfig(s: string): { badge: string; label: string } {
    const m: Record<string, { badge: string; label: string }> = {
      DRAFT:           { badge: 'bg-gray-100 text-gray-700',    label: 'Draft' },
      PENDING_PAYMENT: { badge: 'bg-yellow-100 text-yellow-800',label: 'Pending Payment' },
      PAYMENT_FAILED:  { badge: 'bg-red-100 text-red-800',      label: 'Payment Failed' },
      CONFIRMED:       { badge: 'bg-blue-100 text-blue-800',    label: 'Confirmed' },
      ON_HOLD:         { badge: 'bg-orange-100 text-orange-800',label: 'On Hold' },
      PROCESSING:      { badge: 'bg-indigo-100 text-indigo-800',label: 'Processing' },
      SHIPPED:         { badge: 'bg-purple-100 text-purple-800',label: 'Shipped' },
      OUT_FOR_DELIVERY:{ badge: 'bg-violet-100 text-violet-800',label: 'Out for Delivery' },
      DELIVERED:       { badge: 'bg-green-100 text-green-800',  label: 'Delivered' },
      CANCELLED:       { badge: 'bg-red-100 text-red-800',      label: 'Cancelled' },
      RETURN_INITIATED:{ badge: 'bg-pink-100 text-pink-800',    label: 'Return Initiated' },
      RETURN_IN_TRANSIT:{ badge:'bg-rose-100 text-rose-800',    label: 'Return in Transit' },
      RETURN_RECEIVED: { badge: 'bg-fuchsia-100 text-fuchsia-800',label: 'Return Received' },
      REFUNDED:        { badge: 'bg-gray-100 text-gray-800',    label: 'Refunded' },
      PARTIALLY_REFUNDED:{ badge:'bg-gray-100 text-gray-700',  label: 'Part. Refunded' },
      CLOSED:          { badge: 'bg-slate-100 text-slate-700',  label: 'Closed' }
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', label: s };
  }

  getTimelineIcon(s: string): { icon: string; color: string } {
    const m: Record<string, { icon: string; color: string }> = {
      DRAFT:            { icon: 'file-earmark-text', color: 'bg-gray-100 text-gray-600' },
      PENDING_PAYMENT:  { icon: 'credit-card',       color: 'bg-yellow-100 text-yellow-600' },
      CONFIRMED:        { icon: 'check-circle',      color: 'bg-blue-100 text-blue-600' },
      ON_HOLD:          { icon: 'pause-circle',      color: 'bg-orange-100 text-orange-600' },
      PROCESSING:       { icon: 'gear',              color: 'bg-indigo-100 text-indigo-600' },
      SHIPPED:          { icon: 'truck',             color: 'bg-purple-100 text-purple-600' },
      DELIVERED:        { icon: 'house-check',       color: 'bg-green-100 text-green-600' },
      CANCELLED:        { icon: 'x-circle',          color: 'bg-red-100 text-red-600' },
      REFUNDED:         { icon: 'arrow-counterclockwise', color: 'bg-gray-100 text-gray-600' },
      CLOSED:           { icon: 'lock',              color: 'bg-slate-100 text-slate-600' }
    };
    return m[s] ?? { icon: 'circle', color: 'bg-gray-100 text-gray-600' };
  }

  formatDate(iso: string): string {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  getTimeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const h = Math.floor(diff / 3600000), d = Math.floor(diff / 86400000);
    if (h < 1)  return 'Just now';
    if (h < 24) return `${h}h ago`;
    return `${d}d ago`;
  }

  setActiveTab(tabId: any){
    this.activeTab.set(tabId);
  }
}
