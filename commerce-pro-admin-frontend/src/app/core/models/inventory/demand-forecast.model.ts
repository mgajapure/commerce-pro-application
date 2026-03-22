// src/app/core/models/inventory/demand-forecast.model.ts
// Demand forecasting and prediction models

import type { InventoryProduct } from './product.model';
import type { Warehouse } from './warehouse/warehouse.model';

export type ForecastPeriod = 'daily' | 'weekly' | 'monthly' | 'quarterly' | 'yearly';

export type ForecastAlgorithm = 'moving_average' | 'linear_regression' | 'seasonal_decomposition' | 'machine_learning';

export interface HistoricalData {
  date: Date;
  quantity: number;
  revenue: number;
  orders: number;
}

export interface ForecastData {
  period: string;
  date: Date;
  predictedDemand: number;
  lowerBound: number;
  upperBound: number;
  confidence: number;
  seasonalityFactor?: number;
  trendFactor?: number;
  confidenceUpper?: number;
  confidenceLower?: number;
}

export type ForecastStatus = 'active' | 'archived' | 'obsolete';

export interface DemandForecast {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  warehouseId?: string;
  warehouseName?: string;
  period: ForecastPeriod;
  algorithm: ForecastAlgorithm;
  status: ForecastStatus;
  historicalData: HistoricalData[];
  forecastData: ForecastData[];
  startDate: Date;
  endDate: Date;
  generatedAt: Date;
  generatedBy: string;
  lastUpdatedAt: Date;
  totalPredictedDemand: number;
  averageDailyDemand: number;
  peakDemandDate?: Date;
  peakDemandQuantity?: number;
  safetyStockRecommendation: number;
  reorderPointRecommendation: number;
  notes?: string;
  product: InventoryProduct;
  avgDailySales: number;
  trend: 'up' | 'down' | 'stable';
  trendPercentage?: number;
  alertLevel: 'none' | 'low' | 'medium' | 'high';
  forecasts?: ForecastData[];
  forecastHorizon: number;
  currentStock: number;
}

export interface ForecastAccuracy {
  forecastId: string;
  productId: string;
  sku: string;
  period: ForecastPeriod;
  actualDemand: number;
  predictedDemand: number;
  error: number;
  errorPercentage: number;
  mape: number; // Mean Absolute Percentage Error
  mae: number; // Mean Absolute Error
  rmse: number; // Root Mean Square Error
  bias: number; // Forecast Bias
  accuracyScore: number;
  evaluatedAt: Date;
  lastTested?: Date;
}
