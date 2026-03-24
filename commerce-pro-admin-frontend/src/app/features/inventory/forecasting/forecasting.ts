// src/app/features/inventory/forecasting/forecasting.component.ts
import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

import { DemandForecast, ForecastAccuracy } from '../../../core/models/inventory';
import { DemandForecastService } from '../../../core/services/inventory/demand-forecast.service';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiDemandForecastResult } from '../../../core/models/ai/ai.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-forecasting',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, HttpClientModule],
  templateUrl: './forecasting.html',
  styleUrl: './forecasting.scss'
})
export class Forecasting implements OnInit {
  private fb = inject(FormBuilder);
  private forecastService = inject(DemandForecastService);
  private aiSvc = inject(AiService);
  private destroy$ = new Subject<void>();

  // AI Demand Forecast
  aiForecastResults = signal<Record<string, AiDemandForecastResult>>({});
  aiForecastLoading = signal<string | null>(null);
  showAiForecastModal = signal(false);
  aiForecastModalResult = signal<AiDemandForecastResult | null>(null);

  activeView = signal<'list' | 'detail' | 'create'>('list');
  selectedForecast = signal<DemandForecast | null>(null);
  showFilters = signal(false);
  showSettingsModal = signal(false);
  showGenerateModal = signal(false);
  showExportModal = signal(false);

  filterForm!: FormGroup;
  settingsForm!: FormGroup;
  generateForm!: FormGroup;

  searchQuery = signal('');
  filterCategory = signal('');
  filterTrend = signal('');
  filterAlertLevel = signal('');
  filterTimeRange = signal(30);

  // Data from services
  forecasts = this.forecastService.allForecasts;
  isLoading = this.forecastService.isLoading;
  hasError = this.forecastService.hasError;
  forecastAccuracy = this.forecastService.accuracy;

  // Categories derived from forecasts
  categories = computed(() => {
    const cats = new Set<string>();
    this.forecasts().forEach(f => {
      if (f.product?.category) cats.add(f.product.category);
    });
    return Array.from(cats).sort();
  });

