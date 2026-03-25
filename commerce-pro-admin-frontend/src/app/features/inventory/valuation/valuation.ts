// src/app/features/inventory/valuation/valuation.component.ts
import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

import { 
  InventoryValuation, 
  ValuationMethodType, 
  ValuationSummary 
} from '../../../core/models/inventory/inventory-valuation.model';

// Custom interface for UI method configuration
interface MethodConfig {
  method: ValuationMethodType;
  name: string;
  description: string;
  icon: string;
  color: string;
  taxImplication: string;
  bestFor: string[];
}
import { InventoryValuationService } from '../../../core/services/inventory/inventory-valuation.service';

@Component({
  selector: 'app-valuation',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, HttpClientModule],
  templateUrl: './valuation.html',
  styleUrl: './valuation.scss'
})
export class Valuation implements OnInit {
  private fb = inject(FormBuilder);
  protected valuationService = inject(InventoryValuationService);

  // View State
  activeView = signal<'list' | 'detail' | 'adjust'>('list');
  selectedValuation = signal<InventoryValuation | null>(null);
  showFilters = signal(false);
  showMethodComparison = signal(false);

  // Modals
  showMethodModal = signal(false);
  showAdjustModal = signal(false);
  showExportModal = signal(false);
  showRecalculateModal = signal(false);
  showDetailModal = signal(false);

  // Forms
  filterForm!: FormGroup;
  adjustForm!: FormGroup;
  methodForm!: FormGroup;

  // Filters
  searchQuery = signal('');
  filterCategory = signal('');
  filterMethod = signal<ValuationMethodType | ''>('');
  filterStatus = signal<string>('');

  // Data from services
  valuations = this.valuationService.allValuations;
  isLoading = this.valuationService.isLoading;
  hasError = this.valuationService.hasError;
  // summary = this.valuationService.summary; // Service doesn't have summary property

  // Categories derived from valuations
  categories = computed(() => {
    const cats = new Set<string>();
    this.valuations().forEach(v => {
      // Category not in model, skip for now
    });
    return Array.from(cats).sort();
  });

  valuationMethods: MethodConfig[] = [
    {
      method: 'fifo',
      name: 'FIFO',
      description: 'First-In, First-Out. Oldest inventory costs are assigned to COGS first.',
      icon: 'bi-arrow-down-circle',
      color: 'bg-blue-100 text-blue-700',
      taxImplication: 'Higher taxable income during inflation',
      bestFor: ['Perishable goods', 'Stable prices', 'International reporting']
    },
    {
      method: 'lifo',
      name: 'LIFO',
      description: 'Last-In, First-Out. Newest inventory costs are assigned to COGS first.',
      icon: 'bi-arrow-up-circle',
      color: 'bg-purple-100 text-purple-700',
      taxImplication: 'Lower taxable income during inflation (US only)',
      bestFor: ['Inflationary periods', 'Tax optimization', 'Non-perishable goods']
    },
    {
      method: 'weighted_average',
      name: 'Weighted Average',
      description: 'Average cost of all inventory items is calculated and applied uniformly.',
      icon: 'bi-calculator',
      color: 'bg-green-100 text-green-700',
      taxImplication: 'Moderate and consistent tax treatment',
      bestFor: ['High-volume items', 'Homogeneous products', 'Simplified accounting']
    },
    {
      method: 'specific_identification',
      name: 'Specific ID',
      description: 'Each item is tracked individually by its actual purchase cost.',
      icon: 'bi-fingerprint',
      color: 'bg-orange-100 text-orange-700',
      taxImplication: 'Exact cost matching to revenue',
      bestFor: ['High-value items', 'Unique products', 'Serialized inventory']
    }
  ];

  ngOnInit() {
    this.initializeForms();
  }

  initializeForms() {
    this.filterForm = this.fb.group({
      category: [''],
      method: [''],
      status: ['']
    });

    this.adjustForm = this.fb.group({
      adjustmentType: ['value_increase', Validators.required],
      amount: [0, [Validators.required, Validators.min(0)]],
      reason: ['', Validators.required],
      notes: ['']
    });

    this.methodForm = this.fb.group({
      method: ['fifo', Validators.required],
      effectiveDate: [new Date().toISOString().split('T')[0], Validators.required],
      reason: ['', Validators.required],
      applyToAll: [false]
    });
  }

  retryLoad() {
    this.valuationService.retry();
  }

