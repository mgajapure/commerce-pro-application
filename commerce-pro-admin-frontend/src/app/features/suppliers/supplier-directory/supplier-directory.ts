import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { Supplier } from '../../../core/models/suppliers/suppliers.model';
import { SupplierApiService } from '../../../core/services/suppliers/suppliers.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-supplier-directory',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './supplier-directory.html'
})
export class SupplierDirectory implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private supplierSvc = inject(SupplierApiService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  // View State
  showModal = signal(false);
  isSaving = signal(false);
  isLoading = signal(true);
  selectedSuppliers = signal<string[]>([]);

  // Filters
  searchQuery = signal('');
  filterStatus = signal<string>('');
  viewMode = signal<'grid' | 'list'>('list');

  // Data
  suppliers = signal<Supplier[]>([]);
  stats = signal<{ total: number; active: number; preferred: number }>({
    total: 0, active: 0, preferred: 0
  });

  editingSupplier = signal<Supplier | null>(null);
  supplierForm!: FormGroup;

  exportItems: DropdownItem[] = [
    { id: 'csv', label: 'Export as CSV', icon: 'filetype-csv' },
    { id: 'excel', label: 'Export as Excel', icon: 'filetype-xlsx' }
  ];

  displayStats = computed(() => {
    const s = this.stats();
    return [
      { label: 'Total Suppliers', value: s.total.toString(), icon: 'building', bgColor: 'bg-blue-100', iconColor: 'text-blue-600', filter: 'all' },
      { label: 'Active', value: s.active.toString(), icon: 'check-circle', bgColor: 'bg-green-100', iconColor: 'text-green-600', filter: 'active' },
      { label: 'Preferred', value: s.preferred.toString(), icon: 'star-fill', bgColor: 'bg-amber-100', iconColor: 'text-amber-600', filter: 'preferred' }
    ];
  });

  filteredSuppliers = computed(() => {
    let result = this.suppliers();
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(s =>
        s.name.toLowerCase().includes(query) ||
        s.code?.toLowerCase().includes(query) ||
        s.email?.toLowerCase().includes(query) ||
        s.contactPerson?.toLowerCase().includes(query)
      );
    }
    if (this.filterStatus()) {
      switch (this.filterStatus()) {
        case 'active': result = result.filter(s => s.isActive); break;
        case 'inactive': result = result.filter(s => !s.isActive); break;
        case 'preferred': result = result.filter(s => s.isPreferred); break;
      }
    }
    return result;
  });

  constructor() { this.initForm(); }

  ngOnInit() { this.loadData(); }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  private loadData() {
    this.isLoading.set(true);
    this.supplierSvc.getSuppliers(undefined, { page: 0, size: 500 }).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.suppliers.set(res?.content || []);
      this.isLoading.set(false);
    });
    this.supplierSvc.getSupplierStats().pipe(takeUntil(this.destroy$)).subscribe(s => {
      if (s) this.stats.set(s);
    });
  }

  private initForm() {
    this.supplierForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      code: ['', [Validators.required, Validators.pattern('^[A-Z0-9-]+$')]],
      contactPerson: [''],
      email: ['', [Validators.email]],
      phone: [''],
      address: [''],
      city: [''],
      state: [''],
      country: [''],
      postalCode: [''],
      website: [''],
      description: [''],
      paymentTerms: ['Net 30'],
      paymentTermsDays: [30],
      leadTimeDays: [14],
      minOrderValue: [null],
      preferredCurrency: ['USD'],
      certifications: [''],
      notes: [''],
      isActive: [true],
      isPreferred: [false]
    });
  }

  // Helpers
  getStatusColor(isActive: boolean): string {
    return isActive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800';
  }

  getInitials(name: string): string {
    return name.split(' ').map(w => w.charAt(0).toUpperCase()).slice(0, 2).join('');
  }

  getRatingStars(rating: number): string[] {
    const stars: string[] = [];
    for (let i = 1; i <= 5; i++) {
      stars.push(i <= Math.round(rating) ? 'star-fill' : 'star');
    }
    return stars;
  }

  // Actions
  toggleViewMode() { this.viewMode.update(v => v === 'grid' ? 'list' : 'grid'); }
  clearFilters() { this.searchQuery.set(''); this.filterStatus.set(''); }
  applyQuickFilter(filter: string) { if (filter) { this.filterStatus.set(filter); } }

  openModal(supplier?: Supplier) {
    this.editingSupplier.set(supplier || null);
    if (supplier) {
      this.supplierForm.patchValue(supplier);
    } else {
      this.supplierForm.reset({ isActive: true, isPreferred: false, paymentTerms: 'Net 30', paymentTermsDays: 30, leadTimeDays: 14, preferredCurrency: 'USD' });
    }
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.editingSupplier.set(null);
    this.supplierForm.reset();
  }

  generateCode() {
    const name = this.supplierForm.get('name')?.value;
    if (name && !this.editingSupplier()) {
      const code = name.split(' ').map((w: string) => w.charAt(0).toUpperCase()).join('') + '-' + String(Math.floor(Math.random() * 900) + 100);
      this.supplierForm.patchValue({ code });
    }
  }

  saveSupplier() {
    if (this.supplierForm.invalid) { this.supplierForm.markAllAsTouched(); return; }
    this.isSaving.set(true);
    const formValue = this.supplierForm.value;
    const action = this.editingSupplier()
      ? this.supplierSvc.updateSupplier(this.editingSupplier()!.id, formValue)
      : this.supplierSvc.createSupplier(formValue);
    action.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closeModal();
        this.loadData();
        this.alertSvc.success(this.editingSupplier() ? 'Supplier updated' : 'Supplier created');
      },
      error: () => { this.isSaving.set(false); this.alertSvc.error('Failed to save supplier'); }
    });
  }

  deleteSupplier(supplier: Supplier) {
    this.alertSvc.confirm({
      title: 'Delete Supplier',
      message: `Are you sure you want to delete "${supplier.name}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.supplierSvc.deleteSupplier(supplier.id).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => { this.loadData(); this.closeModal(); this.alertSvc.success('Supplier deleted'); },
        error: () => this.alertSvc.error('Failed to delete supplier')
      });
    });
  }

  toggleSelection(id: string) {
    this.selectedSuppliers.update(sel => sel.includes(id) ? sel.filter(s => s !== id) : [...sel, id]);
  }
  isSelected(id: string): boolean { return this.selectedSuppliers().includes(id); }
  isAllSelected(): boolean {
    const v = this.filteredSuppliers();
    return v.length > 0 && v.every(s => this.isSelected(s.id));
  }
  toggleSelectAll() {
    this.isAllSelected() ? this.selectedSuppliers.set([]) : this.selectedSuppliers.set(this.filteredSuppliers().map(s => s.id));
  }

  getMenuItems(supplier: Supplier): DropdownItem[] {
    const items: DropdownItem[] = [
      { id: 'edit', label: 'Edit Supplier', icon: 'pencil' },
      { id: 'view', label: 'View Products', icon: 'box-seam' }
    ];
    if (supplier.website) items.push({ id: 'website', label: 'Visit Website', icon: 'globe' });
    items.push(supplier.isActive
      ? { id: 'deactivate', label: 'Deactivate', icon: 'pause-circle' }
      : { id: 'activate', label: 'Activate', icon: 'check-circle' }
    );
    items.push(supplier.isPreferred
      ? { id: 'unprefer', label: 'Remove Preferred', icon: 'star' }
      : { id: 'prefer', label: 'Mark Preferred', icon: 'star-fill' }
    );
    items.push(
      { id: 'divider', label: '', divider: true },
      { id: 'delete', label: 'Delete Supplier', icon: 'trash', danger: true }
    );
    return items;
  }

  onAction(item: DropdownItem, supplier: Supplier) {
    switch (item.id) {
      case 'edit': this.openModal(supplier); break;
      case 'website': if (supplier.website) window.open(supplier.website, '_blank'); break;
      case 'activate':
        this.supplierSvc.toggleActive(supplier.id, true).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadData());
        break;
      case 'deactivate':
        this.supplierSvc.toggleActive(supplier.id, false).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadData());
        break;
      case 'prefer':
        this.supplierSvc.togglePreferred(supplier.id, true).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadData());
        break;
      case 'unprefer':
        this.supplierSvc.togglePreferred(supplier.id, false).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadData());
        break;
      case 'delete': this.deleteSupplier(supplier); break;
    }
  }

  aiRiskAssessment() {
    this.alertSvc.info('AI Supplier Risk Assessment', 'AI-powered risk analysis evaluates supplier reliability, financial stability, and compliance risks. This feature will analyze all active suppliers and provide risk scores.');
  }

  bulkActivate() { this.selectedSuppliers.set([]); this.alertSvc.success('Suppliers activated'); }
  bulkDeactivate() { this.selectedSuppliers.set([]); this.alertSvc.success('Suppliers deactivated'); }
  bulkDelete() {
    const count = this.selectedSuppliers().length;
    if (!count) return;
    this.alertSvc.confirm({ title: 'Delete Suppliers', message: `Delete ${count} supplier(s)?`, confirmLabel: `Delete ${count}`, danger: true })
      .then(ok => { if (ok) { this.selectedSuppliers.set([]); this.alertSvc.success(`${count} supplier(s) deleted`); } });
  }

  onExport(item: DropdownItem) { this.alertSvc.info('Export', `Exporting suppliers as ${item.id?.toUpperCase()}`); }
}
