// src/app/features/inventory/low-stock/low-stock.component.ts
// Low Stock Alerts Component - UI Guidelines Compliant

import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { 
  LowStockAlert, 
  LowStockFilter,
  ReorderSuggestion 
} from '../../../core/models/inventory';
import { LowStockService } from '../../../core/services/inventory/low-stock.service';
import { WarehouseService } from '../../../core/services/inventory/warehouse.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-low-stock',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './low-stock.html',
  styleUrl: './low-stock.scss'
})
export class LowStock implements OnInit {
  private fb               = inject(FormBuilder);
  private lowStockService  = inject(LowStockService);
  private warehouseService = inject(WarehouseService);
  private alertSvc         = inject(AlertService);

  // View State
  activeView = signal<'dashboard' | 'alerts' | 'suggestions'>('dashboard');
  selectedAlert = signal<LowStockAlert | null>(null);
  
  // Modal States
  showAcknowledgeModal = signal(false);
  showResolveModal = signal(false);
  showSettingsModal = signal(false);
  showCreatePOModal = signal(false);
  
  // Filters
  searchQuery = signal('');
  filterStatus = signal<'all' | 'CRITICAL' | 'LOW' | 'REORDER'>('all');
  filterSeverity = signal<'all' | 'critical' | 'warning' | 'info'>('all');
  filterWarehouse = signal<string>('all');
  filterCategory = signal<string>('all');
  showOnlyUnacknowledged = signal(false);
  showOnlyUnread = signal(false);
  
  // Selection
  selectedAlertIds = signal<string[]>([]);
  
  // Forms
  settingsForm!: FormGroup;
  poForm!: FormGroup;

  // Data from services
  alerts = this.lowStockService.allAlerts;
  warehouses = this.warehouseService.allWarehouses;
  isLoading = this.lowStockService.isLoading;
  hasError = this.lowStockService.hasError;
  stats = this.lowStockService.lowStockStats;
  poSuggestions = this.lowStockService.poSuggestions;

  // Categories derived from alerts
  categories = computed(() => {
    const cats = new Set<string>();
    this.alerts().forEach(alert => {
      if (alert.category) cats.add(alert.category);
    });
    return Array.from(cats).sort();
  });

  // Critical alerts for dashboard
  criticalAlerts = computed(() => 
    this.alerts().filter(a => a.status === 'CRITICAL')
  );

