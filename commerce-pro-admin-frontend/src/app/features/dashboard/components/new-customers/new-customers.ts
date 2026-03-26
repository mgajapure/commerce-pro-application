import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardService } from '../../../../core/services/dashboard/dashboard.service';

@Component({
  selector: 'app-new-customers',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './new-customers.html',
  styleUrl: './new-customers.scss'
})
export class NewCustomers {
  private dashboardService = inject(DashboardService);

  isLoading = this.dashboardService.isLoading;
  totalCustomers = this.dashboardService.totalCustomers;
  customerGrowth = this.dashboardService.customerGrowth;

  newCustomersCount = computed(() => {
    const total = this.totalCustomers();
    const growth = this.customerGrowth();
    if (!total || !growth) return 0;
    return Math.round(total * (growth / 100));
  });
}
