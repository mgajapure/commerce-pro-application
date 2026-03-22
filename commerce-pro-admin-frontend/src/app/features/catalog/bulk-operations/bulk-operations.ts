import { Component, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { 
  ProductSummary as Product,
  BulkOperationType,
  EditableField,
  ExportField,
  OperationHistoryItem,
  BulkEditValues,
  CopyOptions,
  DeleteOptions,
  ExportOptions,
  ImportMode,
  ExportFormat,
  ImportPreviewRow
} from '../../../core/models';
import { ProductService } from '../../../core/services/catalog/product.service';
import { BulkOperationService } from '../../../core/services/catalog/bulk-operation.service';
import { CategoryService } from '../../../core/services/catalog/category.service';
import { BrandService } from '../../../core/services/catalog/brand.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-bulk-operations',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, Dropdown],
  templateUrl: './bulk-operations.html',
  styleUrl: './bulk-operations.scss'
})
export class BulkOperations {
  private router               = inject(Router);
  private productService       = inject(ProductService);
  private bulkOperationService = inject(BulkOperationService);
  private categoryService      = inject(CategoryService);
  private brandService         = inject(BrandService);
  private alertSvc             = inject(AlertService);

  // View State
  activeOperation = signal<BulkOperationType>('edit');
  showHistory = signal(false);
  showSettings = signal(false);
  showProductSelector = signal(false);

  // Data from services
  allProducts = this.productService.allProducts;
  operationHistory = this.bulkOperationService.operationHistory;
  isProcessing = this.bulkOperationService.isProcessing;
  
  // Selection
  selectedProducts = signal<Product[]>([]);

  // Operations Configuration
  operations = signal([
    { id: 'edit' as const, label: 'Mass Edit', icon: 'pencil-square' },
    { id: 'import' as const, label: 'Import', icon: 'upload' },
    { id: 'export' as const, label: 'Export', icon: 'download' },
    { id: 'copy' as const, label: 'Duplicate', icon: 'copy' },
    { id: 'delete' as const, label: 'Delete', icon: 'trash3' }
  ]);

  // Edit Operation
  editableFields = signal<EditableField[]>([
    { id: 'status', label: 'Status', icon: 'check-circle' },
    { id: 'category', label: 'Category', icon: 'folder' },
    { id: 'price', label: 'Price', icon: 'currency-dollar' },
    { id: 'stock', label: 'Stock', icon: 'box-seam' },
    { id: 'tags', label: 'Tags', icon: 'tags' },
    { id: 'brand', label: 'Brand', icon: 'tag' },
    { id: 'weight', label: 'Weight', icon: 'weight' },
    { id: 'description', label: 'Description', icon: 'text-left' }
  ]);
  selectedFields = signal<string[]>(['status']);
  
  bulkValues = signal<BulkEditValues>({
    status: 'active',
    category: '',
    priceOperation: 'set',
    priceValue: null,
    stockOperation: 'set',
    stockValue: null,
    tags: [],
    tagOperation: 'add',
    brand: '',
    weight: null,
    weightUnit: 'kg',
    description: '',
    descriptionOperation: 'replace',
    findText: '',
    replaceText: ''
  });
  
  newTagInput = signal('');
  validationErrors = signal<{ id: string; product: string; message: string }[]>([]);

  // Import Operation
  uploadedFile = signal<File | null>(null);
  importMode = signal<ImportMode>('upsert');
  skipValidation = signal(false);
  importPreview = signal<ImportPreviewRow[]>([]);

  // Export Operation
  exportFormats = signal([
    { id: 'csv', label: 'CSV', icon: 'filetype-csv' },
    { id: 'excel', label: 'Excel', icon: 'filetype-xlsx' },
    { id: 'json', label: 'JSON', icon: 'filetype-json' },
    { id: 'xml', label: 'XML', icon: 'filetype-xml' }
  ]);
  selectedFormat = signal<ExportFormat>('csv');
  
  exportableFields = signal<ExportField[]>([
    { id: 'id', label: 'ID', selected: true },
    { id: 'name', label: 'Name', selected: true },
    { id: 'sku', label: 'SKU', selected: true },
    { id: 'description', label: 'Description', selected: true },
    { id: 'category', label: 'Category', selected: true },
    { id: 'brand', label: 'Brand', selected: true },
    { id: 'price', label: 'Price', selected: true },
    { id: 'comparePrice', label: 'Compare at Price', selected: false },
    { id: 'cost', label: 'Cost', selected: false },
    { id: 'stock', label: 'Stock', selected: true },
    { id: 'weight', label: 'Weight', selected: false },
    { id: 'status', label: 'Status', selected: true },
    { id: 'tags', label: 'Tags', selected: false },
    { id: 'createdAt', label: 'Created Date', selected: false },
    { id: 'updatedAt', label: 'Updated Date', selected: false }
  ]);
  
