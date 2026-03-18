// product-list.ts
// Product List Component - Integrated with Spring Boot Backend API

import { Component, signal, computed, OnInit, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { 
  Product, 
  ProductStatus, 
  StockStatus,
  ProductStats,
  ProductSummary,
  ProductFilterState
} from '../../../core/models/catalog/product.model';
import { ProductService } from '../../../core/services/catalog/product.service';
import { PageResponse, PageParams } from '../../../core/models/common';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, Dropdown],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss'
})
export class ProductList implements OnInit {
  private productService = inject(ProductService);
  private router         = inject(Router);
  private alertSvc       = inject(AlertService);
  
  // Cleanup subject for subscriptions
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();
  
  // expose global Math for template
  readonly Math: typeof Math = Math;
  
  // ==================== State Signals ====================
  
  // View State
  viewMode = signal<'table' | 'grid'>('table');
  showFilters = signal(false);
  selectedProducts = signal<string[]>([]);

  // Pagination & Data
  currentPage = signal(1); // 1-based for UI, converted to 0-based for API
  itemsPerPage = signal(20);
  totalElements = signal(0);
  totalPages = signal(0);
  products = signal<ProductSummary[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  
  // Stats from backend
  productStats = signal<ProductStats>({
    total: 0, active: 0, lowStock: 0, outOfStock: 0, drafts: 0, revenue: 0
  });

  // Filters
  searchQuery = signal('');
  filterStatus = signal<string>('');
  filterCategory = signal<string>('');
  filterStockStatus = signal<string>('');
  filterBrand = signal<string>('');
  filterMinPrice = signal<number | null>(null);
  filterMaxPrice = signal<number | null>(null);
  filterMinRating = signal<number>(0);

  // Sorting
  sortField = signal<string>('createdAt');
  sortDirection = signal<'asc' | 'desc'>('desc');
  
  // Quick filter selection
  selectedQuickFilter = signal<string>('');

  exportItems: DropdownItem[] = [
    { id: 'csv', label: 'Export as CSV', icon: 'filetype-csv' },
    { id: 'excel', label: 'Export as Excel', icon: 'filetype-xlsx' },
    { id: 'pdf', label: 'Export as PDF', icon: 'filetype-pdf' }
  ];
  
  // ==================== Computed Values ====================
  
  // Stats for display cards
  displayStats = computed(() => {
    const stats = this.productStats();
    return [
      { 
        label: 'Total Products', 
        value: stats.total.toLocaleString(), 
        trend: 8.2, 
        icon: 'box-seam', 
        bgColor: 'bg-blue-100', 
        iconColor: 'text-blue-600',
        filter: 'all'
      },
      { 
        label: 'Active', 
        value: stats.active.toLocaleString(), 
        trend: 12.5, 
        icon: 'check-circle', 
        bgColor: 'bg-green-100', 
        iconColor: 'text-green-600',
        filter: 'active'
      },
      { 
        label: 'Low Stock', 
        value: stats.lowStock.toLocaleString(), 
        trend: -3.1, 
        icon: 'exclamation-triangle', 
        bgColor: 'bg-orange-100', 
        iconColor: 'text-orange-600',
        filter: 'low_stock'
      },
      { 
        label: 'Out of Stock', 
        value: stats.outOfStock.toLocaleString(), 
        trend: -15.3, 
        icon: 'x-circle', 
        bgColor: 'bg-red-100', 
        iconColor: 'text-red-600',
        filter: 'out_of_stock'
      },
      { 
        label: 'Drafts', 
        value: stats.drafts.toLocaleString(), 
        trend: 5.7, 
        icon: 'file-earmark', 
        bgColor: 'bg-gray-100', 
        iconColor: 'text-gray-600',
        filter: 'draft'
      },
      { 
        label: 'Revenue', 
        value: '$' + (stats.revenue || 0).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 }), 
        trend: 22.4, 
        icon: 'cash-stack', 
        bgColor: 'bg-emerald-100', 
        iconColor: 'text-emerald-600',
        filter: 'revenue'
      }
    ];
  });

  activeFiltersCount = computed(() => {
    let count = 0;
    if (this.filterStatus()) count++;
    if (this.filterCategory()) count++;
    if (this.filterStockStatus()) count++;
    if (this.filterBrand()) count++;
    if (this.filterMinPrice() !== null) count++;
    if (this.filterMaxPrice() !== null) count++;
    if (this.filterMinRating() > 0) count++;
    if (this.searchQuery()) count++;
    return count;
  });

  // Computed property for paginated products (now just returns current products from backend)
  paginatedProducts = computed(() => this.products());

  // ==================== Lifecycle ====================
  
  ngOnInit() {
    // Setup search debounce
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.currentPage.set(1); // Reset to first page on search
      this.loadProducts();
    });
    
    // Initial load
    this.loadProducts();
    this.loadStats();
  }
  
  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ==================== Data Loading ====================
  
  /**
   * Load products from backend with current filters/pagination
   */
  loadProducts(): void {
    this.isLoading.set(true);
    this.error.set(null);
    
    const filter: Partial<ProductFilterState> = {
      searchQuery: this.searchQuery(),
      status: this.filterStatus(),
      category: this.filterCategory(),
      stockStatus: this.filterStockStatus(),
      brand: this.filterBrand(),
      minPrice: this.filterMinPrice(),
      maxPrice: this.filterMaxPrice(),
      minRating: this.filterMinRating()
    };
    
    const pageParams: PageParams = {
      page: this.currentPage() - 1, // Convert 1-based UI to 0-based API
      size: this.itemsPerPage(),
      sort: this.sortField(),
      direction: this.sortDirection()
    };
    
    this.productService.getProducts(filter, pageParams)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pageResponse) => {
          this.products.set(pageResponse.content);
          this.totalElements.set(pageResponse.totalElements);
          this.totalPages.set(pageResponse.totalPages);
          this.isLoading.set(false);
        },
        error: (err) => {
          this.error.set(err?.message || 'Failed to load products');
          this.isLoading.set(false);
        }
      });
  }
  
  /**
   * Load product statistics from backend
   */
  loadStats(): void {
    this.productService.getProductStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stats) => {
          this.productStats.set({
            total: stats.total,
            active: stats.active,
            lowStock: stats.lowStock,
            outOfStock: stats.outOfStock,
            drafts: stats.drafts,
            revenue: stats.revenue
          });
        },
        error: (err) => {
          console.error('Failed to load stats:', err);
        }
      });
  }

  // ==================== Event Handlers ====================

  toggleFilters() {
    this.showFilters.update(v => !v);
  }

  toggleViewMode() {
    this.viewMode.update(v => v === 'table' ? 'grid' : 'table');
  }

  getProductMenuItems(product: ProductSummary): DropdownItem[] {
    const items: DropdownItem[] = [
      { id: 'edit', label: 'Edit Product', icon: 'pencil', shortcut: '⌘E' },
      { id: 'duplicate', label: 'Duplicate', icon: 'copy', shortcut: '⌘D' },
      { id: 'view', label: 'View on Store', icon: 'box-arrow-up-right' }
    ];

    if (product.status === 'active') {
      items.push({ id: 'draft', label: 'Move to Draft', icon: 'file-earmark' });
    } else {
      items.push({ id: 'activate', label: 'Activate', icon: 'check-circle' });
    }

    items.push(
      { id: 'divider', label: '', divider: true },
      { id: 'delete', label: 'Delete Product', icon: 'trash', danger: true }
    );

    return items;
  }

  onExport(item: DropdownItem) {
    this.exportProducts(item.id as 'csv' | 'excel' | 'pdf');
  }

  onProductAction(item: DropdownItem, product: ProductSummary) {
    switch (item.id) {
      case 'edit':
        // Navigate to edit - routerLink handles this
        this.router.navigate(['edit', product.id]);
        break;
      case 'duplicate':
        this.duplicateProduct(product);
        break;
      case 'view':
        // View on store
        break;
      case 'draft':
        this.updateProductStatus(product.id, 'draft');
        break;
      case 'activate':
        this.updateProductStatus(product.id, 'active');
        break;
      case 'delete':
        this.confirmDeleteProduct(product);
        break;
    }
  }
  
  /**
   * Update single product status
   */
  updateProductStatus(id: string, status: ProductStatus) {
    this.productService.patchProduct(id, { status })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadProducts();
          this.loadStats();
        },
        error: (err) => {
          this.error.set(err?.message || 'Failed to update status');
        }
      });
  }
  
  /**
   * Duplicate a product
   */
  duplicateProduct(product: ProductSummary) {
    // Get full product details first
    this.productService.getProduct(product.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (fullProduct) => {
          if (!fullProduct) return;
          
          const { id, sku, createdAt, updatedAt, ...productData } = fullProduct;
          const newProduct = {
            ...productData,
            name: `${productData.name} (Copy)`,
            sku: `${sku}-COPY`,
            status: 'draft' as ProductStatus,
            visibility: 'hidden' as const
          };
          
          // TODO: Implement create with full product data
          // For now, just reload
          this.loadProducts();
        },
        error: (err) => {
          this.error.set(err?.message || 'Failed to duplicate product');
        }
      });
  }
  
  /**
   * Confirm and delete single product
   */
  confirmDeleteProduct(product: ProductSummary) {
    this.alertSvc.confirm({
      title: 'Delete Product',
      message: `Are you sure you want to delete "${product.name}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.productService.deleteProduct(product.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => { this.loadProducts(); this.loadStats(); this.alertSvc.success('Product deleted', `"${product.name}" has been removed.`); },
          error: (err) => { this.error.set(err?.message || 'Failed to delete product'); this.alertSvc.error('Failed to delete product', err?.message); }
        });
    });
  }

  // ==================== Selection ====================

  toggleSelection(productId: string) {
    this.selectedProducts.update(selected => {
      if (selected.includes(productId)) {
        return selected.filter(id => id !== productId);
      } else {
        return [...selected, productId];
      }
    });
  }

  isSelected(productId: string): boolean {
    return this.selectedProducts().includes(productId);
  }

  isAllSelected(): boolean {
    const products = this.paginatedProducts();
    return products.length > 0 && products.every(p => this.isSelected(p.id));
  }

  toggleSelectAll() {
    if (this.isAllSelected()) {
      this.selectedProducts.set([]);
    } else {
      this.selectedProducts.set(this.paginatedProducts().map(p => p.id));
    }
  }

  // ==================== Sorting ====================

  sort(field: string) {
    if (this.sortField() === field) {
      this.sortDirection.update(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDirection.set('desc');
    }
    this.loadProducts();
  }

  // ==================== Filters ====================
  
  /**
   * Triggered when search query changes
   */
  onSearchChange(value: string) {
    this.searchQuery.set(value);
    this.searchSubject.next(value);
  }
  
  /**
   * Apply filter and reload
   */
  applyFilter() {
    this.currentPage.set(1);
    this.loadProducts();
  }

  applyQuickFilter(filter: string) {
    this.selectedQuickFilter.set(filter);
    this.clearAllFilters();
    
    if (filter === 'active') {
      this.filterStatus.set('active');
    } else if (filter === 'low_stock') {
      this.filterStockStatus.set('low_stock');
    } else if (filter === 'out_of_stock') {
      this.filterStockStatus.set('out_of_stock');
    } else if (filter === 'draft') {
      this.filterStatus.set('draft');
    }
    
    this.loadProducts();
  }

  clearAllFilters() {
    this.filterStatus.set('');
    this.filterCategory.set('');
    this.filterStockStatus.set('');
    this.filterBrand.set('');
    this.filterMinPrice.set(null);
    this.filterMaxPrice.set(null);
    this.filterMinRating.set(0);
    this.searchQuery.set('');
    this.selectedQuickFilter.set('');
    this.currentPage.set(1);
    this.loadProducts();
  }

  // ==================== Export ====================

  exportProducts(format: 'csv' | 'excel' | 'pdf') {
    console.log('Exporting as', format);
    // TODO: Implement export using BulkOperationService
  }

  // ==================== Bulk Operations ====================

  bulkUpdateStatus(status: string) {
    const ids = this.selectedProducts();
    if (ids.length === 0) return;
    
    this.productService.bulkUpdateStatus(ids, status)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.selectedProducts.set([]);
          this.loadProducts();
          this.loadStats();
        },
        error: (err) => {
          this.error.set(err?.message || 'Failed to update status');
        }
      });
  }

  bulkUpdateStock() {
    const ids = this.selectedProducts();
    if (ids.length === 0) return;
    
    // TODO: Open modal for bulk stock update
    console.log('Bulk update stock for', ids.length, 'products');
  }

  bulkDelete() {
    const ids = this.selectedProducts();
    if (ids.length === 0) return;
    this.alertSvc.confirm({
      title: 'Delete Products',
      message: `Are you sure you want to delete ${ids.length} product(s)? This action cannot be undone.`,
      confirmLabel: `Delete ${ids.length}`,
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.productService.bulkDelete(ids)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => { this.selectedProducts.set([]); this.loadProducts(); this.loadStats(); this.alertSvc.success('Products deleted', `${ids.length} product(s) removed.`); },
          error: (err) => { this.error.set(err?.message || 'Failed to delete products'); this.alertSvc.error('Failed to delete products', err?.message); }
        });
    });
  }

  // ==================== Pagination ====================

  previousPage() {
    if (this.currentPage() > 1) {
      this.currentPage.update(p => p - 1);
      this.loadProducts();
    }
  }

  nextPage() {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.update(p => p + 1);
      this.loadProducts();
    }
  }

  goToPage(page: number) {
    this.currentPage.set(page);
    this.loadProducts();
  }
  
  onItemsPerPageChange() {
    this.currentPage.set(1);
    this.loadProducts();
  }

  visiblePages(): (number | string)[] {
    const total = this.totalPages();
    const current = this.currentPage();
    const pages: (number | string)[] = [];

    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else {
      if (current <= 3) {
        pages.push(1, 2, 3, 4, '...', total);
      } else if (current >= total - 2) {
        pages.push(1, '...', total - 3, total - 2, total - 1, total);
      } else {
        pages.push(1, '...', current - 1, current, current + 1, '...', total);
      }
    }
    return pages;
  }

  // ==================== Utility ====================

  getStatusColor(status: string): string {
    const colors: Record<string, string> = {
      active: 'bg-green-100 text-green-800',
      draft: 'bg-gray-100 text-gray-800',
      archived: 'bg-slate-100 text-slate-800',
      out_of_stock: 'bg-red-100 text-red-800',
      discontinued: 'bg-orange-100 text-orange-800'
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  }

  getStatusDot(status: string): string {
    const colors: Record<string, string> = {
      active: 'bg-green-500',
      draft: 'bg-gray-500',
      archived: 'bg-slate-500',
      out_of_stock: 'bg-red-500',
      discontinued: 'bg-orange-500'
    };
    return colors[status] || 'bg-gray-500';
  }

  getStockStatusColor(status: string): string {
    const colors: Record<string, string> = {
      in_stock: 'text-green-600',
      low_stock: 'text-orange-600',
      out_of_stock: 'text-red-600'
    };
    return colors[status] || 'text-gray-600';
  }

  getStockStatusIcon(status: string): string {
    const icons: Record<string, string> = {
      in_stock: 'check-circle-fill',
      low_stock: 'exclamation-triangle-fill',
      out_of_stock: 'x-circle-fill'
    };
    return icons[status] || 'question-circle';
  }
}
