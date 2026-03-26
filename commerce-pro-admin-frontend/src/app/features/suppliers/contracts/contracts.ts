import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { SupplierContract, Supplier } from '../../../core/models/suppliers/suppliers.model';
import { SupplierApiService } from '../../../core/services/suppliers/suppliers.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-contracts',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './contracts.html'
})
export class Contracts implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private supplierSvc = inject(SupplierApiService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  showModal = signal(false);
  isSaving = signal(false);
  isLoading = signal(true);
  searchQuery = signal('');
  filterStatus = signal('');

  contracts = signal<SupplierContract[]>([]);
  suppliers = signal<Supplier[]>([]);
  editingContract = signal<SupplierContract | null>(null);
  contractForm!: FormGroup;

  statusOptions = ['DRAFT', 'ACTIVE', 'EXPIRED', 'TERMINATED', 'RENEWED'];
  typeOptions = ['FIXED_PRICE', 'TIME_AND_MATERIALS', 'BLANKET', 'FRAMEWORK'];

  displayStats = computed(() => {
    const all = this.contracts();
    const totalValue = all.filter(c => c.status === 'ACTIVE').reduce((sum, c) => sum + (c.totalValue || 0), 0);
    return [
      { label: 'Total Contracts', value: all.length, icon: 'file-earmark-text', bgColor: 'bg-blue-100', iconColor: 'text-blue-600' },
      { label: 'Active', value: all.filter(c => c.status === 'ACTIVE').length, icon: 'check-circle', bgColor: 'bg-green-100', iconColor: 'text-green-600', filter: 'ACTIVE' },
      { label: 'Expiring Soon', value: all.filter(c => { if (!c.endDate || c.status !== 'ACTIVE') return false; const d = new Date(c.endDate).getTime() - Date.now(); return d > 0 && d < 30 * 86400000; }).length, icon: 'exclamation-triangle', bgColor: 'bg-amber-100', iconColor: 'text-amber-600' },
      { label: 'Active Value', value: this.formatCurrency(totalValue), icon: 'currency-dollar', bgColor: 'bg-purple-100', iconColor: 'text-purple-600' }
    ];
  });

  filteredContracts = computed(() => {
    let result = this.contracts();
    const query = this.searchQuery().toLowerCase();
    if (query) { result = result.filter(c => c.contractNumber?.toLowerCase().includes(query) || c.title?.toLowerCase().includes(query) || c.supplierName?.toLowerCase().includes(query)); }
    if (this.filterStatus()) { result = result.filter(c => c.status === this.filterStatus()); }
    return result;
  });

  constructor() { this.initForm(); }
  ngOnInit() { this.loadData(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  private loadData() {
    this.isLoading.set(true);
    this.supplierSvc.getContracts().pipe(takeUntil(this.destroy$)).subscribe(res => { this.contracts.set(res?.content || []); this.isLoading.set(false); });
    this.supplierSvc.getActiveSuppliers().pipe(takeUntil(this.destroy$)).subscribe(data => this.suppliers.set(data || []));
  }

  private initForm() {
    this.contractForm = this.fb.group({
      supplierId: ['', Validators.required],
      supplierName: [''],
      title: ['', Validators.required],
      description: [''],
      contractType: ['FIXED_PRICE'],
      startDate: [''],
      endDate: [''],
      totalValue: [null],
      currency: ['USD'],
      paymentTerms: ['Net 30'],
      autoRenew: [false],
      renewalNoticeDays: [30],
      terms: [''],
      documentUrl: ['']
    });
  }

  onSupplierChange() {
    const sid = this.contractForm.get('supplierId')?.value;
    const supplier = this.suppliers().find(s => s.id === sid);
    if (supplier) this.contractForm.patchValue({ supplierName: supplier.name });
  }

  openModal(contract?: SupplierContract) {
    this.editingContract.set(contract || null);
    if (contract) { this.contractForm.patchValue({ ...contract, startDate: contract.startDate?.split('T')[0], endDate: contract.endDate?.split('T')[0] }); }
    else { this.contractForm.reset({ contractType: 'FIXED_PRICE', currency: 'USD', paymentTerms: 'Net 30', autoRenew: false, renewalNoticeDays: 30 }); }
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); this.editingContract.set(null); this.contractForm.reset(); }

  saveContract() {
    if (this.contractForm.invalid) { this.contractForm.markAllAsTouched(); return; }
    this.isSaving.set(true);
    const val = this.contractForm.value;
    const action = this.editingContract()
      ? this.supplierSvc.updateContract(this.editingContract()!.id, val)
      : this.supplierSvc.createContract(val);
    action.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.isSaving.set(false); this.closeModal(); this.loadData(); this.alertSvc.success(this.editingContract() ? 'Contract updated' : 'Contract created'); },
      error: () => { this.isSaving.set(false); this.alertSvc.error('Failed to save'); }
    });
  }

  deleteContract(contract: SupplierContract) {
    if (contract.status !== 'DRAFT') { this.alertSvc.warning('Cannot delete', 'Only draft contracts can be deleted.'); return; }
    this.alertSvc.confirm({ title: 'Delete Contract', message: `Delete "${contract.title}"?`, confirmLabel: 'Delete', danger: true }).then(ok => {
      if (!ok) return;
      this.supplierSvc.deleteContract(contract.id).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.loadData(); this.closeModal(); this.alertSvc.success('Contract deleted'); }, error: () => this.alertSvc.error('Failed to delete') });
    });
  }

  activateContract(contract: SupplierContract) {
    this.supplierSvc.activateContract(contract.id).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.loadData(); this.alertSvc.success('Contract activated'); }, error: () => this.alertSvc.error('Failed to activate') });
  }

  terminateContract(contract: SupplierContract) {
    this.alertSvc.confirm({ title: 'Terminate Contract', message: `Terminate "${contract.title}"?`, confirmLabel: 'Terminate', danger: true }).then(ok => {
      if (!ok) return;
      this.supplierSvc.terminateContract(contract.id).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.loadData(); this.alertSvc.success('Contract terminated'); }, error: () => this.alertSvc.error('Failed to terminate') });
    });
  }

  getStatusBadge(status: string): string {
    const map: Record<string, string> = { 'DRAFT': 'bg-gray-100 text-gray-800', 'ACTIVE': 'bg-green-100 text-green-800', 'EXPIRED': 'bg-red-100 text-red-800', 'TERMINATED': 'bg-gray-100 text-gray-500', 'RENEWED': 'bg-blue-100 text-blue-800' };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  getMenuItems(contract: SupplierContract): DropdownItem[] {
    const items: DropdownItem[] = [{ id: 'edit', label: 'Edit', icon: 'pencil' }];
    if (contract.status === 'DRAFT') items.push({ id: 'activate', label: 'Activate', icon: 'check-circle' });
    if (contract.status === 'ACTIVE') items.push({ id: 'terminate', label: 'Terminate', icon: 'x-circle' });
    if (contract.documentUrl) items.push({ id: 'document', label: 'View Document', icon: 'file-earmark-pdf' });
    if (contract.status === 'DRAFT') items.push({ id: 'divider', label: '', divider: true }, { id: 'delete', label: 'Delete', icon: 'trash', danger: true });
    return items;
  }

  onAction(item: DropdownItem, contract: SupplierContract) {
    switch (item.id) {
      case 'edit': this.openModal(contract); break;
      case 'activate': this.activateContract(contract); break;
      case 'terminate': this.terminateContract(contract); break;
      case 'document': if (contract.documentUrl) window.open(contract.documentUrl, '_blank'); break;
      case 'delete': this.deleteContract(contract); break;
    }
  }

  aiContractAnalysis() {
    this.alertSvc.info('AI Contract Analysis', 'AI reviews contract terms, identifies risks, suggests optimizations, and flags upcoming renewals or expiring agreements.');
  }

  clearFilters() { this.searchQuery.set(''); this.filterStatus.set(''); }

  formatCurrency(value: number, currency: string = 'USD'): string { return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(value || 0); }
  formatDate(dateStr: string): string { if (!dateStr) return '-'; return new Intl.DateTimeFormat('en-US', { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(dateStr)); }
}
