import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { CashFlowForecastDTO } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-cashflow', standalone: true, imports: [CommonModule], templateUrl: './cashflow.html', styleUrl: './cashflow.scss' })
export class Cashflow implements OnInit {
  private svc = inject(FinanceService);
  forecast = signal<CashFlowForecastDTO | null>(null); isLoading = signal(true);
  ngOnInit(): void { this.svc.getCashFlowForecast().subscribe(f => { this.forecast.set(f); this.isLoading.set(false); }); }
  fmt(n: number | null | undefined): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
}
