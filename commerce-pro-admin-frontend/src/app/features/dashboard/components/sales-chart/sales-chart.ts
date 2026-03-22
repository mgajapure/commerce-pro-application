import { Component, ElementRef, viewChild, afterNextRender, signal, inject, effect, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { DashboardService } from '../../../../core/services/dashboard/dashboard.service';
import { ChartPeriod } from '../../../../core/models/dashboard/dashboard.model';

@Component({
  selector: 'app-sales-chart',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sales-chart.html',
  styleUrl: './sales-chart.scss',
})
export class SalesChart implements OnDestroy {
  private dashboardService = inject(DashboardService);
  
  chartCanvas = viewChild.required<ElementRef<HTMLCanvasElement>>('chartCanvas');
  chart: Chart | null = null;
  activeChart = signal<'revenue' | 'orders'>('revenue');
  hasError = signal(false);
  isChartReady = signal(false);
  
  // Available periods
  periods: { value: ChartPeriod; label: string }[] = [
    { value: 'daily', label: 'Daily' },
    { value: 'weekly', label: 'Weekly' },
    { value: 'monthly', label: 'Monthly' },
    { value: 'yearly', label: 'Yearly' }
  ];
  
  currentPeriod = this.dashboardService.period;
  isLoading = this.dashboardService.isLoading;
  
  // Chart data from service
  chartLabels = this.dashboardService.chartLabels;
  salesData = this.dashboardService.salesChartData;
  ordersData = this.dashboardService.ordersChartData;

  constructor() {
    // Initialize chart after view is ready
    afterNextRender(() => {
      // Wait for DOM to be fully ready
      requestAnimationFrame(() => {
        this.initChart();
      });
    });
    
    // React to data changes
    effect(() => {
      const labels = this.chartLabels();
      const sales = this.salesData();
      const orders = this.ordersData();
      const loading = this.isLoading();
      
      if (!loading && this.isChartReady() && this.chart) {
        this.updateChartData();
      }
    });
  }

  ngOnDestroy() {
    this.destroyChart();
  }

  private destroyChart() {
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
    this.isChartReady.set(false);
  }

  setChartType(type: 'revenue' | 'orders') {
    if (this.activeChart() === type) return;
    this.activeChart.set(type);
    if (this.isChartReady()) {
      this.updateChartData();
    }
  }

  setPeriod(period: ChartPeriod) {
    this.dashboardService.setPeriod(period);
  }

  private initChart() {
    try {
      const canvas = this.chartCanvas()?.nativeElement;
      if (!canvas) {
        console.warn('Chart canvas element not found');
        this.hasError.set(true);
        return;
      }

      // Ensure canvas has proper dimensions
      const rect = canvas.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) {
        console.warn('Chart canvas has zero dimensions, retrying...');
        setTimeout(() => this.initChart(), 100);
        return;
      }

      const ctx = canvas.getContext('2d');
      if (!ctx) {
        console.warn('Could not get 2D context from canvas');
        this.hasError.set(true);
        return;
      }

      // Destroy existing chart
      this.destroyChart();

      const isRevenue = this.activeChart() === 'revenue';
      const data = isRevenue ? this.salesData() : this.ordersData();
      const labels = this.chartLabels();

      // Create gradient
      const gradient = ctx.createLinearGradient(0, 0, 0, 250);
      gradient.addColorStop(0, isRevenue ? 'rgba(99, 102, 241, 0.9)' : 'rgba(59, 130, 246, 0.9)');
      gradient.addColorStop(1, isRevenue ? 'rgba(99, 102, 241, 0.1)' : 'rgba(59, 130, 246, 0.1)');

      const config: ChartConfiguration = {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [{
            label: isRevenue ? 'Revenue' : 'Orders',
            data: data,
            backgroundColor: gradient,
            borderColor: isRevenue ? '#6366f1' : '#3b82f6',
            borderWidth: 2,
            borderRadius: 8,
            borderSkipped: false,
            barPercentage: 0.6,
            categoryPercentage: 0.8
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: {
            duration: 600,
            easing: 'easeOutQuart'
          },
          plugins: {
            legend: { display: false },
            tooltip: {
              backgroundColor: 'rgba(17, 24, 39, 0.95)',
              titleColor: '#fff',
              bodyColor: '#fff',
              padding: 12,
              cornerRadius: 8,
              displayColors: false,
              titleFont: { size: 13 },
              bodyFont: { size: 12 },
              callbacks: {
                label: (context) => {
                  const val = context.parsed.y;
                  const num = typeof val === 'number' ? val : 0;
                  return isRevenue ? `Revenue: $${num.toLocaleString()}` : `Orders: ${num}`;
                }
              }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              grid: { 
                color: 'rgba(229, 231, 235, 0.5)',
                tickLength: 0
              },
              ticks: {
                font: { size: 11, family: 'system-ui, sans-serif' },
                color: '#6b7280',
                padding: 8,
                callback: (tickValue) => {
                  const num = typeof tickValue === 'number' ? tickValue : 0;
                  if (isRevenue) {
                    return '$' + (num >= 1000 ? (num / 1000).toFixed(1) + 'k' : num);
                  }
                  return num >= 1000 ? (num / 1000).toFixed(1) + 'k' : num;
                }
              },
              border: { display: false }
            },
            x: {
              grid: { display: false },
              ticks: {
                font: { size: 11, family: 'system-ui, sans-serif' },
                color: '#6b7280',
                padding: 8
              },
              border: { display: false }
            }
          },
          interaction: {
            intersect: false,
            mode: 'index'
          }
        }
      };

      this.chart = new Chart(ctx, config);
      this.isChartReady.set(true);
      this.hasError.set(false);
      
    } catch (error) {
      console.error('Chart initialization error:', error);
      this.hasError.set(true);
      this.isChartReady.set(false);
    }
  }

  private updateChartData() {
    try {
      if (!this.chart) return;

      const isRevenue = this.activeChart() === 'revenue';
      const data = isRevenue ? this.salesData() : this.ordersData();
      const labels = this.chartLabels();

      this.chart.data.labels = labels;
      if (this.chart.data.datasets[0]) {
        this.chart.data.datasets[0].data = data;
        this.chart.data.datasets[0].label = isRevenue ? 'Revenue' : 'Orders';
      }
      
      this.chart.update('active');
    } catch (error) {
      console.error('Chart update error:', error);
    }
  }

  retryChart() {
    this.hasError.set(false);
    this.isChartReady.set(false);
    setTimeout(() => this.initChart(), 100);
  }
}