  filteredForecasts = computed(() => {
    let result = this.forecasts();

    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(f =>
        f.product?.name?.toLowerCase().includes(query) ||
        f.product?.sku?.toLowerCase().includes(query) ||
        f.product?.suppliers?.join(' ').toLowerCase().includes(query)
      );
    }

    if (this.filterCategory()) {
      result = result.filter(f => f.product?.category === this.filterCategory());
    }
    if (this.filterTrend()) {
      result = result.filter(f => f.trend === this.filterTrend());
    }
    if (this.filterAlertLevel()) {
      result = result.filter(f => f.alertLevel === this.filterAlertLevel());
    }

    return result;
  });

  stats = computed(() => {
    const forecasts = this.forecasts();
    return {
      totalProducts: forecasts.length,
      highDemand: forecasts.filter(f => f.trend === 'up').length,
      lowDemand: forecasts.filter(f => f.trend === 'down').length,
      stableDemand: forecasts.filter(f => f.trend === 'stable').length,
      criticalAlerts: forecasts.filter(f => f.alertLevel === 'high').length,
      warningAlerts: forecasts.filter(f => f.alertLevel === 'medium').length,
      avgAccuracy: 92.5
    };
  });

  totalProjectedDemand = computed(() => {
    return this.forecasts().reduce((sum, f) => {
      return sum + (f.forecasts?.reduce((fs, forecast) => fs + (forecast.predictedDemand || 0), 0) || 0);
    }, 0);
  });

  totalRecommendedOrder = computed(() => {
    return this.forecasts()
      .filter(f => f.alertLevel === 'high' || f.alertLevel === 'medium')
      .reduce((sum, f) => {
        const avgForecast = (f.forecasts?.reduce((a, b) => a + (b.predictedDemand || 0), 0) || 0) / (f.forecasts?.length || 1);
        const recommendedStock = avgForecast * (f.forecastHorizon || 30) * 1.5;
        return sum + Math.max(0, recommendedStock - (f.currentStock || 0));
      }, 0);
  });

  ngOnInit() {
    this.initializeForms();
  }

  getWidth(forecast: DemandForecast){
    const leadTime = forecast.product ? forecast.product.leadTimeDays : 1;
    return Math.min((forecast.currentStock / ((forecast.avgDailySales || 1) * (leadTime || 1) * 3)) * 100, 100)
  }

  initializeForms() {
    this.filterForm = this.fb.group({
      category: [''],
      trend: [''],
      alertLevel: [''],
      timeRange: [30]
    });

    this.settingsForm = this.fb.group({
      forecastHorizon: [30, [Validators.required, Validators.min(7), Validators.max(365)]],
      confidenceLevel: [95, [Validators.required, Validators.min(80), Validators.max(99)]],
      seasonalityEnabled: [true],
      trendAnalysisEnabled: [true],
      autoReorderSuggestions: [true],
      safetyStockMultiplier: [1.5, [Validators.required, Validators.min(1), Validators.max(3)]],
      reviewFrequency: ['weekly']
    });

    this.generateForm = this.fb.group({
      productIds: [[], Validators.required],
      forecastDays: [30, [Validators.required, Validators.min(7), Validators.max(90)]],
      includeSeasonality: [true],
      includeTrend: [true]
    });
  }

  retryLoad() {
    this.forecastService.retry();
  }

  viewForecastDetail(forecast: DemandForecast) {
    this.selectedForecast.set(forecast);
    this.activeView.set('detail');
  }

  goToList() {
    this.activeView.set('list');
    this.selectedForecast.set(null);
  }

  clearFilters() {
    this.searchQuery.set('');
    this.filterCategory.set('');
    this.filterTrend.set('');
    this.filterAlertLevel.set('');
    this.filterForm?.reset();
  }

  applyQuickFilter(filter: string) {
    this.clearFilters();
    if (filter === 'critical') {
      this.filterAlertLevel.set('high');
    } else if (filter === 'warning') {
      this.filterAlertLevel.set('medium');
    } else if (filter === 'trending-up') {
      this.filterTrend.set('up');
    } else if (filter === 'trending-down') {
      this.filterTrend.set('down');
    }
  }

  getTrendIcon(trend: string): string {
    const icons: Record<string, string> = {
      'up': 'bi-graph-up-arrow',
      'down': 'bi-graph-down-arrow',
      'stable': 'bi-dash-lg'
    };
    return icons[trend] || 'bi-dash-lg';
  }

  getTrendColor(trend: string): string {
    const colors: Record<string, string> = {
      'up': 'text-green-600 bg-green-100',
      'down': 'text-red-600 bg-red-100',
      'stable': 'text-gray-600 bg-gray-100'
    };
    return colors[trend] || 'text-gray-600 bg-gray-100';
  }

  getAlertColor(level: string): string {
    const colors: Record<string, string> = {
      'none': 'bg-green-100 text-green-700 border-green-200',
      'low': 'bg-blue-100 text-blue-700 border-blue-200',
      'medium': 'bg-yellow-100 text-yellow-700 border-yellow-200',
      'high': 'bg-red-100 text-red-700 border-red-200'
    };
    return colors[level] || 'bg-gray-100 text-gray-700';
  }

  getAlertIcon(level: string): string {
    const icons: Record<string, string> = {
      'none': 'bi-check-circle',
      'low': 'bi-info-circle',
      'medium': 'bi-exclamation-triangle',
      'high': 'bi-exclamation-octagon'
    };
    return icons[level] || 'bi-circle';
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value);
  }

  formatNumber(value: number): string {
    return new Intl.NumberFormat('en-US').format(Math.round(value));
  }

  generateNewForecast() {
    this.showGenerateModal.set(true);
  }

  confirmGenerate() {
    if (this.generateForm.invalid) return;
    const formValue = this.generateForm.value;
    const now = new Date();
    const endDate = new Date();
    endDate.setDate(now.getDate() + formValue.forecastDays);
    
    // Generate forecast for each selected product
    formValue.productIds.forEach((productId: string) => {
      this.forecastService.generateForecast({
        productId: productId,
        period: 'daily',
        algorithm: formValue.includeTrend ? 'machine_learning' : 'moving_average',
        startDate: now,
        endDate: endDate,
        historicalDays: 90
      }).subscribe({
        next: () => {
          this.showGenerateModal.set(false);
          this.generateForm.reset();
        },
        error: (err) => console.error('Failed to generate forecast:', err)
      });
    });
  }

  exportForecasts() {
    this.showExportModal.set(true);
  }

  confirmExport() {
    this.showExportModal.set(false);
  }

  saveSettings() {
    if (this.settingsForm.invalid) return;
    this.showSettingsModal.set(false);
  }

  refreshForecasts() {
    this.forecastService.refreshForecasts();
  }

  getForeCastCount(forecast: DemandForecast) {
    return (forecast.forecasts?.slice(0, 30).reduce((a, b) => a + (b.predictedDemand || 0), 0) || 0);
  }

  // Products computed from forecasts for the generate modal
  products = computed(() => {
    const seen = new Set<string>();
    return this.forecasts()
      .filter(f => {
        if (seen.has(f.productId)) return false;
        seen.add(f.productId);
        return true;
      })
      .map(f => ({
        id: f.productId,
        name: f.productName,
        sku: f.sku,
        image: f.product?.image || ''
      }));
  });

  protected readonly Math = Math;

  // ─── AI Demand Forecast ───────────────────────────────────────────────────

  runAiForecast(productId: string, days = 30): void {
    this.aiForecastLoading.set(productId);
    this.aiSvc.forecastDemand(productId, days).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => {
        this.aiForecastResults.update(m => ({ ...m, [productId]: r }));
        this.aiForecastModalResult.set(r);
        this.showAiForecastModal.set(true);
        this.aiForecastLoading.set(null);
      },
      error: () => this.aiForecastLoading.set(null)
    });
  }

  getAiForecast(productId: string): AiDemandForecastResult | null { return this.aiForecastResults()[productId] ?? null; }

  confidenceBadge(level: string): string {
    if (level === 'HIGH') return 'bg-green-100 text-green-700';
    if (level === 'MEDIUM') return 'bg-yellow-100 text-yellow-700';
    return 'bg-red-100 text-red-700';
  }
}