  exportOptions = signal<ExportOptions>({
    includeImages: false,
    includeVariants: false,
    includeMetadata: true,
    useHeaders: true
  });
  
  totalProductCount = computed(() => this.allProducts().length);

  // Copy Operation
  copyOptions = signal<CopyOptions>({
    namingPattern: 'suffix',
    customPattern: '{original} - Copy',
    skuPattern: 'suffix',
    customSkuPattern: 'SKU-{original}-COPY',
    copyImages: true,
    copyVariants: true,
    setAsDraft: true,
    resetInventory: false
  });

  // Delete Operation
  deleteOptions = signal<DeleteOptions>({
    deleteImages: false,
    deleteVariants: true,
    archiveInstead: false
  });
  deleteConfirmation = signal('');

  // Settings
  settings = signal({
    autoSave: true,
    confirmBeforeExecute: true,
    emailNotifications: false,
    defaultPageSize: 25
  });

  // Product Selector
  productSelectorQuery = signal('');
  productSelectorResults = signal<Product[]>([]);
  productSelectorSelected = signal<Product[]>([]);

  // Templates
  templateItems: DropdownItem[] = [
    { id: 'price_update', label: 'Price Update Template', icon: 'currency-dollar' },
    { id: 'stock_sync', label: 'Stock Sync Template', icon: 'box-seam' },
    { id: 'category_reassign', label: 'Category Reassign', icon: 'folder' },
    { id: 'seasonal_prep', label: 'Seasonal Preparation', icon: 'sun' }
  ];

  // Available Options from services
  availableCategories = computed(() => 
    this.categoryService.allCategories()
      .filter(c => c.isActive)
      .map(c => ({ id: c.id, name: c.name }))
  );

  availableBrands = computed(() => 
    this.brandService.allBrands()
      .filter(b => b.isActive)
      .map(b => b.name)
  );

  // Computed Values
  selectedCount = computed(() => this.selectedProducts().length);
  uniqueCategories = computed(() => new Set(this.selectedProducts().map(p => p.category)).size);
  uniqueBrands = computed(() => new Set(this.selectedProducts().map(p => p.brand)).size);
  totalValue = computed(() => this.selectedProducts().reduce((sum, p) => sum + (p.price * p.stock), 0));
  averagePrice = computed(() => this.selectedCount() > 0 ? this.totalValue() / this.selectedCount() : 0);
  
  pricePreview = computed(() => {
    const op = this.bulkValues().priceOperation;
    const val = this.bulkValues().priceValue;
    const avg = this.averagePrice();
    if (!val || val <= 0) return null;
    
    switch (op) {
      case 'set': return val;
      case 'increase_percent': return avg * (1 + val / 100);
      case 'decrease_percent': return avg * (1 - val / 100);
      case 'increase_fixed': return avg + val;
      case 'decrease_fixed': return avg - val;
      default: return null;
    }
  });

  hasOrders = computed(() => this.selectedProducts().some(p => p.hasOrders));
  productsWithOrders = computed(() => this.selectedProducts().filter(p => p.hasOrders).length);
  selectedExportFieldsCount = computed(() => this.exportableFields().filter(f => f.selected).length);
  estimatedFileSize = computed(() => {
    const fields = this.selectedExportFieldsCount();
    const products = this.selectedCount() || this.totalProductCount();
    const bytes = products * fields * 50;
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  });

  constructor() {
    // Initialize product selector
    this.productSelectorResults.set(
      this.allProducts().slice(0, 10).map(p => ({
        id: p.id,
        name: p.name,
        sku: p.sku,
        category: p.category,
        brand: p.brand,
        price: p.price,
        stock: p.stock,
        image: p.image,
        status: p.status,
        hasOrders: Math.random() > 0.7 // Simulated
      }))
    );
  }

  // Operations
  setActiveOperation(op: BulkOperationType) {
    this.activeOperation.set(op);
  }