  // Computed
  filteredValuations = computed(() => {
    let result = this.valuations();

    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(v =>
        v.productName?.toLowerCase().includes(query) ||
        v.sku?.toLowerCase().includes(query)
      );
    }

    // Category filter not supported by model
    if (this.filterMethod()) {
      result = result.filter(v => v.valuationMethod === this.filterMethod());
    }
    if (this.filterStatus()) {
      result = result.filter(v => v.status === this.filterStatus());
    }

    return result.sort((a, b) => (b.totalValue || 0) - (a.totalValue || 0));
  });

  stats = computed(() => {
    const valuations = this.valuations();
    return {
      totalProducts: valuations.length,
      fifoCount: valuations.filter(v => v.valuationMethod === 'fifo').length,
      lifoCount: valuations.filter(v => v.valuationMethod === 'lifo').length,
      weightedCount: valuations.filter(v => v.valuationMethod === 'weighted_average').length,
      specificCount: valuations.filter(v => v.valuationMethod === 'specific_identification').length,
      highValue: valuations.filter(v => (v.totalValue || 0) > 10000).length,
      increasing: 0 // variance not in model
    };
  });

  // Methods
  viewValuationDetail(valuation: InventoryValuation) {
    this.selectedValuation.set(valuation);
    this.showDetailModal.set(true);
    this.activeView.set('detail');
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedValuation.set(null);
    this.activeView.set('list');
  }

  goToList() {
    this.activeView.set('list');
    this.selectedValuation.set(null);
  }

  clearFilters() {
    this.searchQuery.set('');
    this.filterCategory.set('');
    this.filterMethod.set('');
    this.filterStatus.set('');
    this.filterForm?.reset();
  }

  applyQuickFilter(filter: string) {
    this.clearFilters();
    if (filter === 'fifo') {
      this.filterMethod.set('fifo');
    } else if (filter === 'lifo') {
      this.filterMethod.set('lifo');
    } else if (filter === 'weighted') {
      this.filterMethod.set('weighted_average');
    } else if (filter === 'high-value') {
      // Sort by value in computed
    } else if (filter === 'increasing') {
      // Filter by positive variance
    }
  }

  getMethodConfig(method: ValuationMethodType): MethodConfig {
    return this.valuationMethods.find(m => m.method === method) || this.valuationMethods[0];
  }

  getMethodName(method: ValuationMethodType): string {
    const config = this.getMethodConfig(method);
    return config.name;
  }

  getMethodColor(method: ValuationMethodType): string {
    const config = this.getMethodConfig(method);
    return config.color;
  }

  getVarianceColor(variance?: number): string {
    if (!variance) return 'text-gray-600 bg-gray-100';
    if (variance > 0) return 'text-green-600 bg-green-100';
    if (variance < 0) return 'text-red-600 bg-red-100';
    return 'text-gray-600 bg-gray-100';
  }

  getVarianceIcon(variance?: number): string {
    if (!variance) return 'bi-dash';
    if (variance > 0) return 'bi-arrow-up-right';
    if (variance < 0) return 'bi-arrow-down-right';
    return 'bi-dash';
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2
    }).format(value);
  }

  formatNumber(value: number): string {
    return new Intl.NumberFormat('en-US').format(Math.round(value));
  }

  formatDate(date: Date | string): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  calculateCOGS(valuation: InventoryValuation, unitsSold: number): number {
    let cogs = 0;
    let remainingUnits = unitsSold;

    if (valuation.valuationMethod === 'fifo') {
      // FIFO: Use oldest layers first
      for (const layer of [...(valuation.costLayers || [])].reverse()) {
        if (remainingUnits <= 0) break;
        const unitsFromLayer = Math.min(remainingUnits, layer.remainingQuantity || 0);
        cogs += unitsFromLayer * (layer.unitCost || 0);
        remainingUnits -= unitsFromLayer;
      }
    } else if (valuation.valuationMethod === 'lifo') {
      // LIFO: Use newest layers first
      for (const layer of valuation.costLayers || []) {
        if (remainingUnits <= 0) break;
        const unitsFromLayer = Math.min(remainingUnits, layer.remainingQuantity || 0);
        cogs += unitsFromLayer * (layer.unitCost || 0);
        remainingUnits -= unitsFromLayer;
      }
    } else {
      // Weighted Average or Specific ID
      cogs = unitsSold * (valuation.averageUnitCost || 0);
    }

    return Math.round(cogs * 100) / 100;
  }

  compareMethods() {
    this.showMethodComparison.set(true);
  }

  changeMethod() {
    this.showMethodModal.set(true);
  }

  confirmMethodChange() {
    if (this.methodForm.invalid) return;
    this.showMethodModal.set(false);
    this.methodForm.reset();
  }

  adjustValuation() {
    this.showAdjustModal.set(true);
  }

  confirmAdjustment() {
    if (this.adjustForm.invalid) return;
    this.showAdjustModal.set(false);
    this.adjustForm.reset();
  }

  exportValuations() {
    this.showExportModal.set(true);
  }

  confirmExport() {
    this.showExportModal.set(false);
  }

  recalculateValuations() {
    this.showRecalculateModal.set(true);
  }

  confirmRecalculate() {
    this.valuationService.recalculateValuations().subscribe({
      next: () => {
        this.showRecalculateModal.set(false);
      },
      error: (err) => console.error('Failed to recalculate:', err)
    });
  }

  protected readonly Math = Math;
}
