import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { TaxRateDTO, TaxJurisdictionDTO, TaxReportDTO, CreateTaxRateRequest, TaxType } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-tax', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './tax.html', styleUrl: './tax.scss' })
export class Tax implements OnInit {
  readonly parseFloat = parseFloat;
  private svc = inject(FinanceService);
  rates = signal<TaxRateDTO[]>([]); jurisdictions = signal<TaxJurisdictionDTO[]>([]); reports = signal<TaxReportDTO[]>([]);
  isLoading = signal(false); actionLoading = signal<string | null>(null);
  activeTab = signal<'rates' | 'jurisdictions' | 'reports'>('rates');
  showCreateRate = signal(false); showGenReport = signal(false);
  newRate = signal<Partial<CreateTaxRateRequest>>({ applicability: 'ALL' });
  genReport = signal<{ periodId: string; reportType: string }>({ periodId: '', reportType: 'SALES_TAX' });
  showActiveOnly = signal(false);
  taxTypes = ['SALES_TAX', 'VAT', 'GST', 'HST', 'PST', 'EXCISE', 'WITHHOLDING', 'CUSTOM'];

  ngOnInit(): void { this.load(); }
  load(): void {
    this.isLoading.set(true);
    this.svc.getTaxRates(this.showActiveOnly()).subscribe(r => { this.rates.set(r); this.isLoading.set(false); });
    this.svc.getJurisdictions().subscribe(j => this.jurisdictions.set(j));
    this.svc.getTaxReports().subscribe(p => this.reports.set(p.content));
  }
  toggleActive(id: string, active: boolean): void {
    const obs = active ? this.svc.deactivateTaxRate(id) : this.svc.activateTaxRate(id);
    this.actionLoading.set(id); obs.subscribe(() => { this.actionLoading.set(null); this.load(); });
  }
  submitCreateRate(): void {
    const r = this.newRate(); if (!r.rateCode || !r.name || !r.taxType || r.rate == null) return;
    this.actionLoading.set('create'); this.svc.createTaxRate(r as CreateTaxRateRequest).subscribe(() => { this.actionLoading.set(null); this.showCreateRate.set(false); this.newRate.set({ applicability: 'ALL' }); this.load(); });
  }
  submitGenReport(): void {
    const g = this.genReport(); if (!g.periodId || !g.reportType) return;
    this.actionLoading.set('gen'); this.svc.generateTaxReport(g.periodId, g.reportType).subscribe(() => { this.actionLoading.set(null); this.showGenReport.set(false); this.load(); });
  }
  fileReport(id: string): void { this.actionLoading.set(id); this.svc.fileTaxReport(id).subscribe(() => { this.actionLoading.set(null); this.load(); }); }
  patchRate(patch: Partial<CreateTaxRateRequest>): void { this.newRate.update(v => ({ ...v, ...patch })); }
  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
}
