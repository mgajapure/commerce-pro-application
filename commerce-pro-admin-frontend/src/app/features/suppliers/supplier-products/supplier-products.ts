import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { SupplierProduct, Supplier } from '../../../core/models/suppliers/suppliers.model';
import { SupplierApiService } from '../../../core/services/suppliers/suppliers.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-supplier-products',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './supplier-products.html'
})
export class SupplierProducts implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private supplierSvc = inject(SupplierApiService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  showModal = signal(false);
  isSaving = signal(false);
  isLoading = signal(true);
  selectedItems = signal<string[]>([]);
  searchQuery = signal('');
  filterSupplier = signal('');

  products = signal<SupplierProduct[]>([]);
  suppliers = signal<Supplier[]>([]);
  editingProduct = signal<SupplierProduct | null>(null);
  productForm!: FormGroup;

  displayStats = computed(() => {
    const all = this.products();
    return [
      { label: 'Total Links', value: all.length, icon: 'link-45deg', bgColor: 'bg-blue-100', iconColor: 'text-blue-600' },
      { label: 'Active', value: all.filter(p => p.isActive).length, icon: 'check-circle', bgColor: 'bg-green-100', iconColor: 'text-green-600' },
      { label: 'Primary', value: all.filter(p => p.isPrimary).length, icon: 'star-fill', bgColor: 'bg-amber-100', iconColor: 'text-amber-600' }
    ];
  });

  filteredProducts = computed(() => {
    let result = this.products();
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(p =>
        p.productName?.toLowerCase().includes(query) ||
        p.supplierName?.toLowerCase().includes(query) ||
        p.sku?.toLowerCase().includes(query) ||
        p.supplierSku?.toLowerCase().includes(query)
      );
    }
    if (this.filterSupplier()) {
      result = result.filter(p => p.supplierId === this.filterSupplier());
    }
    return result;
  });

  constructor() { this.initForm(); }

  ngOnInit() { this.loadData(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  private loadData() {
    this.isLoading.set(true);
    this.supplierSvc.getSupplierProducts().pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.products.set(res?.content || []);
      this.isLoading.set(false);
    });
    this.supplierSvc.getActiveSuppliers().pipe(takeUntil(this.destroy$)).subscribe(data => {
      this.suppliers.set(data || []);
    });
  }

  private initForm() {
    this.productForm = this.fb.group({
      supplierId: ['', Validators.required],
      supplierName: [''],
      productId: ['', Validators.required],
      productName: ['', Validators.required],
      sku: [''],
      supplierSku: [''],
      unitCost: [null, [Validators.required, Validators.min(0)]],
      currency: ['USD'],
      minOrderQuantity: [1, Validators.min(1)],
      leadTimeDays: [14],
      isActive: [true],
      isPrimary: [false],
      notes: ['']
    });
  }

  onSupplierChange() {
    const sid = this.productForm.get('supplierId')?.value;
    const supplier = this.suppliers().find(s => s.id === sid);
    if (supplier) this.productForm.patchValue({ supplierName: supplier.name });
  }

  openModal(product?: SupplierProduct) {
    this.editingProduct.set(product || null);
    if (product) { this.productForm.patchValue(product); }
    else { this.productForm.reset({ isActive: true, isPrimary: false, currency: 'USD', minOrderQuantity: 1, leadTimeDays: 14 }); }
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); this.editingProduct.set(null); this.productForm.reset(); }

  saveProduct() {
    if (this.productForm.invalid) { this.productForm.markAllAsTouched(); return; }
    this.isSaving.set(true);
    const val = this.productForm.value;
    const action = this.editingProduct()
      ? this.supplierSvc.updateSupplierProduct(this.editingProduct()!.id, val)
      : this.supplierSvc.createSupplierProduct(val);
    action.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.isSaving.set(false); this.closeModal(); this.loadData(); this.alertSvc.success(this.editingProduct() ? 'Product link updated' : 'Product link created'); },
      error: () => { this.isSaving.set(false); this.alertSvc.error('Failed to save'); }
    });
  }

  deleteProduct(product: SupplierProduct) {
    this.alertSvc.confirm({ title: 'Delete Product Link', message: `Remove "${product.productName}" from "${product.supplierName}"?`, confirmLabel: 'Delete', danger: true })
      .then(ok => { if (!ok) return;
        this.supplierSvc.deleteSupplierProduct(product.id).pipe(takeUntil(this.destroy$)).subscribe({
          next: () => { this.loadData(); this.closeModal(); this.alertSvc.success('Product link deleted'); },
          error: () => this.alertSvc.error('Failed to delete')
        });
      });
  }

  toggleSelection(id: string) { this.selectedItems.update(sel => sel.includes(id) ? sel.filter(s => s !== id) : [...sel, id]); }
  isSelected(id: string): boolean { return this.selectedItems().includes(id); }
  isAllSelected(): boolean { const v = this.filteredProducts(); return v.length > 0 && v.every(p => this.isSelected(p.id)); }
  toggleSelectAll() { this.isAllSelected() ? this.selectedItems.set([]) : this.selectedItems.set(this.filteredProducts().map(p => p.id)); }

  getMenuItems(product: SupplierProduct): DropdownItem[] {
    return [
      { id: 'edit', label: 'Edit', icon: 'pencil' },
      { id: 'divider', label: '', divider: true },
      { id: 'delete', label: 'Delete', icon: 'trash', danger: true }
    ];
  }

  onAction(item: DropdownItem, product: SupplierProduct) {
    if (item.id === 'edit') this.openModal(product);
    else if (item.id === 'delete') this.deleteProduct(product);
  }

  aiCostAnalysis() {
    this.alertSvc.info('AI Cost Analysis', 'AI analyzes supplier pricing trends, identifies cost optimization opportunities, and suggests better alternatives based on market data.');
  }

  formatCurrency(value: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(value);
  }
}
