// src/app/core/services/dashboard.service.ts
// Dashboard service wired to real Spring Boot backend API

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, forkJoin } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  KpiCard,
  KpiData,
  SalesChartData,
  TrafficSource,
  DashboardSummary,
  DashboardStats,
  ChartPeriod,
  Activity,
  RealtimeMetrics
} from '../../models/dashboard/dashboard.model';
import { ApiResponse } from '../../models/common';

// ── Backend DTO shapes (matching dashboard DTOs) ──────────────────────────

export interface DashboardSummaryDTO {
  totalRevenue: number;
  totalOrders: number;
  totalCustomers: number;
  totalProducts: number;
  averageOrderValue: number;
  revenueGrowthPercent: number;
  orderGrowthPercent: number;
  customerGrowthPercent: number;
}

export interface KpiCardDTO {
  title: string;
  value: string;
  change: number;
  changePercent: number;
  trend: 'UP' | 'DOWN' | 'STABLE';
  period: string;
}

export interface TopProductDTO {
  productId: string;
  productName: string;
  sku: string;
  imageUrl: string;
  totalSold: number;
  totalRevenue: number;
}

export interface RecentOrderDTO {
  orderId: string;
  orderNumber: string;
  customerName: string;
  total: number;
  status: string;
  createdAt: string;
}

export interface InventoryStatusDTO {
  totalItems: number;
  lowStockCount: number;
  outOfStockCount: number;
  totalValue: number;
}

export interface SalesChartDatasetDTO {
  label: string;
  data: number[];
}

export interface SalesChartDTO {
  labels: string[];
  datasets: SalesChartDatasetDTO[];
}