  // Selection Management
  removeFromSelection(id: string) {
    this.selectedProducts.update(products => products.filter(p => p.id !== id));
  }

  clearSelection() {
    this.selectedProducts.set([]);
  }

  openProductSelector() {
    this.showProductSelector.set(true);
    this.productSelectorSelected.set([...this.selectedProducts()]);
  }

  closeProductSelector() {
    this.showProductSelector.set(false);
  }

  isProductSelected(id: string): boolean {
    return this.productSelectorSelected().some(p => p.id === id);
  }

  toggleProductSelection(product: Product) {
    this.productSelectorSelected.update(selected => {
      const exists = selected.some(p => p.id === product.id);
      if (exists) {
        return selected.filter(p => p.id !== product.id);
      }
      return [...selected, product];
    });
  }

  selectAllProducts() {
    this.productSelectorSelected.set([...this.productSelectorResults()]);
  }

  confirmProductSelection() {
    this.selectedProducts.set([...this.productSelectorSelected()]);
    this.closeProductSelector();
  }

  searchProducts() {
    const query = this.productSelectorQuery().toLowerCase();
    if (!query) {
      this.productSelectorResults.set(
        this.allProducts().slice(0, 10).map(p => ({
          id: p.id,
          name: p.name,
          sku: p.sku,
          category: p.category,
          brand: p.brand,
          price: p.price,
          stock: p.stock,
          image: p.image,
          status: p.status,
          hasOrders: Math.random() > 0.7
        }))
      );
      return;
    }
    
    this.productSelectorResults.update(products => 
      products.filter(p => 
        p.name.toLowerCase().includes(query) ||
        p.sku.toLowerCase().includes(query) ||
        p.category.toLowerCase().includes(query)
      )
    );
  }

  // Field Management
  toggleFieldSelection(fieldId: string) {
    this.selectedFields.update(fields => {
      if (fields.includes(fieldId)) {
        return fields.filter(f => f !== fieldId);
      }
      return [...fields, fieldId];
    });
  }

  // Tag Management
  addBulkTag() {
    const tag = this.newTagInput().trim();
    if (tag && !this.bulkValues().tags?.includes(tag)) {
      this.bulkValues.update(v => ({ ...v, tags: [...(v.tags || []), tag] }));
      this.newTagInput.set('');
    }
  }

  removeBulkTag(tag: string) {
    this.bulkValues.update(v => ({ ...v, tags: v.tags?.filter((t: string) => t !== tag) || [] }));
  }

  // Validation
  validateBulkEdit() {
    const errors: { id: string; product: string; message: string }[] = [];
    
    if (this.bulkValues().priceValue !== null && this.bulkValues().priceValue! < 0) {
      this.selectedProducts().forEach(p => {
        errors.push({
          id: p.id,
          product: p.name,
          message: 'Price cannot be negative'
        });
      });
    }
    
    this.validationErrors.set(errors);
  }

  previewChanges() {
    console.log('Previewing changes:', this.bulkValues());
    this.alertSvc.info('Preview', 'A comparison table of changes would be shown here.');
  }

  executeBulkEdit() {
    const ids = this.selectedProducts().map(p => p.id);
    if (ids.length === 0) return;
    
    this.bulkOperationService.executeBulkEdit(
      ids,
      this.selectedFields(),
      this.bulkValues()
    ).subscribe({
      next: () => {
        this.selectedProducts.set([]);
        this.alertSvc.success('Bulk edit completed', `${ids.length} product(s) updated successfully.`);
      },
      error: (err) => this.alertSvc.error('Bulk edit failed', err.message)
    });
  }

