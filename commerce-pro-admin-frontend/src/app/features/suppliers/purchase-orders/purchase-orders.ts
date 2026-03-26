import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { PurchaseOrder, Supplier } from '../../../core/models/suppliers/suppliers.model';
import { SupplierApiService } from '../../../core/services/suppliers/suppliers.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-purchase-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './purchase-orders.html'
})
export class PurchaseOrders implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private supplierSvc = inject(SupplierApiService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  showModal = signal(false);
  showDetailModal = signal(false);
  isSaving = signal(false);
  isLoading = signal(true);
  searchQuery = signal('');
  filterStatus = signal('');

  orders = signal<PurchaseOrder[]>([]);
  suppliers = signal<Supplier[]>([]);
  selectedPO = signal<PurchaseOrder | null>(null);
  editingPO = signal<PurchaseOrder | null>(null);
  poForm!: FormGroup;

  statusOptions = ['DRAFT', 'SUBMITTED', 'CONFIRMED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED'];

  displayStats = computed(() => {
    const all = this.orders();
    return [
      { label: 'Total POs', value: all.length, icon: 'file-earmark-text', bgColor: 'bg-blue-100', iconColor: 'text-blue-600' },
      { label: 'Draft', value: all.filter(o => o.status === 'DRAFT').length, icon: 'pencil-square', bgColor: 'bg-gray-100', iconColor: 'text-gray-600', filter: 'DRAFT' },
      { label: 'Submitted', value: all.filter(o => o.status === 'SUBMITTED').length, icon: 'send', bgColor: 'bg-blue-100', iconColor: 'text-blue-600', filter: 'SUBMITTED' },
      { label: 'Confirmed', value: all.filter(o => o.status === 'CONFIRMED').length, icon: 'check-circle', bgColor: 'bg-amber-100', iconColor: 'text-amber-600', filter: 'CONFIRMED' },
      { label: 'Received', value: all.filter(o => o.status === 'RECEIVED').length, icon: 'box-seam', bgColor: 'bg-green-100', iconColor: 'text-green-600', filter: 'RECEIVED' }
    ];
  });

  filteredOrders = computed(() => {
    let result = this.orders();
    const query = this.searchQuery().toLowerCase();
    if (query) { result = result.filter(o => o.poNumber?.toLowerCase().includes(query) || o.supplierName?.toLowerCase().includes(query)); }
    if (this.filterStatus()) { result = result.filter(o => o.status === this.filterStatus()); }
    return result;
  });

  get items(): FormArray { return this.poForm.get('items') as FormArray; }

  constructor() { this.initForm(); }
  ngOnInit() { this.loadData(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  private loadData() {
    this.isLoading.set(true);
    this.supplierSvc.getPurchaseOrders().pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.orders.set(res?.content || []);
      this.isLoading.set(false);
    });
    this.supplierSvc.getActiveSuppliers().pipe(takeUntil(this.destroy$)).subscribe(data => this.suppliers.set(data || []));
  }

  private initForm() {
    this.poForm = this.fb.group({
      supplierId: ['', Validators.required],
      supplierName: [''],
      expectedDeliveryDate: [''],
      currency: ['USD'],
      notes: [''],
      items: this.fb.array([])
    });
  }

  addItem() {
    this.items.push(this.fb.group({
      productName: ['', Validators.required],
      sku: [''],
      quantityOrdered: [1, [Validators.required, Validators.min(1)]],
      unitCost: [0, [Validators.required, Validators.min(0)]],
      notes: ['']
    }));
  }

  removeItem(index: number) { this.items.removeAt(index); }

  getSubtotal(): number {
    return this.items.controls.reduce((sum, ctrl) => {
      const qty = ctrl.get('quantityOrdered')?.value || 0;
      const cost = ctrl.get('unitCost')?.value || 0;
      return sum + (qty * cost);
    }, 0);
  }

  onSupplierChange() {
    const sid = this.poForm.get('supplierId')?.value;
    const supplier = this.suppliers().find(s => s.id === sid);
    if (supplier) this.poForm.patchValue({ supplierName: supplier.name });
  }

  openModal(po?: PurchaseOrder) {
    this.editingPO.set(po || null);
    this.items.clear();
    if (po) {
      this.poForm.patchValue({ supplierId: po.supplierId, supplierName: po.supplierName, expectedDeliveryDate: po.expectedDeliveryDate?.split('T')[0], currency: po.currency, notes: po.notes });
      po.items?.forEach(item => {
        this.items.push(this.fb.group({ productName: [item.productName, Validators.required], sku: [item.sku], quantityOrdered: [item.quantityOrdered, [Validators.required, Validators.min(1)]], unitCost: [item.unitCost, [Validators.required, Validators.min(0)]], notes: [item.notes] }));
      });
    } else {
      this.poForm.reset({ currency: 'USD' });
      this.addItem();
    }
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); this.editingPO.set(null); this.poForm.reset(); this.items.clear(); }

  viewDetail(po: PurchaseOrder) {
    this.supplierSvc.getPurchaseOrderById(po.id).pipe(takeUntil(this.destroy$)).subscribe(full => {
      this.selectedPO.set(full);
      this.showDetailModal.set(true);
    });
  }

  closeDetail() { this.showDetailModal.set(false); this.selectedPO.set(null); }

  savePO() {
    if (this.poForm.invalid || this.items.length === 0) { this.poForm.markAllAsTouched(); if (this.items.length === 0) this.alertSvc.warning('Add items', 'At least one line item is required.'); return; }
    this.isSaving.set(true);
    const val = this.poForm.value;
    const action = this.editingPO()
      ? this.supplierSvc.updatePurchaseOrder(this.editingPO()!.id, val)
      : this.supplierSvc.createPurchaseOrder(val);
    action.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.isSaving.set(false); this.closeModal(); this.loadData(); this.alertSvc.success(this.editingPO() ? 'PO updated' : 'PO created'); },
      error: () => { this.isSaving.set(false); this.alertSvc.error('Failed to save PO'); }
    });
  }

  deletePO(po: PurchaseOrder) {
    if (po.status !== 'DRAFT') { this.alertSvc.warning('Cannot delete', 'Only draft POs can be deleted.'); return; }
    this.alertSvc.confirm({ title: 'Delete PO', message: `Delete "${po.poNumber}"?`, confirmLabel: 'Delete', danger: true }).then(ok => {
      if (!ok) return;
      this.supplierSvc.deletePurchaseOrder(po.id).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.loadData(); this.alertSvc.success('PO deleted'); }, error: () => this.alertSvc.error('Failed to delete') });
    });
  }

  updateStatus(po: PurchaseOrder, status: string) {
    this.supplierSvc.updatePurchaseOrderStatus(po.id, status).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.loadData(); this.closeDetail(); this.alertSvc.success(`PO status updated to ${status}`); },
      error: () => this.alertSvc.error('Failed to update status')
    });
  }

  receivePO(po: PurchaseOrder) {
    this.supplierSvc.receivePurchaseOrder(po.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.loadData(); this.closeDetail(); this.alertSvc.success('PO marked as received'); },
      error: () => this.alertSvc.error('Failed to receive PO')
    });
  }

  getStatusBadge(status: string): string {
    const map: Record<string, string> = {
      'DRAFT': 'bg-gray-100 text-gray-800', 'SUBMITTED': 'bg-blue-100 text-blue-800', 'CONFIRMED': 'bg-amber-100 text-amber-800',
      'PARTIALLY_RECEIVED': 'bg-orange-100 text-orange-800', 'RECEIVED': 'bg-green-100 text-green-800', 'CANCELLED': 'bg-red-100 text-red-800'
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  getMenuItems(po: PurchaseOrder): DropdownItem[] {
    const items: DropdownItem[] = [{ id: 'view', label: 'View Details', icon: 'eye' }];
    if (po.status === 'DRAFT') items.push({ id: 'edit', label: 'Edit', icon: 'pencil' }, { id: 'submit', label: 'Submit', icon: 'send' });
    if (po.status === 'SUBMITTED') items.push({ id: 'confirm', label: 'Confirm', icon: 'check-circle' });
    if (po.status === 'CONFIRMED') items.push({ id: 'receive', label: 'Mark Received', icon: 'box-seam' });
    if (po.status === 'DRAFT') items.push({ id: 'divider', label: '', divider: true }, { id: 'delete', label: 'Delete', icon: 'trash', danger: true });
    return items;
  }

  onAction(item: DropdownItem, po: PurchaseOrder) {
    switch (item.id) {
      case 'view': this.viewDetail(po); break;
      case 'edit': this.openModal(po); break;
      case 'submit': this.updateStatus(po, 'SUBMITTED'); break;
      case 'confirm': this.updateStatus(po, 'CONFIRMED'); break;
      case 'receive': this.receivePO(po); break;
      case 'delete': this.deletePO(po); break;
    }
  }

  aiReorderSuggestions() {
    this.alertSvc.info('AI Reorder Suggestions', 'AI analyzes inventory levels, sales velocity, and supplier lead times to recommend optimal reorder quantities and timing.');
  }

  clearFilters() { this.searchQuery.set(''); this.filterStatus.set(''); }

  formatCurrency(value: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(value || 0);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    return new Intl.DateTimeFormat('en-US', { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(dateStr));
  }
}
