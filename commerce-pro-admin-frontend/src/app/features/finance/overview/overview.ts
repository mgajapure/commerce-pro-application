import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { RevenueOverviewDTO, ProfitLossDTO } from '../../../core/models/finance/finance.model';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './overview.html',
  styleUrl: './overview.scss'
})
export class Overview implements OnInit {
  readonly Math = Math;
  private svc = inject(FinanceService);

  overview   = signal<RevenueOverviewDTO | null>(null);
  pnl        = signal<ProfitLossDTO | null>(null);
  isLoading  = signal(true);
  pnlMode    = signal<'mtd' | 'ytd' | 'custom'>('mtd');
  customFrom = signal('');
  customTo   = signal('');

  kpiCards = computed(() => {
    const o = this.overview();
    if (!o) return [];
    return [
      { label: 'Revenue Today',   value: this.fmt(o.grossRevenueToday),  icon: 'graph-up-arrow',    bg: 'bg-emerald-100', ic: 'text-emerald-600', sub: `${o.ordersToday} orders` },
      { label: 'Revenue MTD',     value: this.fmt(o.grossRevenueMTD),    icon: 'cash-stack',         bg: 'bg-blue-100',   ic: 'text-blue-600',    sub: `${o.ordersMTD} orders` },
      { label: 'Net Revenue YTD', value: this.fmt(o.netRevenueYTD),      icon: 'currency-dollar',    bg: 'bg-indigo-100', ic: 'text-indigo-600',  sub: 'Year to date' },
      { label: 'Gross Margin',    value: (o.grossMarginPct ?? 0).toFixed(1) + '%', icon: 'percent', bg: 'bg-purple-100', ic: 'text-purple-600',  sub: 'MTD gross margin' },
      { label: 'Outstanding AR',  value: this.fmt(o.outstandingAR),      icon: 'inbox-fill',         bg: 'bg-amber-100',  ic: 'text-amber-600',   sub: `${this.fmt(o.overdueAR)} overdue`, route: '/finance/ar' },
      { label: 'Outstanding AP',  value: this.fmt(o.outstandingAP),      icon: 'box-arrow-up',       bg: 'bg-rose-100',   ic: 'text-rose-600',    sub: `${this.fmt(o.overdueAP)} overdue`, route: '/finance/ap' },
      { label: 'Expenses MTD',    value: this.fmt(o.totalExpensesMTD),   icon: 'receipt',            bg: 'bg-orange-100', ic: 'text-orange-600',  sub: 'Operating expenses', route: '/finance/expenses' },
      { label: 'Tax Collected',   value: this.fmt(o.totalTaxCollectedMTD),icon: 'building-fill',     bg: 'bg-teal-100',   ic: 'text-teal-600',    sub: 'MTD' },
    ];
  });

  ngOnInit(): void {
    this.svc.getRevenueOverview().subscribe(o => { this.overview.set(o); this.isLoading.set(false); });
    this.loadPnL();
  }

  loadPnL(): void {
    const obs = this.pnlMode() === 'ytd' ? this.svc.getPnLYTD() : this.svc.getPnLMTD();
    obs.subscribe(p => this.pnl.set(p));
  }

  setPnLMode(m: 'mtd' | 'ytd' | 'custom'): void { this.pnlMode.set(m); if (m !== 'custom') this.loadPnL(); }

  loadCustomPnL(): void {
    if (!this.customFrom() || !this.customTo()) return;
    this.svc.getPnL(this.customFrom(), this.customTo()).subscribe(p => this.pnl.set(p));
  }

  pnlRows = computed(() => {
    const p = this.pnl();
    if (!p) return [];
    return [
      { label: 'Gross Revenue',        value: p.grossRevenue,             indent: 0, bold: false, positive: true },
      { label: 'Sales Returns',         value: -p.salesReturns,           indent: 1, bold: false, positive: false },
      { label: 'Net Revenue',           value: p.netRevenue,              indent: 0, bold: true,  positive: true, separator: true },
      { label: 'Cost of Goods Sold',    value: -p.cogs,                   indent: 1, bold: false, positive: false },
      { label: 'Gross Profit',          value: p.grossProfit,             indent: 0, bold: true,  positive: p.grossProfit >= 0, separator: true },
      { label: 'Operating Expenses',    value: -p.totalOperatingExpenses, indent: 1, bold: false, positive: false },
      { label: 'EBITDA',               value: p.ebitda,                  indent: 0, bold: true,  positive: p.ebitda >= 0, separator: true },
      { label: 'Tax',                   value: -p.totalTax,               indent: 1, bold: false, positive: false },
      { label: 'Net Income',            value: p.netIncome,               indent: 0, bold: true,  positive: p.netIncome >= 0, separator: true },
    ];
  });

  expenseBreakdown = computed(() => {
    const p = this.pnl();
    if (!p?.expenseByCategory) return [];
    return Object.entries(p.expenseByCategory)
      .map(([cat, amt]) => ({ cat, amt: amt as number }))
      .sort((a, b) => b.amt - a.amt)
      .slice(0, 6);
  });

  maxExpense = computed(() => Math.max(...this.expenseBreakdown().map(e => e.amt), 1));

  fmt(n: number | null | undefined): string {
    if (n == null) return '$0.00';
    const abs = Math.abs(n);
    const prefix = n < 0 ? '-$' : '$';
    if (abs >= 1_000_000) return prefix + (abs / 1_000_000).toFixed(2) + 'M';
    if (abs >= 1_000)     return prefix + (abs / 1_000).toFixed(1) + 'K';
    return prefix + abs.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  fmtFull(n: number | null | undefined): string {
    if (n == null) return '$0.00';
    const prefix = n < 0 ? '-$' : '$';
    return prefix + Math.abs(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