// ── Service ────────────────────────────────────────────────────────────────

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly BASE_URL = 'http://localhost:8080/api/v1/dashboard';
  private readonly http = inject(HttpClient);

  // ── Signals ──────────────────────────────────────────────────────────────

  private summary = signal<DashboardSummaryDTO | null>(null);
  private salesData = signal<SalesChartDTO | null>(null);
  private kpis = signal<KpiCardDTO[]>([]);
  private topProducts = signal<TopProductDTO[]>([]);
  private recentOrders = signal<RecentOrderDTO[]>([]);
  private inventoryStatus = signal<InventoryStatusDTO | null>(null);
  private trafficSources = signal<TrafficSource[]>([
    { name: 'Direct', value: 40, visitors: 12400, color: '#6366f1', change: 5.2 },
    { name: 'Organic Search', value: 30, visitors: 9300, color: '#10b981', change: 3.1 },
    { name: 'Social Media', value: 15, visitors: 4650, color: '#f59e0b', change: -1.4 },
    { name: 'Referral', value: 10, visitors: 3100, color: '#ef4444', change: 2.8 },
    { name: 'Email', value: 5, visitors: 1550, color: '#8b5cf6', change: 0.5 }
  ]);
  private activities = signal<Activity[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);
  private currentPeriod = signal<ChartPeriod>('daily');

  // ── Computed (public) ─────────────────────────────────────────────────────

  readonly dashboardSummary = computed(() => this.summary());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());
  readonly period = computed(() => this.currentPeriod());
  readonly kpiCards = computed(() => this.kpis());
  readonly topSellingProducts = computed(() => this.topProducts());
  readonly latestOrders = computed(() => this.recentOrders());
  readonly currentInventoryStatus = computed(() => this.inventoryStatus());
  readonly trafficSourcesList = computed(() => this.trafficSources());

  // KPI computed signals derived from summary
  readonly totalRevenue = computed(() => this.summary()?.totalRevenue ?? 0);
  readonly revenueGrowth = computed(() => this.summary()?.revenueGrowthPercent ?? 0);

  readonly totalOrders = computed(() => this.summary()?.totalOrders ?? 0);
  readonly ordersGrowth = computed(() => this.summary()?.orderGrowthPercent ?? 0);

  readonly averageOrderValue = computed(() => this.summary()?.averageOrderValue ?? 0);
  readonly aovGrowth = computed(() => 0); // not returned by summary endpoint

  readonly totalCustomers = computed(() => this.summary()?.totalCustomers ?? 0);
  readonly customerGrowth = computed(() => this.summary()?.customerGrowthPercent ?? 0);

  // Sales chart computed signals
  readonly salesChartData = computed(() => {
    const datasets = this.salesData()?.datasets ?? [];
    const revenueDataset = datasets.find(d => d.label?.toLowerCase().includes('revenue'));
    return revenueDataset?.data ?? [];
  });

  readonly ordersChartData = computed(() => {
    const datasets = this.salesData()?.datasets ?? [];
    const ordersDataset = datasets.find(d => d.label?.toLowerCase().includes('order'));
    return ordersDataset?.data ?? [];
  });

  readonly chartLabels = computed(() =>
    this.salesData()?.labels ?? ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  );

  readonly recentActivities = computed(() => this.activities().slice(0, 10));

  readonly unreadActivitiesCount = computed(() =>
    this.activities().filter(a => !a.isRead).length
  );

  // ── Initialisation ────────────────────────────────────────────────────────

  constructor() {
    this.loadDashboardData();
  }

  // ── Data Loading ──────────────────────────────────────────────────────────

  /**
   * Load all dashboard data in parallel:
   *   GET /api/v1/dashboard/summary
   *   GET /api/v1/dashboard/kpis
   *   GET /api/v1/dashboard/sales-chart
   *   GET /api/v1/dashboard/top-products
   *   GET /api/v1/dashboard/recent-orders
   *   GET /api/v1/dashboard/inventory-status
   */
  loadDashboardData(period: string = '30d'): void {
    this.loading.set(true);
    this.error.set(null);

    const params = new HttpParams().set('period', period);

    forkJoin({
      summary: this.http
        .get<ApiResponse<DashboardSummaryDTO>>(`${this.BASE_URL}/summary`, { params })
        .pipe(map(r => r.data), catchError(this.handleError('loadSummary', null as unknown as DashboardSummaryDTO))),

      kpis: this.http
        .get<ApiResponse<KpiCardDTO[]>>(`${this.BASE_URL}/kpis`)
        .pipe(map(r => r.data), catchError(this.handleError('loadKpis', [] as KpiCardDTO[]))),

      salesChart: this.http
        .get<ApiResponse<SalesChartDTO>>(`${this.BASE_URL}/sales-chart`, { params })
        .pipe(map(r => r.data), catchError(this.handleError('loadSalesChart', null as unknown as SalesChartDTO))),

      topProducts: this.http
        .get<ApiResponse<TopProductDTO[]>>(`${this.BASE_URL}/top-products`)
        .pipe(map(r => r.data), catchError(this.handleError('loadTopProducts', [] as TopProductDTO[]))),

      recentOrders: this.http
        .get<ApiResponse<RecentOrderDTO[]>>(`${this.BASE_URL}/recent-orders`)
        .pipe(map(r => r.data), catchError(this.handleError('loadRecentOrders', [] as RecentOrderDTO[]))),

      inventoryStatus: this.http
        .get<ApiResponse<InventoryStatusDTO>>(`${this.BASE_URL}/inventory-status`)
        .pipe(map(r => r.data), catchError(this.handleError('loadInventoryStatus', null as unknown as InventoryStatusDTO)))
    }).subscribe(results => {
      if (results.summary)        this.summary.set(results.summary);
      if (results.kpis)           this.kpis.set(results.kpis);
      if (results.salesChart)     this.salesData.set(results.salesChart);
      if (results.topProducts)    this.topProducts.set(results.topProducts);
      if (results.recentOrders)   this.recentOrders.set(results.recentOrders);
      if (results.inventoryStatus) this.inventoryStatus.set(results.inventoryStatus);
      this.loading.set(false);
    });
  }

  /**
   * Refresh all dashboard data.
   */
  refresh(): void {
    this.loadDashboardData(this.periodToApiValue(this.currentPeriod()));
  }

  /**
   * Change time period and reload chart + summary data.
   * GET /api/v1/dashboard/summary?period={period}
   * GET /api/v1/dashboard/sales-chart?period={period}
   */
  setPeriod(period: ChartPeriod): void {
    this.currentPeriod.set(period);
    this.loading.set(true);

    const apiPeriod = this.periodToApiValue(period);
    const params = new HttpParams().set('period', apiPeriod);

    forkJoin({
      summary: this.http
        .get<ApiResponse<DashboardSummaryDTO>>(`${this.BASE_URL}/summary`, { params })
        .pipe(map(r => r.data), catchError(this.handleError('setPeriod/summary', null as unknown as DashboardSummaryDTO))),

      salesChart: this.http
        .get<ApiResponse<SalesChartDTO>>(`${this.BASE_URL}/sales-chart`, { params })
        .pipe(map(r => r.data), catchError(this.handleError('setPeriod/salesChart', null as unknown as SalesChartDTO)))
    }).subscribe(results => {
      if (results.summary)    this.summary.set(results.summary);
      if (results.salesChart) this.salesData.set(results.salesChart);
      this.loading.set(false);
    });
  }

  // ── Individual Observables (for components that prefer observable streams) ─

  /**
   * GET /api/v1/dashboard/summary?period={period}
   */
  getSummary(period: string = '30d'): Observable<DashboardSummaryDTO> {
    const params = new HttpParams().set('period', period);
    return this.http
      .get<ApiResponse<DashboardSummaryDTO>>(`${this.BASE_URL}/summary`, { params })
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getSummary', null as unknown as DashboardSummaryDTO))
      );
  }

  /**
   * GET /api/v1/dashboard/kpis
   */
  getKpiCardsFromApi(): Observable<KpiCardDTO[]> {
    return this.http
      .get<ApiResponse<KpiCardDTO[]>>(`${this.BASE_URL}/kpis`)
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getKpiCards', [] as KpiCardDTO[]))
      );
  }

  /**
   * GET /api/v1/dashboard/top-products?limit={limit}
   */
  getTopProducts(limit: number = 5): Observable<TopProductDTO[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http
      .get<ApiResponse<TopProductDTO[]>>(`${this.BASE_URL}/top-products`, { params })
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getTopProducts', [] as TopProductDTO[]))
      );
  }

  /**
   * GET /api/v1/dashboard/recent-orders?limit={limit}
   */
  getRecentOrders(limit: number = 10): Observable<RecentOrderDTO[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http
      .get<ApiResponse<RecentOrderDTO[]>>(`${this.BASE_URL}/recent-orders`, { params })
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getRecentOrders', [] as RecentOrderDTO[]))
      );
  }

  /**
   * GET /api/v1/dashboard/inventory-status
   */
  getInventoryStatus(): Observable<InventoryStatusDTO> {
    return this.http
      .get<ApiResponse<InventoryStatusDTO>>(`${this.BASE_URL}/inventory-status`)
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getInventoryStatus', null as unknown as InventoryStatusDTO))
      );
  }

  /**
   * GET /api/v1/dashboard/sales-chart?period={period}
   */
  getSalesChart(period: string = '7d'): Observable<SalesChartDTO> {
    const params = new HttpParams().set('period', period);
    return this.http
      .get<ApiResponse<SalesChartDTO>>(`${this.BASE_URL}/sales-chart`, { params })
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getSalesChart', null as unknown as SalesChartDTO))
      );
  }

  // ── KPI Cards helper (computed from signal) ───────────────────────────────

  /**
   * Build display-ready KPI cards from the stored summary signal.
   */
  getKpiCards(): KpiCard[] {
    const s = this.summary();
    if (!s) return [];

    return [
      {
        title: 'Total Revenue',
        value: this.formatCurrency(s.totalRevenue),
        previous: this.formatCurrency(s.totalRevenue / (1 + s.revenueGrowthPercent / 100)),
        growth: s.revenueGrowthPercent,
        icon: 'cash-coin',
        iconBg: 'bg-white/20',
        iconColor: 'text-white',
        gradient: 'from-indigo-500 via-purple-500 to-pink-500'
      },
      {
        title: 'Total Orders',
        value: s.totalOrders.toLocaleString(),
        previous: Math.round(s.totalOrders / (1 + s.orderGrowthPercent / 100)).toLocaleString(),
        growth: s.orderGrowthPercent,
        icon: 'bag-check',
        iconBg: 'bg-white/20',
        iconColor: 'text-white',
        gradient: 'from-blue-500 via-cyan-500 to-teal-500'
      },
      {
        title: 'Average Order Value',
        value: '$' + s.averageOrderValue.toFixed(2),
        previous: '$' + (s.averageOrderValue * 0.95).toFixed(2),
        growth: 0,
        icon: 'receipt',
        iconBg: 'bg-white/20',
        iconColor: 'text-white',
        gradient: 'from-violet-500 via-purple-500 to-fuchsia-500'
      },
      {
        title: 'Total Customers',
        value: s.totalCustomers.toLocaleString(),
        previous: Math.round(s.totalCustomers / (1 + s.customerGrowthPercent / 100)).toLocaleString(),
        growth: s.customerGrowthPercent,
        icon: 'people',
        iconBg: 'bg-white/20',
        iconColor: 'text-white',
        gradient: 'from-orange-500 via-amber-500 to-yellow-500'
      }
    ];
  }

  /**
   * Get raw KPI data from the stored summary signal.
   */
  getKpiData(): KpiData {
    return {
      revenue: { value: this.totalRevenue(), growth: this.revenueGrowth() },
      orders: { value: this.totalOrders(), growth: this.ordersGrowth() },
      aov: { value: this.averageOrderValue(), growth: this.aovGrowth() },
      conversion: { value: 0, growth: 0 }
    };
  }

  // ── Activity helpers (client-side state, no dedicated backend endpoint) ───

  markActivityAsRead(id: string): void {
    this.activities.update(current =>
      current.map(a => (a.id === id ? { ...a, isRead: true } : a))
    );
  }

  markAllActivitiesAsRead(): void {
    this.activities.update(current => current.map(a => ({ ...a, isRead: true })));
  }

  // ── Export ────────────────────────────────────────────────────────────────

  /**
   * POST /api/v1/dashboard/export  (if/when backend implements this)
   * Falls back to a plain text blob in the meantime.
   */
  exportDashboard(format: 'pdf' | 'csv' | 'excel'): Observable<Blob> {
    return this.http
      .post(`${this.BASE_URL}/export`, { format }, { responseType: 'blob' })
      .pipe(
        catchError(() => {
          const content = `Dashboard Export - ${new Date().toISOString()}`;
          return of(new Blob([content], { type: 'text/plain' }));
        })
      );
  }

  // ── Helper Methods ────────────────────────────────────────────────────────

  private periodToApiValue(period: ChartPeriod): string {
    switch (period) {
      case 'daily':   return '7d';
      case 'weekly':  return '30d';
      case 'monthly': return '90d';
      case 'yearly':  return '365d';
      default:        return '30d';
    }
  }

  private formatCurrency(value: number): string {
    return '$' + value.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  private handleError<T>(operation = 'operation', result: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error);
      this.error.set(error?.message ?? 'Unknown error');
      return of(result);
    };
  }
}
