import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardService } from '../../../../core/services/dashboard/dashboard.service';

@Component({
  selector: 'app-kpi-cards',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './kpi-cards.html',
  styleUrl: './kpi-cards.scss'
})
export class KpiCards {
  private dashboardService = inject(DashboardService);

  isLoading = this.dashboardService.isLoading;
  kpiCards = computed(() => this.dashboardService.getKpiCards());

  getRoute(title: string): string {
    const routes: Record<string, string> = {
      'Total Revenue': '/analytics/financial',
      'Total Orders': '/orders',
      'Average Order Value': '/analytics/overview',
      'Total Customers': '/customers'
    };
    return routes[title] ?? '/dashboard';
  }
}
