import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { ProcurementRequest, Supplier } from '../../../core/models/suppliers/suppliers.model';
import { SupplierApiService } from '../../../core/services/suppliers/suppliers.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-procurement',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './procurement.html'
})
export class Procurement implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private supplierSvc = inject(SupplierApiService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  showModal = signal(false);
  isSaving = signal(false);
  isLoading = signal(true);
  searchQuery = signal('');
  filterStatus = signal('');
  filterPriority = signal('');

  requests = signal<ProcurementRequest[]>([]);
  suppliers = signal<Supplier[]>([]);
  editingRequest = signal<ProcurementRequest | null>(null);
  requestForm!: FormGroup;

  statusOptions = ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ORDERED', 'COMPLETED', 'CANCELLED'];
  priorityOptions = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

  displayStats = computed(() => {
    const all = this.requests();
    return [
      { label: 'Total Requests', value: all.length, icon: 'clipboard2', bgColor: 'bg-blue-100', iconColor: 'text-blue-600' },
      { label: 'Pending Approval', value: all.filter(r => r.status === 'PENDING_APPROVAL').length, icon: 'hourglass-split', bgColor: 'bg-amber-100', iconColor: 'text-amber-600', filter: 'PENDING_APPROVAL' },
      { label: 'Approved', value: all.filter(r => r.status === 'APPROVED').length, icon: 'check-circle', bgColor: 'bg-green-100', iconColor: 'text-green-600', filter: 'APPROVED' },
      { label: 'Urgent', value: all.filter(r => r.priority === 'URGENT').length, icon: 'exclamation-triangle', bgColor: 'bg-red-100', iconColor: 'text-red-600' }
    ];
  });

  filteredRequests = computed(() => {
    let result = this.requests();
    const query = this.searchQuery().toLowerCase();
    if (query) { result = result.filter(r => r.requestNumber?.toLowerCase().includes(query) || r.title?.toLowerCase().includes(query) || r.supplierName?.toLowerCase().includes(query)); }
    if (this.filterStatus()) { result = result.filter(r => r.status === this.filterStatus()); }
    if (this.filterPriority()) { result = result.filter(r => r.priority === this.filterPriority()); }
    return result;
  });

  constructor() { this.initForm(); }
  ngOnInit() { this.loadData(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  private loadData() {
    this.isLoading.set(true);
    this.supplierSvc.getProcurementRequests().pipe(takeUntil(this.destroy$)).subscribe(res => { this.requests.set(res?.content || []); this.isLoading.set(false); });
    this.supplierSvc.getActiveSuppliers().pipe(takeUntil(this.destroy$)).subscribe(data => this.suppliers.set(data || []));
  }

  private initForm() {
    this.requestForm = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      priority: ['MEDIUM'],
      supplierId: [''],
      supplierName: [''],
      estimatedCost: [null],
      currency: ['USD'],
      requiredByDate: [''],
      notes: ['']
    });
  }

  onSupplierChange() {
    const sid = this.requestForm.get('supplierId')?.value;
    const supplier = this.suppliers().find(s => s.id === sid);
    if (supplier) this.requestForm.patchValue({ supplierName: supplier.name });
  }

  openModal(request?: ProcurementRequest) {
    this.editingRequest.set(request || null);
    if (request) { this.requestForm.patchValue({ ...request, requiredByDate: request.requiredByDate?.split('T')[0] }); }
    else { this.requestForm.reset({ priority: 'MEDIUM', currency: 'USD' }); }
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); this.editingRequest.set(null); this.requestForm.reset(); }

  saveRequest() {
    if (this.requestForm.invalid) { this.requestForm.markAllAsTouched(); return; }
    this.isSaving.set(true);
    const val = this.requestForm.value;
    const action = this.editingRequest()
      ? this.supplierSvc.updateProcurementRequest(this.editingRequest()!.id, val)
      : this.supplierSvc.createProcurementRequest(val);
    action.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.isSaving.set(false); this.closeModal(); this.loadData(); this.alertSvc.success(this.editingRequest() ? 'Request updated' : 'Request created'); },
      error: () => { this.isSaving.set(false); this.alertSvc.error('Failed to save'); }
    });
  }

  deleteRequest(request: ProcurementRequest) {
    if (request.status !== 'DRAFT') { this.alertSvc.warning('Cannot delete', 'Only draft requests can be deleted.'); return; }
    this.alertSvc.confirm({ title: 'Delete Request', message: `Delete "${request.title}"?`, confirmLabel: 'Delete', danger: true }).then(ok => {
      if (!ok) return;
      this.supplierSvc.deleteProcurementRequest(request.id).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.loadData(); this.alertSvc.success('Request deleted'); }, error: () => this.alertSvc.error('Failed to delete') });
    });
  }

  approveRequest(request: ProcurementRequest) {
    this.supplierSvc.approveProcurementRequest(request.id).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.loadData(); this.alertSvc.success('Request approved'); }, error: () => this.alertSvc.error('Failed to approve') });
  }

  rejectRequest(request: ProcurementRequest) {
    this.alertSvc.confirm({ title: 'Reject Request', message: `Reject "${request.title}"?`, confirmLabel: 'Reject', danger: true }).then(ok => {
      if (!ok) return;
      this.supplierSvc.rejectProcurementRequest(request.id, 'Rejected by admin').pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.loadData(); this.alertSvc.success('Request rejected'); }, error: () => this.alertSvc.error('Failed to reject') });
    });
  }

  getStatusBadge(status: string): string {
    const map: Record<string, string> = { 'DRAFT': 'bg-gray-100 text-gray-800', 'PENDING_APPROVAL': 'bg-amber-100 text-amber-800', 'APPROVED': 'bg-green-100 text-green-800', 'REJECTED': 'bg-red-100 text-red-800', 'ORDERED': 'bg-blue-100 text-blue-800', 'COMPLETED': 'bg-emerald-100 text-emerald-800', 'CANCELLED': 'bg-gray-100 text-gray-500' };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  getPriorityBadge(priority: string): string {
    const map: Record<string, string> = { 'LOW': 'bg-gray-100 text-gray-700', 'MEDIUM': 'bg-blue-100 text-blue-700', 'HIGH': 'bg-orange-100 text-orange-700', 'URGENT': 'bg-red-100 text-red-700' };
    return map[priority] || 'bg-gray-100 text-gray-700';
  }

  getMenuItems(request: ProcurementRequest): DropdownItem[] {
    const items: DropdownItem[] = [];
    if (request.status === 'DRAFT') items.push({ id: 'edit', label: 'Edit', icon: 'pencil' });
    if (request.status === 'PENDING_APPROVAL') { items.push({ id: 'approve', label: 'Approve', icon: 'check-circle' }); items.push({ id: 'reject', label: 'Reject', icon: 'x-circle' }); }
    if (request.status === 'DRAFT') items.push({ id: 'divider', label: '', divider: true }, { id: 'delete', label: 'Delete', icon: 'trash', danger: true });
    return items;
  }

  onAction(item: DropdownItem, request: ProcurementRequest) {
    switch (item.id) {
      case 'edit': this.openModal(request); break;
      case 'approve': this.approveRequest(request); break;
      case 'reject': this.rejectRequest(request); break;
      case 'delete': this.deleteRequest(request); break;
    }
  }

  aiProcurementOptimization() {
    this.alertSvc.info('AI Procurement Optimization', 'AI analyzes spending patterns, supplier performance, and market conditions to recommend procurement optimization strategies.');
  }

  clearFilters() { this.searchQuery.set(''); this.filterStatus.set(''); this.filterPriority.set(''); }

  formatCurrency(value: number, currency: string = 'USD'): string { return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(value || 0); }
  formatDate(dateStr: string): string { if (!dateStr) return '-'; return new Intl.DateTimeFormat('en-US', { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(dateStr)); }
}
