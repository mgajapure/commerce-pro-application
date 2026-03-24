// src/app/features/inventory/inventory-overview/inventory-overview.component.ts
// Inventory Overview with Spring Boot Backend Integration

import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { InventoryService } from '../../../core/services/inventory/inventory.service';
import {
  InventoryItem,
  InventoryStats,
  Warehouse,
  StockMovement,
  StockStatus,
  InventoryFilter
} from '../../../core/models/inventory';

import { PageParams } from '../../../core/models/common';
import { AlertService } from '../../../shared/services/alert.service';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiInventoryResult } from '../../../core/models/ai/ai.model';
import { Subject, takeUntil } from 'rxjs';

interface InventoryAlert {
  id: string;
  type: 'critical' | 'warning' | 'info';
  title: string;
  message: string;
}

interface InventoryStatCard {
  label: string;
  value: number;
  icon: string;
  bgColor: string;
  iconColor: string;
  subtext: string;
  filter: string;
}

@Component({
  selector: 'app-inventory-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './inventory-overview.html',
  styleUrl: './inventory-overview.scss'
})
export class InventoryOverview implements OnInit {
  // Services
  private inventoryService = inject(InventoryService);
  private alertSvc         = inject(AlertService);
  private aiSvc            = inject(AiService);
  private destroy$         = new Subject<void>();

  // AI Inventory Optimisation
  inventoryAiResults = signal<Record<string, AiInventoryResult>>({});
  inventoryAiLoading = signal<string | null>(null);
  showInventoryAiModal = signal(false);
  inventoryAiModalResult = signal<AiInventoryResult | null>(null);

  // Loading states
  isLoadingStats = signal(false);
  isLoadingInventory = signal(false);
  isLoadingLowStock = signal(false);
  error = signal<string | null>(null);

  // Data signals
  inventoryStats = signal<InventoryStats | null>(null);
  inventoryItems = signal<InventoryItem[]>([]);
  warehouses = signal<Warehouse[]>([]);
  lowStockItems = signal<InventoryItem[]>([]);
  outOfStockItems = signal<InventoryItem[]>([]);
  stockMovements = signal<StockMovement[]>([]);

  // View state
  showFilters = signal(false);
  showAdjustmentModal = signal(false);
  showTransferModal = signal(false);
  
  // Pagination
  currentPage = signal(1);
  itemsPerPage = signal(25);
  totalItems = signal(0);
  totalPages = signal(1);
  
  // Filters
  searchQuery = signal('');
  filterWarehouse = signal('');
  filterStatus = signal('');
  filterCategory = signal('');
  
  // Selection
  selectedItems = signal<string[]>([]);
  selectedItem = signal<InventoryItem | null>(null);
  
  // Sort
  sortField = signal('createdAt');
  sortDirection = signal<'asc' | 'desc'>('desc');

  // Adjustment form
  adjustmentType = signal<'add' | 'remove' | 'set'>('add');
  adjustmentQuantity = signal<number | null>(null);
  adjustmentReason = signal('');
  adjustmentNotes = signal('');

  // Transfer form
  transferToWarehouse = signal('');
  transferQuantity = signal<number | null>(null);
  transferNotes = signal('');

  // Categories (derived from inventory items)
  categories = computed(() => {
    const cats = new Set<string>();
    this.inventoryItems().forEach(item => {
      if (item.product?.category) cats.add(item.product.category);
    });
    return Array.from(cats).sort();
  });

  // Alerts derived from actual data
  alerts = computed<InventoryAlert[]>(() => {
    const stats = this.inventoryStats();
    const alerts: InventoryAlert[] = [];
    
    if (stats && stats.outOfStockCount > 0) {
      alerts.push({
        id: '1',
        type: 'critical',
        title: `${stats.outOfStockCount} Items Out of Stock`,
        message: 'These items need immediate restocking to avoid lost sales.'
      });
    }
    
    if (stats && stats.lowStockCount > 0) {
      alerts.push({
        id: '2',
        type: 'warning',
        title: `${stats.lowStockCount} Items Low on Stock`,
        message: 'Consider placing purchase orders for these items soon.'
      });
    }
    
    if (alerts.length === 0) {
      alerts.push({
        id: '3',
        type: 'info',
        title: 'Inventory Status Healthy',
        message: 'All items have adequate stock levels.'
      });
    }
    
    return alerts;
  });

