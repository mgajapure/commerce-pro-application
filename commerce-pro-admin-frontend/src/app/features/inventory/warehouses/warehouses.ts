// src/app/features/inventory/warehouses/warehouses.component.ts
// Warehouses management with Spring Boot Backend Integration

import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { WarehouseService } from '../../../core/services/inventory/warehouse.service';
import { InventoryService } from '../../../core/services/inventory/inventory.service';
import { Warehouse, InventoryItem } from '../../../core/models/inventory';
import { AlertService } from '../../../shared/services/alert.service';

interface WarehouseActivity {
  id: string;
  warehouse: string;
  type: 'in' | 'out' | 'transfer';
  description: string;
  timestamp: Date;
}

@Component({
  selector: 'app-warehouses',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './warehouses.html',
  styleUrl: './warehouses.scss'
})
export class Warehouses implements OnInit {
  private fb               = inject(FormBuilder);
  private warehouseService = inject(WarehouseService);
  private inventoryService = inject(InventoryService);
  private alertSvc         = inject(AlertService);

  // View state
  showWarehouseModal = signal(false);
  showDeleteConfirm = signal(false);
  activeTab = signal<'overview' | 'inventory' | 'activity'>('overview');
  selectedWarehouse = signal<Warehouse | null>(null);
  
  // Filters
  searchQuery = signal('');
  filterStatus = signal<'all' | 'active' | 'inactive'>('all');
  
  // Data
  warehouseInventory = signal<InventoryItem[]>([]);
  isLoadingInventory = signal(false);

  // Forms
  warehouseForm: FormGroup;

  // Data from services
  warehouseList = this.warehouseService.allWarehouses;
  isLoading = this.warehouseService.isLoading;
  hasError = this.warehouseService.hasError;
  warehouseCounts = this.warehouseService.warehouseCounts;

