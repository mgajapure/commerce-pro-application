import { Component, signal, computed, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiInsight, AiFeatureType } from '../../../core/models/ai/ai.model';

@Component({
  selector: 'app-ai-insights',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-insights.html',
  styleUrl: './ai-insights.scss'
})
export class AiInsights implements OnInit, OnDestroy {
  private aiSvc   = inject(AiService);
  private destroy$ = new Subject<void>();

  insights      = signal<AiInsight[]>([]);
  totalElements = signal(0);
  totalPages    = signal(0);
  isLoading     = signal(false);
  error         = signal<string | null>(null);
  expandedId    = signal<string | null>(null);

  // Filters
  filterFeature:    string = '';
  filterRiskLevel:  string = '';
  filterEntityType: string = '';
  page = 0;
  size = 20;

  readonly featureOptions: AiFeatureType[] = [
    'FRAUD_DETECTION', 'CHURN_PREDICTION', 'SENTIMENT_ANALYSIS', 'PRODUCT_DESCRIPTION',
    'SUPPORT_CHATBOT', 'DEMAND_FORECAST', 'NL_REPORT', 'PRICING_RECOMMENDATION',
    'INVENTORY_OPTIMIZATION', 'BUDGET_ANOMALY', 'SEO_OPTIMIZER',
    'MARKETING_PERSONALIZATION', 'RETURN_PATTERN', 'VENDOR_ANALYSIS', 'SHIPPING_OPTIMIZATION'
  ];

  readonly riskLevels = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL', 'POSITIVE', 'NEGATIVE', 'NEUTRAL'];
  readonly entityTypes = ['TRANSACTION', 'CUSTOMER', 'PRODUCT', 'ORDER', 'VENDOR', 'BUDGET', 'SHIPMENT'];

  pages = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i));

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  load(): void {
    this.isLoading.set(true);
    this.error.set(null);

    const opts: Parameters<AiService['getInsights']>[0] = {
      page: this.page,
      size: this.size
    };
    if (this.filterFeature)    opts.feature    = this.filterFeature as AiFeatureType;
    if (this.filterRiskLevel)  opts.riskLevel  = this.filterRiskLevel;
    if (this.filterEntityType) opts.entityType = this.filterEntityType;

    this.aiSvc.getInsights(opts).pipe(takeUntil(this.destroy$)).subscribe({
      next: p => {
        this.insights.set(p.content);
        this.totalElements.set(p.totalElements);
        this.totalPages.set(p.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load AI insights');
        this.isLoading.set(false);
      }
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.load();
  }

  clearFilters(): void {
    this.filterFeature    = '';
    this.filterRiskLevel  = '';
    this.filterEntityType = '';
    this.page = 0;
    this.load();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this.page = p;
    this.load();
  }

  toggleExpand(id: string): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  featureLabel(f: AiFeatureType | string): string {
    const map: Record<string, string> = {
      FRAUD_DETECTION: 'Fraud Detection', CHURN_PREDICTION: 'Churn Prediction',
      SENTIMENT_ANALYSIS: 'Sentiment Analysis', PRODUCT_DESCRIPTION: 'Product Description',
      SUPPORT_CHATBOT: 'Support Chatbot', DEMAND_FORECAST: 'Demand Forecast',
      NL_REPORT: 'NL Report', PRICING_RECOMMENDATION: 'Pricing',
      INVENTORY_OPTIMIZATION: 'Inventory', BUDGET_ANOMALY: 'Budget Anomaly',
      SEO_OPTIMIZER: 'SEO Optimiser', MARKETING_PERSONALIZATION: 'Marketing',
      RETURN_PATTERN: 'Return Pattern', VENDOR_ANALYSIS: 'Vendor Analysis',
      SHIPPING_OPTIMIZATION: 'Shipping'
    };
    return map[f] ?? f;
  }

  riskBadgeClass(level: string | null): string {
    switch (level) {
      case 'LOW':      return 'bg-green-100 text-green-800';
      case 'MEDIUM':   return 'bg-yellow-100 text-yellow-800';
      case 'HIGH':     return 'bg-orange-100 text-orange-800';
      case 'CRITICAL': return 'bg-red-100 text-red-800';
      case 'POSITIVE': return 'bg-blue-100 text-blue-800';
      case 'NEGATIVE': return 'bg-red-100 text-red-800';
      case 'NEUTRAL':  return 'bg-gray-100 text-gray-700';
      default:         return 'bg-gray-100 text-gray-600';
    }
  }

  reviewBadgeClass(status: string): string {
    switch (status) {
      case 'ACCEPTED':        return 'bg-green-100 text-green-800';
      case 'REJECTED':        return 'bg-red-100 text-red-800';
      case 'OVERRIDDEN':      return 'bg-purple-100 text-purple-800';
      case 'PENDING_REVIEW':  return 'bg-yellow-100 text-yellow-800';
      default:                return 'bg-gray-100 text-gray-600';
    }
  }

  scoreBarColor(score: number | null): string {
    if (score == null) return 'bg-gray-300';
    if (score >= 80) return 'bg-red-500';
    if (score >= 50) return 'bg-yellow-500';
    return 'bg-green-500';
  }

  fmt(n: number | null, decimals = 4): string {
    return n != null ? '$' + n.toFixed(decimals) : '—';
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleString('en-GB', { dateStyle: 'short', timeStyle: 'short' });
  }
}