  // Import Operations
  onFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      this.uploadedFile.set(file);
      this.generateImportPreview();
    }
  }

  onFileDrop(event: DragEvent) {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) {
      this.uploadedFile.set(file);
      this.generateImportPreview();
    }
  }

  removeUploadedFile() {
    this.uploadedFile.set(null);
    this.importPreview.set([]);
  }

  generateImportPreview() {
    const file = this.uploadedFile();
    if (!file) return;
    
    this.bulkOperationService.validateImport(file, this.importMode()).subscribe({
      next: (preview) => {
        this.importPreview.set(preview);
      }
    });
  }

  downloadTemplate(format: 'csv' | 'excel') {
    this.bulkOperationService.downloadTemplate(format).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `product-template.${format}`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  validateImport() {
    const file = this.uploadedFile();
    if (!file) return;
    
    this.bulkOperationService.validateImport(file, this.importMode()).subscribe({
      next: (preview) => {
        this.importPreview.set(preview);
        const validCount = preview.filter(r => r.status === 'valid').length;
        const errorCount = preview.filter(r => r.status === 'error').length;
        if (errorCount > 0) {
          this.alertSvc.warning('Validation complete', `${validCount} valid rows, ${errorCount} errors found. Please review before importing.`);
        } else {
          this.alertSvc.success('Validation complete', `All ${validCount} rows are valid and ready to import.`);
        }
      }
    });
  }

  executeImport() {
    const file = this.uploadedFile();
    if (!file) return;
    
    this.bulkOperationService.executeImport(file, this.importMode(), this.skipValidation()).subscribe({
      next: () => {
        this.uploadedFile.set(null);
        this.importPreview.set([]);
        this.alertSvc.success('Import completed', 'Products have been imported successfully.');
      }
    });
  }

  // Export Operations
  selectAllExportFields() {
    this.exportableFields.update(fields => fields.map(f => ({ ...f, selected: true })));
  }

  deselectAllExportFields() {
    this.exportableFields.update(fields => fields.map(f => ({ ...f, selected: false })));
  }

  previewExport() {
    console.log('Previewing export...');
    this.alertSvc.info('Export Preview', 'A sample of the exported data would be shown here.');
  }

  executeExport() {
    const ids = this.selectedCount() > 0 ? this.selectedProducts().map(p => p.id) : 'all';
    const fields = this.exportableFields().filter(f => f.selected).map(f => f.id);
    
    this.bulkOperationService.executeExport(
      ids,
      this.selectedFormat(),
      fields,
      this.exportOptions()
    ).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `products-export.${this.selectedFormat()}`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  // Copy Operations
  generateCopyNamePreview(): string {
    const original = this.selectedProducts()[0]?.name || 'Product Name';
    const pattern = this.copyOptions().namingPattern;
    const custom = this.copyOptions().customPattern;
    
    switch (pattern) {
      case 'suffix': return `${original} (Copy)`;
      case 'prefix': return `Copy - ${original}`;
      case 'custom': return custom.replace('{original}', original).replace('{date}', new Date().toISOString().split('T')[0]).replace('{index}', '1');
      default: return original;
    }
  }

  executeCopy() {
    const ids = this.selectedProducts().map(p => p.id);
    if (ids.length === 0) return;
    
    this.bulkOperationService.executeCopy(ids, this.copyOptions()).subscribe({
      next: () => {
        this.selectedProducts.set([]);
        this.alertSvc.success('Products duplicated', `${ids.length} product(s) duplicated successfully.`);
      },
      error: () => this.alertSvc.error('Failed to duplicate products')
    });
  }

  // Delete Operations
  executeDelete() {
    const ids = this.selectedProducts().map(p => p.id);
    if (ids.length === 0) return;
    
    this.bulkOperationService.executeDelete(ids, this.deleteOptions()).subscribe({
      next: () => {
        this.selectedProducts.set([]);
        if (this.deleteOptions().archiveInstead) {
          this.alertSvc.success('Products archived', `${ids.length} product(s) have been archived.`);
        } else {
          this.alertSvc.success('Products deleted', `${ids.length} product(s) permanently deleted.`);
          this.router.navigate(['/products']);
        }
      },
      error: () => this.alertSvc.error('Failed to delete products')
    });
  }

  // History Management
  undoOperation(id: string) {
    this.bulkOperationService.undoOperation(id).subscribe({
      next: () => this.alertSvc.info('Undo initiated', 'The operation is being reversed.'),
      error: () => this.alertSvc.error('Failed to undo operation')
    });
  }

  clearHistory() {
    this.alertSvc.confirm({
      title: 'Clear History',
      message: 'Are you sure you want to clear all operation history? This cannot be undone.',
      confirmLabel: 'Clear History',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.bulkOperationService.clearHistory().subscribe({
        next: () => this.alertSvc.success('History cleared'),
        error: () => this.alertSvc.error('Failed to clear history')
      });
    });
  }

  // Settings
  saveSettings() {
    this.showSettings.set(false);
    this.alertSvc.success('Settings saved', 'Your bulk operation settings have been saved.');
  }

  onTemplateSelect(item: DropdownItem) {
    console.log('Selected template:', item.id);
    this.alertSvc.success('Template loaded', `"${item.label}" has been applied.`);
  }
}