  // Computed
  filteredWarehouses = computed(() => {
    let result = this.warehouseList();

    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(w =>
        w.name?.toLowerCase().includes(query) ||
        w.code?.toLowerCase().includes(query) ||
        w.city?.toLowerCase().includes(query) ||
        w.managerName?.toLowerCase().includes(query)
      );
    }

    if (this.filterStatus() !== 'all') {
      const isActive = this.filterStatus() === 'active';
      result = result.filter(w => (w.isActive ?? true) === isActive);
    }

    return result;
  });

  activeWarehouses = computed(() => this.warehouseList().filter(w => w.isActive !== false).length);
  inactiveWarehouses = computed(() => this.warehouseList().filter(w => w.isActive === false).length);
  defaultWarehouse = computed(() => this.warehouseList().find(w => w.isDefault));

  // Mock activities for now
  recentActivity = signal<WarehouseActivity[]>([
    { id: '1', warehouse: 'Main Warehouse', type: 'in', description: 'Received 500 units of Electronics', timestamp: new Date() },
    { id: '2', warehouse: 'West Coast Warehouse', type: 'out', description: 'Shipped 200 units to Customer #1234', timestamp: new Date(Date.now() - 3600000) },
    { id: '3', warehouse: 'Main Warehouse', type: 'transfer', description: 'Transferred 100 units to West Coast', timestamp: new Date(Date.now() - 7200000) },
  ]);

  constructor() {
    this.warehouseForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      code: ['', [Validators.required]],
      address: [''],
      city: [''],
      state: [''],
      country: [''],
      postalCode: [''],
      managerName: [''],
      managerEmail: [''],
      managerPhone: [''],
      isActive: [true],
      isDefault: [false]
    });
  }

  ngOnInit() {
    // Service auto-loads data
  }

  retryLoad() {
    this.warehouseService.retry();
  }

  // ==================== Warehouse CRUD ====================

  openWarehouseModal(warehouse?: Warehouse) {
    if (warehouse) {
      this.selectedWarehouse.set(warehouse);
      this.warehouseForm.patchValue({
        name: warehouse.name,
        code: warehouse.code,
        address: warehouse.address,
        city: warehouse.city,
        state: warehouse.state,
        country: warehouse.country,
        postalCode: warehouse.postalCode,
        managerName: warehouse.managerName,
        managerEmail: warehouse.managerEmail,
        managerPhone: warehouse.managerPhone,
        isActive: warehouse.isActive ?? true,
        isDefault: warehouse.isDefault ?? false
      });
    } else {
      this.selectedWarehouse.set(null);
      this.warehouseForm.reset({
        isActive: true,
        isDefault: false
      });
    }
    this.showWarehouseModal.set(true);
  }

  closeWarehouseModal() {
    this.showWarehouseModal.set(false);
    this.selectedWarehouse.set(null);
    this.warehouseForm.reset();
  }

  saveWarehouse() {
    if (this.warehouseForm.invalid) {
      this.warehouseForm.markAllAsTouched();
      return;
    }

    const formValue = this.warehouseForm.value;
    const existing = this.selectedWarehouse();

    if (existing) {
      this.warehouseService.updateWarehouse(existing.id, formValue).subscribe({
        next: () => { this.closeWarehouseModal(); this.alertSvc.success('Warehouse updated'); },
        error: (err) => { console.error('Failed to update warehouse:', err); this.alertSvc.error('Failed to update warehouse', 'Please try again.'); }
      });
    } else {
      this.warehouseService.createWarehouse(formValue).subscribe({
        next: () => { this.closeWarehouseModal(); this.alertSvc.success('Warehouse created'); },
        error: (err) => { console.error('Failed to create warehouse:', err); this.alertSvc.error('Failed to create warehouse', 'Please try again.'); }
      });
    }
  }

  toggleWarehouseStatus(warehouse: Warehouse, event: Event) {
    event.stopPropagation();
    const newStatus = !(warehouse.isActive ?? true);
    this.warehouseService.updateWarehouse(warehouse.id, { isActive: newStatus }).subscribe({
      next: () => this.alertSvc.success(newStatus ? 'Warehouse activated' : 'Warehouse deactivated'),
      error: (err) => { console.error('Failed to toggle status:', err); this.alertSvc.error('Failed to update status', 'Please try again.'); }
    });
  }

  confirmDelete(warehouse: Warehouse, event: Event) {
    event.stopPropagation();
    this.selectedWarehouse.set(warehouse);
    this.showDeleteConfirm.set(true);
  }

  deleteWarehouse() {
    const id = this.selectedWarehouse()?.id;
    if (id) {
      this.warehouseService.deleteWarehouse(id).subscribe({
        next: () => { this.showDeleteConfirm.set(false); this.selectedWarehouse.set(null); this.alertSvc.success('Warehouse deleted'); },
        error: (err) => { console.error('Failed to delete warehouse:', err); this.alertSvc.error('Failed to delete warehouse', 'It may have inventory items assigned to it.'); }
      });
    }
  }

  cancelDelete() {
    this.showDeleteConfirm.set(false);
    this.selectedWarehouse.set(null);
  }

  // ==================== View Details ====================

  selectWarehouse(warehouse: Warehouse) {
    this.selectedWarehouse.set(warehouse);
    this.activeTab.set('inventory');
    this.loadWarehouseInventory(warehouse.id);
  }

  clearSelection() {
    this.selectedWarehouse.set(null);
    this.activeTab.set('overview');
    this.warehouseInventory.set([]);
  }

  loadWarehouseInventory(warehouseId: string) {
    this.isLoadingInventory.set(true);
    this.inventoryService.getInventoryByWarehouse(warehouseId).subscribe({
      next: (items) => {
        this.warehouseInventory.set(items);
        this.isLoadingInventory.set(false);
      },
      error: (err) => {
        console.error('Failed to load warehouse inventory:', err);
        this.isLoadingInventory.set(false);
      }
    });
  }

  // ==================== Helper Methods ====================

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      'in': 'bi-box-arrow-in-down text-green-600',
      'out': 'bi-box-arrow-up text-blue-600',
      'transfer': 'bi-arrow-left-right text-purple-600'
    };
    return icons[type] || 'bi-circle text-gray-600';
  }

  getActivityBg(type: string): string {
    const colors: Record<string, string> = {
      'in': 'bg-green-100',
      'out': 'bg-blue-100',
      'transfer': 'bg-purple-100'
    };
    return colors[type] || 'bg-gray-100';
  }

  getInitials(name?: string): string {
    if (!name) return 'WH';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }

  getRandomColor(id: string): string {
    const colors = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500', 'bg-indigo-500'];
    let hash = 0;
    for (let i = 0; i < id.length; i++) {
      hash = id.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  formatAddress(warehouse: Warehouse): string {
    const parts = [warehouse.address, warehouse.city, warehouse.state, warehouse.country].filter(Boolean);
    return parts.join(', ');
  }

  getFilteredActivity(warehouse: Warehouse): WarehouseActivity[] {
    return this.recentActivity().filter(a => a.warehouse === warehouse.name);
  }

  protected readonly Math = Math;
}
