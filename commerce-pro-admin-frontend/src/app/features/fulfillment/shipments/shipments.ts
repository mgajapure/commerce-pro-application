import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ShipmentService } from '../../../core/services/fulfillment/shipment.service';
import { FulfillmentService } from '../../../core/services/fulfillment/fulfillment.service';
import { ShipmentSummary, ShipmentDetail, ShipmentStats, CreateShipmentRequest, AddTrackingEventRequest, Carrier } from '../../../core/models/fulfillment/fulfillment.model';
import { PageParams } from '../../../core/models/common';
import { AlertService } from '../../../shared/services/alert.service';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiShippingResult } from '../../../core/models/ai/ai.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-shipments',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './shipments.html',
  styleUrl: './shipments.scss'
})
export class Shipments implements OnInit {
  private svc      = inject(ShipmentService);
  private fulfSvc  = inject(FulfillmentService);
  private alertSvc = inject(AlertService);
  private aiSvc    = inject(AiService);
  private destroy$ = new Subject<void>();

  // AI Shipping
  shippingAiResults = signal<Record<string, AiShippingResult>>({});
  shippingAiLoading = signal<string | null>(null);
  showShippingAiModal = signal(false);
  shippingAiModalResult = signal<AiShippingResult | null>(null);

  shipments     = signal<ShipmentSummary[]>([]);
  stats         = signal<ShipmentStats | null>(null);
  carriers      = signal<Carrier[]>([]);
  isLoading     = signal(false);
  totalElements = signal(0);
  totalPages    = signal(0);
  currentPage   = signal(1);

  selectedShipment = signal<ShipmentDetail | null>(null);
  detailLoading    = signal(false);

  searchQuery  = signal('');
  filterStatus = signal('');

  showCreateModal  = signal(false);
  showTrackModal   = signal(false);
  createForm       = signal({ orderId: '', carrierId: '', trackingNumber: '', shippingMethod: 'Standard', notes: '' });
  trackForm        = signal({ status: '', description: '', location: '' });
  submitting       = signal(false);
  formError        = signal<string | null>(null);

  quickFilters = [
    { label: 'All', value: '' },
    { label: 'Label Created', value: 'LABEL_CREATED' },
    { label: 'Picked Up', value: 'PICKED_UP' },
    { label: 'In Transit', value: 'IN_TRANSIT' },
    { label: 'Out for Delivery', value: 'OUT_FOR_DELIVERY' },
    { label: 'Delivered', value: 'DELIVERED' },
    { label: 'Exception', value: 'EXCEPTION' },
  ];

  statuses = ['LABEL_CREATED','PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','ATTEMPTED_DELIVERY','RETURNED_TO_SENDER','LOST','EXCEPTION'];

  displayStats = computed(() => {
    const s = this.stats();
    if (!s) return [];
    return [
      { label: 'Total',       value: s.totalShipments,  icon: 'truck',        bg: 'bg-indigo-100', ic: 'text-indigo-600' },
      { label: 'In Transit',  value: s.inTransit,       icon: 'arrow-right-circle', bg: 'bg-blue-100', ic: 'text-blue-600' },
      { label: 'Delivered',   value: s.delivered,       icon: 'check-circle', bg: 'bg-green-100', ic: 'text-green-600' },
      { label: 'Exceptions',  value: s.exceptions,      icon: 'exclamation-triangle', bg: 'bg-red-100', ic: 'text-red-600' },
    ];
  });

  ngOnInit() { this.loadShipments(); this.svc.getStats().subscribe(s => this.stats.set(s)); this.fulfSvc.getCarriers().subscribe(c => this.carriers.set(c)); }

