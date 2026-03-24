import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiConfigResponse, AiConfigUpdateRequest, AiFeatureType } from '../../../core/models/ai/ai.model';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-ai-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-config.html',
  styleUrl: './ai-config.scss'
})
export class AiConfig implements OnInit, OnDestroy {
  private aiSvc    = inject(AiService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  configs   = signal<AiConfigResponse[]>([]);
  isLoading = signal(false);
  error     = signal<string | null>(null);
  saving    = signal<string | null>(null);

  // Edit modal
  editTarget = signal<AiConfigResponse | null>(null);
  editForm   = signal<AiConfigUpdateRequest>({});
  showEdit   = signal(false);

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  load(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.aiSvc.getConfigs().pipe(takeUntil(this.destroy$)).subscribe({
      next: c => { this.configs.set(c); this.isLoading.set(false); },
      error: () => { this.error.set('Failed to load configurations'); this.isLoading.set(false); }
    });
  }

  toggleEnabled(cfg: AiConfigResponse): void {
    this.saving.set(cfg.feature);
    this.aiSvc.updateConfig(cfg.feature, { enabled: !cfg.enabled })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: updated => {
          this.configs.update(list => list.map(c => c.feature === updated.feature ? updated : c));
          this.saving.set(null);
          this.alertSvc.success(`${this.featureLabel(cfg.feature)} ${updated.enabled ? 'enabled' : 'disabled'}`);
        },
        error: () => { this.saving.set(null); this.alertSvc.error('Failed to update feature'); }
      });
  }

  openEdit(cfg: AiConfigResponse): void {
    this.editTarget.set(cfg);
    this.editForm.set({
      model: cfg.model,
      maxTokens: cfg.maxTokens,
      dailyBudgetUsd: cfg.dailyBudgetUsd,
      rateLimitPerMinute: cfg.rateLimitPerMinute,
      rateLimitPerHour: cfg.rateLimitPerHour,
      fallbackStrategy: cfg.fallbackStrategy,
      defaultScore: cfg.defaultScore
    });
    this.showEdit.set(true);
  }

  closeEdit(): void { this.showEdit.set(false); this.editTarget.set(null); }

  saveEdit(): void {
    const target = this.editTarget();
    if (!target) return;
    this.saving.set(target.feature);
    this.aiSvc.updateConfig(target.feature, this.editForm())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: updated => {
          this.configs.update(list => list.map(c => c.feature === updated.feature ? updated : c));
          this.saving.set(null);
          this.closeEdit();
          this.alertSvc.success('Configuration saved');
        },
        error: () => { this.saving.set(null); this.alertSvc.error('Failed to save configuration'); }
      });
  }

  featureLabel(f: AiFeatureType): string {
    const map: Record<string, string> = {
      FRAUD_DETECTION: 'Fraud Detection', CHURN_PREDICTION: 'Churn Prediction',
      SENTIMENT_ANALYSIS: 'Sentiment Analysis', PRODUCT_DESCRIPTION: 'Product Description',
      SUPPORT_CHATBOT: 'Support Chatbot', DEMAND_FORECAST: 'Demand Forecast',
      NL_REPORT: 'NL Report', PRICING_RECOMMENDATION: 'Pricing Recommendation',
      INVENTORY_OPTIMIZATION: 'Inventory Optimisation', BUDGET_ANOMALY: 'Budget Anomaly',
      SEO_OPTIMIZER: 'SEO Optimiser', MARKETING_PERSONALIZATION: 'Marketing Personalisation',
      RETURN_PATTERN: 'Return Pattern Analysis', VENDOR_ANALYSIS: 'Vendor Analysis',
      SHIPPING_OPTIMIZATION: 'Shipping Optimisation'
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

  patchForm(key: keyof AiConfigUpdateRequest, val: any): void {
    this.editForm.update(f => ({ ...f, [key]: val }));
  }
}
