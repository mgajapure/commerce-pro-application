// src/app/core/models/dashboard.model.ts
// Dashboard models for admin module

export type ChartPeriod = 'daily' | 'weekly' | 'monthly' | 'yearly';

export interface KpiCard {
  title: string;
  value: string;
  previous: string;
  growth: number;
  icon: string;
  iconBg: string;
  iconColor: string;
  gradient?: string;
}

export interface KpiData {
  revenue: { value: number; growth: number };
  orders: { value: number; growth: number };
  aov: { value: number; growth: number };
  conversion: { value: number; growth: number };
}

export interface SalesChartData {
  labels: string[];
  revenue: number[];
  orders: number[];
}

export interface TrafficSource {
  name: string;
  value: number;
  visitors: number;
  color: string;
  change?: number;
}

export interface Activity {
  id: string;
  type: 'order' | 'customer' | 'alert' | 'system';
  title: string;
  description: string;
  timestamp: Date;
  isRead: boolean;
}

export interface DashboardSummary {
  totalRevenue: number;
  revenueGrowth: number;
  totalOrders: number;
  ordersGrowth: number;
  averageOrderValue: number;
  aovGrowth: number;
  conversionRate: number;
  conversionGrowth: number;
  period: ChartPeriod;
}

export interface DashboardStats {
  summary: DashboardSummary;
  salesData: SalesChartData;
  trafficSources: TrafficSource[];
  activities: Activity[];
}

export interface RealtimeMetrics {
  activeUsers: number;
  activeUsersChange: number;
  cartAdditions: number;
  cartAdditionsChange?: number;
  checkoutInitiated: number;
  checkoutInitiatedChange?: number;
  currentRevenue?: number;
}
