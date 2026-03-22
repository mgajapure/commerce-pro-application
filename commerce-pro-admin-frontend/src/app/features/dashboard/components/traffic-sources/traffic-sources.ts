import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../../../core/services/dashboard/dashboard.service';

@Component({
  selector: 'app-traffic-sources',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './traffic-sources.html',
  styleUrl: './traffic-sources.scss' 
})
export class TrafficSources {
  private dashboardService = inject(DashboardService);
  
  trafficSources = this.dashboardService.trafficSourcesList;
  isLoading = this.dashboardService.isLoading;
  
  // Computed total for percentage calculations
  totalValue = computed(() => 
    this.trafficSources().reduce((sum, source) => sum + source.value, 0)
  );
  
  // Get sources sorted by value
  sortedSources = computed(() => 
    [...this.trafficSources()].sort((a, b) => b.value - a.value)
  );
  
  // Total visitors from top 5 sources
  totalVisitors = computed(() => 
    this.trafficSources().slice(0, 5).reduce((sum, s) => sum + (s.visitors || 0), 0)
  );
}
