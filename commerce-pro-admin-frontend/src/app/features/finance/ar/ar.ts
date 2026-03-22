import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { ARSummaryDTO } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-ar', standalone: true, imports: [CommonModule, RouterModule], templateUrl: './ar.html', styleUrl: './ar.scss' })
export class AR implements OnInit {
  private svc = inject(FinanceService);
  summary = signal<ARSummaryDTO | null>(null);
  isLoading = signal(true);
  ngOnInit(): void { this.svc.getARSummary().subscribe(s => { this.summary.set(s); this.isLoading.set(false); }); }
  buckets = [
    { label: 'Current', key: 'current', color: 'bg-green-500', text: 'text-green-700', bg: 'bg-green-50 border-green-100' },
    { label: '1–30 Days', key: 'days1To30', color: 'bg-yellow-400', text: 'text-yellow-700', bg: 'bg-yellow-50 border-yellow-100' },
    { label: '31–60 Days', key: 'days31To60', color: 'bg-orange-400', text: 'text-orange-700', bg: 'bg-orange-50 border-orange-100' },
    { label: '61–90 Days', key: 'days61To90', color: 'bg-red-400', text: 'text-red-700', bg: 'bg-red-50 border-red-100' },
    { label: '90+ Days', key: 'days90Plus', color: 'bg-red-700', text: 'text-red-900', bg: 'bg-red-100 border-red-200' },
  ];
  getVal(key: string): number { const s = this.summary(); return s ? (s as any)[key] ?? 0 : 0; }
  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
  statusBadge(s: string): string {
    const m: Record<string,string> = { SENT:'bg-blue-100 text-blue-800', OVERDUE:'bg-red-100 text-red-800', PARTIALLY_PAID:'bg-yellow-100 text-yellow-800' };
    return m[s] ?? 'bg-gray-100 text-gray-700';
  }
}