  loadShipments() {
    this.isLoading.set(true);
    const pp: PageParams = { page: this.currentPage() - 1, size: 20, sort: 'createdAt', direction: 'desc' };
    const f = { search: this.searchQuery() || undefined, status: this.filterStatus() || undefined };
    this.svc.getShipments(f, pp).subscribe({
      next: page => { this.shipments.set(page.content); this.totalElements.set(page.totalElements); this.totalPages.set(page.totalPages); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  openDetail(id: string) {
    this.detailLoading.set(true);
    this.svc.getShipment(id).subscribe(s => { this.selectedShipment.set(s); this.detailLoading.set(false); });
  }
  closeDetail() { this.selectedShipment.set(null); }

  openCreateModal() { this.formError.set(null); this.showCreateModal.set(true); }
  closeCreateModal() { this.showCreateModal.set(false); }

  submitCreate() {
    const f = this.createForm();
    if (!f.orderId.trim()) { this.formError.set('Order ID is required'); return; }
    this.submitting.set(true);
    this.formError.set(null);
    const req: CreateShipmentRequest = { orderId: f.orderId, carrierId: f.carrierId || undefined, trackingNumber: f.trackingNumber || undefined, shippingMethod: f.shippingMethod, notes: f.notes };
    this.svc.createShipment(req).subscribe({
      next: () => {
        this.submitting.set(false); this.closeCreateModal(); this.loadShipments();
        this.svc.getStats().subscribe(s => this.stats.set(s));
        this.alertSvc.success('Shipment created', 'Tracking info synced to the order.');
      },
      error: err => { this.submitting.set(false); this.formError.set(err?.error?.message ?? 'Failed to create shipment'); this.alertSvc.error('Failed to create shipment', err?.error?.message); }
    });
  }

  openTrackModal(shipment: ShipmentDetail) { this.trackForm.set({ status: '', description: '', location: '' }); this.formError.set(null); this.showTrackModal.set(true); }
  closeTrackModal() { this.showTrackModal.set(false); }

  submitTrackEvent() {
    const s = this.selectedShipment();
    if (!s) return;
    const f = this.trackForm();
    if (!f.status || !f.description) { this.formError.set('Status and description are required'); return; }
    this.submitting.set(true);
    const req: AddTrackingEventRequest = { status: f.status, description: f.description, location: f.location || undefined };
    this.svc.addTrackingEvent(s.id, req).subscribe({
      next: updated => {
        this.submitting.set(false); this.selectedShipment.set(updated); this.closeTrackModal(); this.loadShipments();
        this.alertSvc.success('Tracking event added', 'Shipment and order status updated.');
      },
      error: err => { this.submitting.set(false); this.formError.set(err?.error?.message ?? 'Failed to add event'); this.alertSvc.error('Failed to add tracking event'); }
    });
  }

  markDelivered(id: string) {
    this.svc.markDelivered(id).subscribe({
      next: updated => { this.selectedShipment.set(updated); this.loadShipments(); this.alertSvc.success('Marked as delivered', 'Order status updated to Delivered.'); },
      error: () => this.alertSvc.error('Failed to mark as delivered')
    });
  }

  deleteShipment(id: string) {
    this.alertSvc.confirm({
      title: 'Delete Shipment',
      message: 'This will permanently delete the shipment label. Only LABEL_CREATED shipments can be deleted.',
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.svc.deleteShipment(id).subscribe({
        next: () => { this.closeDetail(); this.loadShipments(); this.alertSvc.success('Shipment deleted'); },
        error: () => this.alertSvc.error('Failed to delete shipment', 'Only LABEL_CREATED shipments can be deleted.')
      });
    });
  }

  applyQuickFilter(s: string) { this.filterStatus.set(s); this.currentPage.set(1); this.loadShipments(); }

  getStatusConfig(s: string): { badge: string; label: string; dot: string } {
    const m: Record<string, { badge: string; label: string; dot: string }> = {
      LABEL_CREATED:      { badge: 'bg-gray-100 text-gray-700',    label: 'Label Created',     dot: 'bg-gray-400' },
      PICKED_UP:          { badge: 'bg-blue-100 text-blue-700',    label: 'Picked Up',         dot: 'bg-blue-500' },
      IN_TRANSIT:         { badge: 'bg-indigo-100 text-indigo-700',label: 'In Transit',        dot: 'bg-indigo-500' },
      OUT_FOR_DELIVERY:   { badge: 'bg-purple-100 text-purple-700',label: 'Out for Delivery',  dot: 'bg-purple-500' },
      DELIVERED:          { badge: 'bg-green-100 text-green-700',  label: 'Delivered',         dot: 'bg-green-500' },
      ATTEMPTED_DELIVERY: { badge: 'bg-yellow-100 text-yellow-700',label: 'Attempted',         dot: 'bg-yellow-500' },
      RETURNED_TO_SENDER: { badge: 'bg-orange-100 text-orange-700',label: 'Returned',          dot: 'bg-orange-500' },
      LOST:               { badge: 'bg-red-100 text-red-700',      label: 'Lost',              dot: 'bg-red-500' },
      EXCEPTION:          { badge: 'bg-red-100 text-red-800',      label: 'Exception',         dot: 'bg-red-600' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', label: s, dot: 'bg-gray-400' };
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }
  formatDateShort(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso));
  }

  setOrderId(id: any){
    this.createForm.update(f => ({...f, orderId: id}));
  }
  setCarrierId(id: any){
    this.createForm.update(f => ({...f, carrierId: id}));
  }
  setTrackingNumber(trackingNumber: any){
    this.createForm.update(f => ({...f, trackingNumber: trackingNumber}));
  }

  setShippingMethod(shippingMethod: any){
    this.createForm.update(f => ({...f, shippingMethod: shippingMethod}));
  }

  setTrackFormStatus(status: any){
    this.trackForm.update(f => ({...f, status: status}))
  }

  setTrackFormDescription(description: any){
    this.trackForm.update(f => ({...f, description: description}))
  }

  setTrackFormLocation(location: any){
    this.trackForm.update(f => ({...f, location: location}))
  }

  // ─── AI Shipping Optimisation ─────────────────────────────────────────────

  optimiseShipping(orderId: string): void {
    this.shippingAiLoading.set(orderId);
    this.aiSvc.optimiseShipping(orderId).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => {
        this.shippingAiResults.update(m => ({ ...m, [orderId]: r }));
        this.shippingAiModalResult.set(r);
        this.showShippingAiModal.set(true);
        this.shippingAiLoading.set(null);
      },
      error: () => this.shippingAiLoading.set(null)
    });
  }

  getShippingAi(orderId: string): AiShippingResult | null { return this.shippingAiResults()[orderId] ?? null; }
  fmtUsd(n: number): string { return '$' + (n ?? 0).toFixed(2); }
}