  // Computed inventory stats for display cards
  inventoryStatCards = computed((): InventoryStatCard[] => {
    const stats = this.inventoryStats();
    if (!stats) return [];
    
    return [
      { 
        label: 'Total Items', 
        value: stats.totalItems, 
        icon: 'bi-box-seam', 
        bgColor: 'bg-blue-100', 
        iconColor: 'text-blue-600', 
        subtext: 'Across all warehouses', 
        filter: 'all' 
      },
      { 
        label: 'In Stock', 
        value: stats.inStockCount, 
        icon: 'bi-check-circle', 
        bgColor: 'bg-green-100', 
        iconColor: 'text-green-600', 
        subtext: 'Ready to ship', 
        filter: 'IN_STOCK' 
      },
      { 
        label: 'Low Stock', 
        value: stats.lowStockCount, 
        icon: 'bi-exclamation-triangle', 
        bgColor: 'bg-yellow-100', 
        iconColor: 'text-yellow-600', 
        subtext: 'Needs reordering', 
        filter: 'LOW_STOCK' 
      },
      { 
        label: 'Out of Stock', 
        value: stats.outOfStockCount, 
        icon: 'bi-x-circle', 
        bgColor: 'bg-red-100', 
        iconColor: 'text-red-600', 
        subtext: 'No inventory available', 
        filter: 'OUT_OF_STOCK' 
      },
      { 
        label: 'Overstock', 
        value: stats.overstockCount, 
        icon: 'bi-arrow-up-circle', 
        bgColor: 'bg-purple-100', 
        iconColor: 'text-purple-600', 
        subtext: 'Excess inventory', 
        filter: 'OVERSTOCK' 
      },
      { 
        label: 'Not Tracked', 
        value: stats.notTrackedCount, 
        icon: 'bi-eye-slash', 
        bgColor: 'bg-gray-100', 
        iconColor: 'text-gray-600', 
        subtext: 'Manual tracking', 
        filter: 'NOT_TRACKED' 
      }
    ];
  });

  // Quick stats for header
  quickStats = computed(() => {
    const stats = this.inventoryStats();
    if (!stats) return { totalValue: 0, totalUnits: 0, totalAvailable: 0, needsReorder: 0 };
    
    return {
      totalValue: stats.totalInventoryValue,
      totalUnits: stats.totalUnits,
      totalAvailable: stats.totalAvailable,
      needsReorder: stats.lowStockCount + stats.outOfStockCount
    };
  });

  // Filtered items (client-side filtering for now)
  filteredItems = computed(() => {
    let result = [...this.inventoryItems()];
    
    // Search
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(item => 
        item.product?.name?.toLowerCase().includes(query) ||
        item.product?.sku?.toLowerCase().includes(query) ||
        item.binLocation?.toLowerCase().includes(query)
      );
    }
    
    // Filters
    if (this.filterWarehouse()) {
      result = result.filter(item => item.warehouseId === this.filterWarehouse());
    }
    if (this.filterStatus()) {
      result = result.filter(item => item.status === this.filterStatus());
    }
    if (this.filterCategory()) {
      result = result.filter(item => item.product?.category === this.filterCategory());
    }
    
    // Sort
    result.sort((a, b) => {
      const field = this.sortField();
      const dir = this.sortDirection() === 'asc' ? 1 : -1;
      const aVal = (a as any)[field];
      const bVal = (b as any)[field];
  
      if (aVal == null && bVal == null) return 0;
      if (aVal == null) return -1 * dir;
      if (bVal == null) return 1 * dir;
  
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return (aVal - bVal) * dir;
      }
  
      const aStr = String(aVal).toLowerCase();
      const bStr = String(bVal).toLowerCase();
      return aStr.localeCompare(bStr) * dir;
    });
    