  // Filtered alerts
  filteredAlerts = computed(() => {
    let result = this.alerts();
    
    if (this.filterStatus() !== 'all') {
      result = result.filter(a => a.status === this.filterStatus());
    }
    
    if (this.filterSeverity() !== 'all') {
      result = result.filter(a => a.severity === this.filterSeverity());
    }
    
    if (this.filterWarehouse() !== 'all') {
      result = result.filter(a => a.warehouseId === this.filterWarehouse());
    }
    
    if (this.filterCategory() !== 'all') {
      result = result.filter(a => a.category === this.filterCategory());
    }
    
    if (this.showOnlyUnacknowledged()) {
      result = result.filter(a => !a.acknowledged);
    }
    
    if (this.showOnlyUnread()) {
      result = result.filter(a => !a.isRead);
    }
    
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(a => 
        a.productName.toLowerCase().includes(query) ||
        a.productSku.toLowerCase().includes(query) ||
        a.warehouseName.toLowerCase().includes(query)
      );
    }
    
    return result.sort((a, b) => {
      // Sort by status priority: CRITICAL > LOW > REORDER
      const priority = { 'CRITICAL': 0, 'LOW': 1, 'REORDER': 2 };
      return priority[a.status] - priority[b.status];
    });
  });

  // Dashboard stats for display
  displayStats = computed(() => [
    { 
      label: 'Total Alerts', 
      value: this.stats().total, 
      icon: 'bell',
      bgColor: 'bg-blue-100',
      iconColor: 'text-blue-600',
      trend: 0
    },
    { 
      label: 'Critical', 
      value: this.stats().critical, 
      icon: 'exclamation-octagon',
      bgColor: 'bg-red-100',
      iconColor: 'text-red-600',
      trend: 0
    },
    { 
      label: 'Low Stock', 
      value: this.stats().low, 
      icon: 'exclamation-triangle',
      bgColor: 'bg-orange-100',
      iconColor: 'text-orange-600',
      trend: 0
    },
    { 
      label: 'Reorder', 
      value: this.stats().reorder, 
      icon: 'arrow-down-circle',
      bgColor: 'bg-yellow-100',
      iconColor: 'text-yellow-600',
      trend: 0
    }
  ]);

  ngOnInit() {
    this.initializeForms();
  }

  initializeForms() {
    this.settingsForm = this.fb.group({
      reorderPoint: [10, [Validators.required, Validators.min(0)]],
      reorderQuantity: [50, [Validators.required, Validators.min(1)]],
      safetyStock: [5, [Validators.required, Validators.min(0)]],
      lowStockThreshold: [10, [Validators.required, Validators.min(0)]]
    });

    this.poForm = this.fb.group({
      supplier: ['', Validators.required],
      quantity: [0, [Validators.required, Validators.min(1)]],
      unitCost: [0, [Validators.required, Validators.min(0)]],
      notes: ['']
    });
  }

  // ==================== View Navigation ====================

  setView(view: 'dashboard' | 'alerts' | 'suggestions') {
    this.activeView.set(view);
    this.selectedAlert.set(null);
  }

  // ==================== Alert Actions ====================

  acknowledgeAlert(alert: LowStockAlert) {
    this.selectedAlert.set(alert);
    this.showAcknowledgeModal.set(true);
  }

  confirmAcknowledge() {
    const alert = this.selectedAlert();
    if (!alert) return;

    this.lowStockService.acknowledgeAlert(alert.id).subscribe({
      next: () => {
        this.showAcknowledgeModal.set(false);
        this.selectedAlert.set(null);
      },
      error: (err) => console.error('Failed to acknowledge:', err)
    });
  }

  resolveAlert(alert: LowStockAlert) {
    this.selectedAlert.set(alert);
    this.showResolveModal.set(true);
  }

  confirmResolve() {
    const alert = this.selectedAlert();
    if (!alert) return;

    this.lowStockService.resolveAlert(alert.id).subscribe({
      next: () => {
        this.showResolveModal.set(false);
        this.selectedAlert.set(null);
      },
      error: (err) => console.error('Failed to resolve:', err)
    });
  }

  markAsRead(alert: LowStockAlert) {
    this.lowStockService.markAsRead(alert.id).subscribe();
  }

  // ==================== Selection ====================

  toggleAlertSelection(alertId: string) {
    this.selectedAlertIds.update(ids => 
      ids.includes(alertId) 
        ? ids.filter(id => id !== alertId)
        : [...ids, alertId]
    );
  }

  isSelected(alertId: string): boolean {
    return this.selectedAlertIds().includes(alertId);
  }

  selectAll() {
    this.selectedAlertIds.set(this.filteredAlerts().map(a => a.id));
  }

  deselectAll() {
    this.selectedAlertIds.set([]);
  }

  acknowledgeSelected() {
    const ids = this.selectedAlertIds();
    this.lowStockService.bulkAcknowledge(ids).subscribe({
      next: () => this.selectedAlertIds.set([]),
      error: (err) => console.error('Failed to acknowledge:', err)
    });
  }

  // ==================== PO Creation ====================

  createPOFromAlert(alert: LowStockAlert) {
    this.selectedAlert.set(alert);
    this.poForm.patchValue({
      quantity: alert.reorderQuantity || 50,
      unitCost: 0
    });
    this.showCreatePOModal.set(true);
  }

  createPOFromSuggestion(suggestion: ReorderSuggestion) {
    this.poForm.patchValue({
      supplier: suggestion.supplier || '',
      quantity: suggestion.suggestedQuantity,
      unitCost: suggestion.unitCost || 0
    });
    this.showCreatePOModal.set(true);
  }

  confirmCreatePO() {
    if (this.poForm.invalid) return;
    const poId = `PO-${Date.now()}`;
    this.showCreatePOModal.set(false);
    this.poForm.reset();
    this.alertSvc.success('Purchase Order Created', `${poId} has been created successfully.`);
  }

  // ==================== Settings ====================

  openSettings(alert: LowStockAlert) {
    this.selectedAlert.set(alert);
    this.settingsForm.patchValue({
      reorderPoint: alert.reorderPoint || alert.lowStockThreshold || 10,
      reorderQuantity: alert.reorderQuantity || 50,
      safetyStock: alert.lowStockThreshold || 5,
      lowStockThreshold: alert.lowStockThreshold || 10
    });
    this.showSettingsModal.set(true);
  }

  closeSettingsModal() {
    this.showSettingsModal.set(false);
    this.selectedAlert.set(null);
  }

  saveSettings() {
    if (this.settingsForm.invalid) return;
    this.closeSettingsModal();
  }

  // ==================== Helper Methods ====================

  getStatusColor(status: string): string {
    const colors: Record<string, string> = {
      'CRITICAL': 'bg-red-100 text-red-700 border-red-200',
      'REORDER': 'bg-yellow-100 text-yellow-700 border-yellow-200',
      'LOW': 'bg-orange-100 text-orange-700 border-orange-200'
    };
    return colors[status] || 'bg-gray-100 text-gray-700';
  }

  getStatusIcon(status: string): string {
    const icons: Record<string, string> = {
      'CRITICAL': 'exclamation-octagon-fill',
      'REORDER': 'arrow-down-circle-fill',
      'LOW': 'exclamation-triangle-fill'
    };
    return icons[status] || 'info-circle';
  }

  getSeverityColor(severity?: string): string {
    const colors: Record<string, string> = {
      'critical': 'bg-red-100 text-red-700',
      'warning': 'bg-orange-100 text-orange-700',
      'info': 'bg-blue-100 text-blue-700'
    };
    return colors[severity || 'info'] || 'bg-gray-100 text-gray-700';
  }

  getDaysColor(days?: number): string {
    if (!days || days <= 3) return 'text-red-600';
    if (days <= 7) return 'text-orange-600';
    if (days <= 14) return 'text-yellow-600';
    return 'text-green-600';
  }

  formatCurrency(value?: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value || 0);
  }

  formatDate(date?: Date): string {
    if (!date) return '-';
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }).format(new Date(date));
  }

  calculateProgress(alert: LowStockAlert): number {
    if (!alert.lowStockThreshold || alert.lowStockThreshold === 0) return 0;
    return Math.min(100, (alert.currentStock / alert.lowStockThreshold) * 100);
  }

  getProgressColor(percentage: number): string {
    if (percentage <= 25) return 'bg-red-500';
    if (percentage <= 50) return 'bg-orange-500';
    if (percentage <= 75) return 'bg-yellow-500';
    return 'bg-green-500';
  }

  // ==================== Retry ====================
  
  retry() {
    this.lowStockService.retry();
  }

  protected readonly Math = Math;
}
