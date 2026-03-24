import { Component, signal, computed, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AiService } from '../../../core/services/ai/ai.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { AiDashboardResponse, AiUsageSummaryItem, AiFeatureType } from '../../../core/models/ai/ai.model';

@Component({
  selector: 'app-ai-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, AlertComponent],
  templateUrl: './ai-dashboard.html',
  styleUrl: './ai-dashboard.scss'
})
export class AiDashboard implements OnInit, OnDestroy {
  private aiSvc    = inject(AiService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  dashboard    = signal<AiDashboardResponse | null>(null);
  usage        = signal<AiUsageSummaryItem[]>([]);
  isLoading    = signal(false);
  error        = signal<string | null>(null);

  spendPercent = computed(() => {
    const d = this.dashboard();
    if (!d || !d.dailyBudgetUsd) return 0;
    return Math.min(100, Math.round((d.todaySpendUsd / d.dailyBudgetUsd) * 100));
  });

  successRate = computed(() => {
    const d = this.dashboard();
    if (!d || !d.totalCallsToday) return 0;
    return Math.round((d.successCallsToday / d.totalCallsToday) * 100);
  });

  topFeatureBySpend = computed(() =>
    [...this.usage()].sort((a, b) => b.totalCostUsd - a.totalCostUsd).slice(0, 5)
  );

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  load(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.aiSvc.getDashboard().pipe(takeUntil(this.destroy$)).subscribe({
      next: d => { this.dashboard.set(d); this.loadUsage(); },
      error: () => { this.error.set('Failed to load AI dashboard'); this.alertSvc.error('Failed to load AI dashboard'); this.isLoading.set(false); }
    });
  }

  private loadUsage(): void {
    this.aiSvc.getUsage().pipe(takeUntil(this.destroy$)).subscribe({
      next: u => { this.usage.set(u); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  featureLabel(f: AiFeatureType): string {
    const map: Record<string, string> = {
      FRAUD_DETECTION: 'Fraud Detection', CHURN_PREDICTION: 'Churn Prediction',
      SENTIMENT_ANALYSIS: 'Sentiment Analysis', PRODUCT_DESCRIPTION: 'Product Description',
      SUPPORT_CHATBOT: 'Support Chatbot', DEMAND_FORECAST: 'Demand Forecast',
      NL_REPORT: 'NL Report', PRICING_RECOMMENDATION: 'Pricing', INVENTORY_OPTIMIZATION: 'Inventory Optimisation',
      BUDGET_ANOMALY: 'Budget Anomaly', SEO_OPTIMIZER: 'SEO Optimiser',
      MARKETING_PERSONALIZATION: 'Marketing', RETURN_PATTERN: 'Return Pattern',
      VENDOR_ANALYSIS: 'Vendor Analysis', SHIPPING_OPTIMIZATION: 'Shipping Optimisation'
    };
    return map[f] ?? f;
  }

  featureIcon(f: AiFeatureType): string {
    const map: Record<string, string> = {
      FRAUD_DETECTION: 'shield-exclamation', CHURN_PREDICTION: 'person-dash',
      SENTIMENT_ANALYSIS: 'emoji-smile', PRODUCT_DESCRIPTION: 'file-text',
      SUPPORT_CHATBOT: 'chat-dots', DEMAND_FORECAST: 'graph-up-arrow',
      NL_REPORT: 'bar-chart-line', PRICING_RECOMMENDATION: 'tag',
      INVENTORY_OPTIMIZATION: 'box-seam', BUDGET_ANOMALY: 'exclamation-triangle',
      SEO_OPTIMIZER: 'search', MARKETING_PERSONALIZATION: 'megaphone',
      RETURN_PATTERN: 'arrow-return-left', VENDOR_ANALYSIS: 'building',
      SHIPPING_OPTIMIZATION: 'truck'
    };
    return map[f] ?? 'cpu';
  }

  spendBarWidth(item: AiUsageSummaryItem): number {
    const max = Math.max(...this.usage().map(u => u.totalCostUsd), 0.01);
    return Math.round((item.totalCostUsd / max) * 100);
  }

  fmt(n: number): string {
    return '$' + (n ?? 0).toFixed(4);
  }

  fmtUsd(n: number): string {
    return '$' + (n ?? 0).toFixed(2);
  }

  formatNum(n: number): string {
    return (n ?? 0).toLocaleString('en-US');
  }

  successRateColor(rate: number): string {
    if (rate >= 95) return 'text-green-600';
    if (rate >= 80) return 'text-yellow-600';
    return 'text-red-600';
  }

  spendBarColor(pct: number): string {
    if (pct >= 90) return 'bg-red-500';
    if (pct >= 70) return 'bg-yellow-500';
    return 'bg-violet-500';
  }
}
