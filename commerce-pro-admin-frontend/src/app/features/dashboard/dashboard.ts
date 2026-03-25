import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { KpiCards } from './components/kpi-cards/kpi-cards';
import { SalesChart } from './components/sales-chart/sales-chart';
import { TrafficSources } from './components/traffic-sources/traffic-sources';
import { TopProducts } from './components/top-products/top-products';
import { InventoryStatus } from './components/inventory-status/inventory-status';
import { RecentOrders } from './components/recent-orders/recent-orders';
import { CustomerInsights } from './components/customer-insights/customer-insights';
import { NewCustomers } from './components/new-customers/new-customers';
import { AiInsights } from './components/ai-insights/ai-insights';
import { DashboardService } from '../../core/services/dashboard/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    KpiCards,
    SalesChart,
    TrafficSources,
    TopProducts,
    InventoryStatus,
    RecentOrders,
    CustomerInsights,
    NewCustomers,
    AiInsights
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  private dashboardService = inject(DashboardService);
  selectedRange = signal('30d');

  onRangeChange(range: string): void {
    this.selectedRange.set(range);
    this.dashboardService.loadDashboardData(range);
  }
}
