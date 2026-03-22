import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { ExchangeRateDTO, ConversionResultDTO, AddExchangeRateRequest } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-exchange-rates', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './exchange-rates.html', styleUrl: './exchange-rates.scss' })
export class ExchangeRates implements OnInit {
  readonly parseFloat = parseFloat;
  private svc = inject(FinanceService);
  rates = signal<ExchangeRateDTO[]>([]); isLoading = signal(false); actionLoading = signal<string | null>(null);
  showAddModal = signal(false);
  newRate = signal<Partial<AddExchangeRateRequest>>({ source: 'MANUAL', rateDate: new Date().toISOString().split('T')[0] });
  // Currency converter
  fromCurrency = signal('USD'); toCurrency = signal('EUR'); convertAmount = signal('100');
  conversionResult = signal<ConversionResultDTO | null>(null);
  ngOnInit(): void { this.load(); }
  load(): void { this.isLoading.set(true); this.svc.getExchangeRates().subscribe(r => { this.rates.set(r); this.isLoading.set(false); }); }
  convert(): void {
    const amt = parseFloat(this.convertAmount()); if (!amt) return;
    this.svc.convertCurrency(this.fromCurrency(), this.toCurrency(), amt).subscribe(r => this.conversionResult.set(r));
  }
  submitAdd(): void {
    const r = this.newRate(); if (!r.baseCurrency || !r.quoteCurrency || !r.rate || !r.rateDate) return;
    this.actionLoading.set('add'); this.svc.addExchangeRate(r as AddExchangeRateRequest).subscribe(() => { this.actionLoading.set(null); this.showAddModal.set(false); this.newRate.set({ source: 'MANUAL', rateDate: new Date().toISOString().split('T')[0] }); this.load(); });
  }
  patchNew(patch: Partial<AddExchangeRateRequest>): void { this.newRate.update(v => ({ ...v, ...patch })); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }

  // Journal TS stub
}
