import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AiService } from '../../../../core/services/ai/ai.service';

@Component({
  selector: 'app-ai-insights',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ai-insights.html',
  styleUrl: './ai-insights.scss'
})
export class AiInsights implements OnInit {
  private aiSvc = inject(AiService);

  isLoading = signal(false);
  callsToday = signal(0);
  spendToday = signal(0);
  successRate = signal(0);

  readonly aiFeatures = [
    { icon: 'shield-exclamation', label: 'Fraud Detection', route: '/payments/transactions', color: 'text-red-500', bg: 'bg-red-50' },
    { icon: 'person-dash', label: 'Churn Prediction', route: '/customers', color: 'text-orange-500', bg: 'bg-orange-50' },
    { icon: 'graph-up-arrow', label: 'Demand Forecast', route: '/inventory/forecasting', color: 'text-blue-500', bg: 'bg-blue-50' },
    { icon: 'tag', label: 'Price Optimizer', route: '/catalog/products', color: 'text-green-500', bg: 'bg-green-50' },
    { icon: 'chat-dots', label: 'Support Chatbot', route: '/ai/chatbot-sessions', color: 'text-violet-500', bg: 'bg-violet-50' },
    { icon: 'search', label: 'SEO Optimizer', route: '/catalog/seo', color: 'text-indigo-500', bg: 'bg-indigo-50' },
  ];

  ngOnInit(): void {
    this.isLoading.set(true);
    this.aiSvc.getDashboard().subscribe({
      next: d => {
        this.callsToday.set(d.totalCallsToday ?? 0);
        this.spendToday.set(d.todaySpendUsd ?? 0);
        const rate = d.totalCallsToday > 0 ? Math.round((d.successCallsToday / d.totalCallsToday) * 100) : 0;
        this.successRate.set(rate);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }
}