    return result;
  });

  // Pagination
  paginatedItems = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.filteredItems().slice(start, start + this.itemsPerPage());
  });

  visiblePages = computed(() => {
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
  });

  // Selection helpers
  isAllSelected = computed(() => {
    const visible = this.paginatedItems();
    return visible.length > 0 && visible.every(item => this.isSelected(item.id));
  });

  activeFiltersCount = computed(() => {
    let count = 0;
    if (this.searchQuery()) count++;
    if (this.filterWarehouse()) count++;
    if (this.filterStatus()) count++;
    if (this.filterCategory()) count++;
    return count;
  });

  ngOnInit() {
    this.loadAllData();
    this.loadWarehouses();
  }

  // ==================== Data Loading ====================

  loadAllData(): void {
    this.loadInventoryStats();
    this.loadInventory();
    this.loadLowStockItems();
    this.loadOutOfStockItems();
  }

  loadInventoryStats(): void {
    this.isLoadingStats.set(true);
    this.inventoryService.getInventoryStats().subscribe({
      next: (stats) => {
        this.inventoryStats.set(stats);
        this.isLoadingStats.set(false);
      },
      error: (err) => {
        console.error('Failed to load inventory stats:', err);
        this.error.set('Failed to load statistics');
        this.isLoadingStats.set(false);
      }
    });
  }

  loadInventory(): void {
    this.isLoadingInventory.set(true);
    const filter: InventoryFilter = {
      searchQuery: this.searchQuery(),
      warehouseId: this.filterWarehouse(),
      status: this.filterStatus(),
      sortBy: this.sortField(),
      sortDirection: this.sortDirection()
    };

    this.inventoryService.getInventory(filter, { page: 0, size: 100 }).subscribe({
      next: (page) => {
        this.inventoryItems.set(page.content);
        this.totalItems.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.isLoadingInventory.set(false);
      },
      error: (err) => {
        console.error('Failed to load inventory:', err);
        this.error.set('Failed to load inventory');
        this.isLoadingInventory.set(false);
      }
    });
  }

  loadWarehouses(): void {
    this.inventoryService.getWarehouses().subscribe({
      next: (warehouses) => {
        this.warehouses.set(warehouses);
      },
      error: (err) => {
        console.error('Failed to load warehouses:', err);
      }
    });
  }

  loadLowStockItems(): void {
    this.isLoadingLowStock.set(true);
    this.inventoryService.getLowStockItems().subscribe({
      next: (items) => {
        this.lowStockItems.set(items);
        this.isLoadingLowStock.set(false);
      },
      error: (err) => {
        console.error('Failed to load low stock items:', err);
        this.isLoadingLowStock.set(false);
      }
    });
  }

  loadOutOfStockItems(): void {
    this.inventoryService.getOutOfStockItems().subscribe({
      next: (items) => {
        this.outOfStockItems.set(items);
      },
      error: (err) => {
        console.error('Failed to load out of stock items:', err);
      }
    });
  }

  loadStockMovements(inventoryId: string): void {
    this.inventoryService.getStockMovements(inventoryId).subscribe({
      next: (movements) => {
        this.stockMovements.set(movements);
      },
      error: (err) => {
        console.error('Failed to load stock movements:', err);
      }
    });
  }

  retryLoad(): void {
    this.error.set(null);
    this.loadAllData();
  }

  // ==================== UI Actions ====================

  applyFilters(): void {
    this.currentPage.set(1);
    this.loadInventory();
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.filterWarehouse.set('');
    this.filterStatus.set('');
    this.filterCategory.set('');
    this.applyFilters();
  }

  applyQuickFilter(filter: string): void {
    this.clearFilters();
    if (filter === 'all') {
      this.loadInventory();
    } else if (filter === 'LOW_STOCK') {
      this.inventoryService.getLowStockItems().subscribe(items => {
        this.inventoryItems.set(items);
        this.filterStatus.set('LOW_STOCK');
      });
    } else if (filter === 'OUT_OF_STOCK') {
      this.inventoryService.getOutOfStockItems().subscribe(items => {
        this.inventoryItems.set(items);
        this.filterStatus.set('OUT_OF_STOCK');
      });
    } else {
      this.filterStatus.set(filter);
      this.loadInventory();
    }
  }

  sort(field: string): void {
    if (this.sortField() === field) {
      this.sortDirection.update(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDirection.set('asc');
    }
    this.loadInventory();
  }

  // Selection methods
  isSelected(id: string): boolean {
    return this.selectedItems().includes(id);
  }

  toggleSelection(id: string): void {
    this.selectedItems.update(selected => {
      if (selected.includes(id)) {
        return selected.filter(s => s !== id);
      }
      return [...selected, id];
    });
  }

  toggleSelectAll(): void {
    const visible = this.paginatedItems().map(item => item.id);
    if (this.isAllSelected()) {
      this.selectedItems.update(selected => selected.filter(id => !visible.includes(id)));
    } else {
      this.selectedItems.update(selected => [...new Set([...selected, ...visible])]);
    }
  }

  // Pagination
  goToPage(page: number | string): void {
    if (typeof page === 'number') {
      this.currentPage.set(page);
    }
  }

  previousPage(): void {
    if (this.currentPage() > 1) {
      this.currentPage.update(p => p - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.update(p => p + 1);
    }
  }

  dismissAlert(id: string): void {
    console.log('Dismissed alert:', id);
  }

  // ==================== Stock Adjustment ====================

  openAdjustmentModal(item: InventoryItem): void {
    this.selectedItem.set(item);
    this.adjustmentType.set('add');
    this.adjustmentQuantity.set(null);
    this.adjustmentReason.set('');
    this.adjustmentNotes.set('');
    this.showAdjustmentModal.set(true);
  }

  closeAdjustmentModal(): void {
    this.showAdjustmentModal.set(false);
    this.selectedItem.set(null);
  }

  saveAdjustment(): void {
    const item = this.selectedItem();
    if (!item || !this.adjustmentQuantity() || !this.adjustmentReason()) return;

    const qty = this.adjustmentQuantity()!;
    const type = this.adjustmentType();
    
    let newQuantity: number;
    if (type === 'add') {
      newQuantity = item.quantity + qty;
    } else if (type === 'remove') {
      newQuantity = Math.max(0, item.quantity - qty);
    } else {
      newQuantity = qty;
    }

    this.inventoryService.adjustStock(item.id, {
      quantity: newQuantity,
      adjust: false,
      reason: this.adjustmentReason(),
      notes: this.adjustmentNotes()
    }).subscribe({
      next: () => { this.closeAdjustmentModal(); this.loadAllData(); this.alertSvc.success('Stock adjusted successfully'); },
      error: (err) => { console.error('Failed to adjust stock:', err); this.alertSvc.error('Failed to adjust stock', 'Please try again.'); }
    });
  }

  // ==================== Stock Transfer ====================

  openTransferModal(item: InventoryItem): void {
    this.selectedItem.set(item);
    this.transferToWarehouse.set('');
    this.transferQuantity.set(null);
    this.transferNotes.set('');
    this.showTransferModal.set(true);
  }

  closeTransferModal(): void {
    this.showTransferModal.set(false);
    this.selectedItem.set(null);
  }

  saveTransfer(): void {
    const item = this.selectedItem();
    if (!item || !this.transferToWarehouse() || !this.transferQuantity()) return;

    this.inventoryService.transferStock({
      fromWarehouseId: item.warehouseId,
      toWarehouseId: this.transferToWarehouse(),
      itemId: item.productId,
      quantity: this.transferQuantity()!,
      notes: this.transferNotes()
    }).subscribe({
      next: () => { this.closeTransferModal(); this.loadAllData(); this.alertSvc.success('Stock transferred successfully'); },
      error: (err) => { console.error('Failed to transfer stock:', err); this.alertSvc.error('Failed to transfer stock', 'Please try again.'); }
    });
  }

  // ==================== Other Actions ====================

  viewProductDetails(item: InventoryItem): void {
    console.log('View product details:', item);
  }

  editItem(item: InventoryItem): void {
    console.log('Edit item:', item);
  }

  viewHistory(item: InventoryItem): void {
    this.loadStockMovements(item.id);
    console.log('View history for:', item);
  }

  setReorderPoint(item: InventoryItem): void {
    const newPoint = prompt(`Set reorder point for ${item.product?.name} (current: ${item.reorderPoint || 'Not set'}):`);
    if (newPoint !== null) {
      const point = parseInt(newPoint, 10);
      if (!isNaN(point) && point >= 0) {
        this.inventoryService.updateInventory(item.id, { reorderPoint: point }).subscribe({
          next: () => this.loadAllData(),
          error: (err) => console.error('Failed to update reorder point:', err)
        });
      }
    }
  }

  printLabel(item: InventoryItem): void {
    console.log('Print label:', item);
  }

  // ==================== Bulk Actions ====================

  bulkAdjustStock(): void {
    console.log('Bulk adjust:', this.selectedItems());
  }

  bulkTransfer(): void {
    console.log('Bulk transfer:', this.selectedItems());
  }

  bulkSetThreshold(): void {
    console.log('Bulk set threshold:', this.selectedItems());
  }

  bulkExport(): void {
    const items = this.filteredItems();
    const csv = this.convertToCSV(items);
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `inventory-export-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  private convertToCSV(items: InventoryItem[]): string {
    const headers = ['ID', 'Product', 'SKU', 'Warehouse', 'Quantity', 'Reserved', 'Available', 'Status', 'Bin Location', 'Unit Cost', 'Total Value'];
    const rows = items.map(item => [
      item.id,
      item.product?.name,
      item.product?.sku,
      item.warehouse?.name,
      item.quantity,
      item.reserved,
      item.available,
      item.status,
      item.binLocation,
      item.unitCost,
      item.totalValue
    ]);
    return [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
  }

  openCountModal(): void {
    console.log('Open stock count modal');
  }

  exportInventory(): void {
    this.bulkExport();
  }

  // ==================== Helper Methods ====================

  getStatusColor(status?: StockStatus): string {
    switch (status) {
      case 'IN_STOCK': return 'bg-green-100 text-green-800';
      case 'LOW_STOCK': return 'bg-yellow-100 text-yellow-800';
      case 'OUT_OF_STOCK': return 'bg-red-100 text-red-800';
      case 'OVERSTOCK': return 'bg-purple-100 text-purple-800';
      case 'NOT_TRACKED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusDotColor(status?: StockStatus): string {
    switch (status) {
      case 'IN_STOCK': return 'bg-green-500';
      case 'LOW_STOCK': return 'bg-yellow-500';
      case 'OUT_OF_STOCK': return 'bg-red-500';
      case 'OVERSTOCK': return 'bg-purple-500';
      case 'NOT_TRACKED': return 'bg-gray-500';
      default: return 'bg-gray-500';
    }
  }

  formatStatusLabel(status?: string): string {
    if (!status) return 'Unknown';
    return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  }

  protected readonly Math = Math;

  // ─── AI Inventory Optimisation ────────────────────────────────────────────

  optimiseInventoryAi(productId: string): void {
    this.inventoryAiLoading.set(productId);
    this.aiSvc.optimiseInventory(productId).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => {
        this.inventoryAiResults.update(m => ({ ...m, [productId]: r }));
        this.inventoryAiModalResult.set(r);
        this.showInventoryAiModal.set(true);
        this.inventoryAiLoading.set(null);
      },
      error: () => this.inventoryAiLoading.set(null)
    });
  }

  getInventoryAi(productId: string): AiInventoryResult | null { return this.inventoryAiResults()[productId] ?? null; }

  inventoryRecBadge(rec: string): string {
    if (rec === 'RESTOCK') return 'bg-red-100 text-red-700';
    if (rec === 'REDUCE') return 'bg-yellow-100 text-yellow-700';
    return 'bg-green-100 text-green-700';
  }
}
