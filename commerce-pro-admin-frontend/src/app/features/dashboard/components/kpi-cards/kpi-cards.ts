import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../../../core/services/dashboard/dashboard.service';

@Component({
  selector: 'app-kpi-cards',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kpi-cards.html',
  styleUrl: './kpi-cards.scss'
})
export class KpiCards {
  private dashboardService = inject(DashboardService);
  
  // Loading state from service
  isLoading = this.dashboardService.isLoading;
  
  // KPI cards computed from service data
  kpiCards = computed(() => this.dashboardService.getKpiCards());
}
