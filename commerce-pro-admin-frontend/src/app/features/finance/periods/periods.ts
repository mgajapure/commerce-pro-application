import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { FinancialPeriodDTO, PeriodStatus, CreatePeriodRequest } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-periods', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './periods.html', styleUrl: './periods.scss' })
export class Periods implements OnInit {
  readonly parseInt = parseInt;
  private svc = inject(FinanceService);
  periods = signal<FinancialPeriodDTO[]>([]); isLoading = signal(false); actionLoading = signal<string | null>(null);
  showCreateModal = signal(false); confirmModal = signal<{ id: string; action: 'close' | 'lock' | 'reopen'; label: string } | null>(null);
  confirmNotes = signal('');
  newPeriod = signal<Partial<CreatePeriodRequest>>({ periodType: 'MONTHLY' });
  currentYear = new Date().getFullYear();
  ngOnInit(): void { this.load(); }
  load(): void {
    this.isLoading.set(true);
    this.svc.getPeriods({ size: 24 }).subscribe(p => { this.periods.set(p.content); this.isLoading.set(false); });
  }
  openConfirm(id: string, action: 'close' | 'lock' | 'reopen', label: string): void { this.confirmModal.set({ id, action, label }); this.confirmNotes.set(''); }
  submitAction(): void {
    const c = this.confirmModal(); if (!c) return;
    this.actionLoading.set(c.id);
    const obs = c.action === 'close' ? this.svc.closePeriod(c.id, this.confirmNotes()) :
                c.action === 'lock'  ? this.svc.lockPeriod(c.id, this.confirmNotes()) :
                                       this.svc.reopenPeriod(c.id, this.confirmNotes());
    obs.subscribe(() => { this.actionLoading.set(null); this.confirmModal.set(null); this.load(); });
  }
  submitCreate(): void {
    const p = this.newPeriod(); if (!p.fiscalYear || !p.periodNumber || !p.periodName || !p.startDate || !p.endDate) return;
    this.actionLoading.set('create'); this.svc.createPeriod(p as CreatePeriodRequest).subscribe(() => { this.actionLoading.set(null); this.showCreateModal.set(false); this.newPeriod.set({ periodType: 'MONTHLY' }); this.load(); });
  }
  patchNew(patch: Partial<CreatePeriodRequest>): void { this.newPeriod.update(v => ({ ...v, ...patch })); }
  getStatusCfg(s: string) {
    const m: Record<string,any> = {
      OPEN:    { badge: 'bg-green-100 text-green-800',  icon: 'bi-unlock-fill text-green-600',  label: 'Open' },
      CLOSING: { badge: 'bg-yellow-100 text-yellow-800',icon: 'bi-clock text-yellow-600',        label: 'Closing' },
      CLOSED:  { badge: 'bg-gray-100 text-gray-700',    icon: 'bi-lock text-gray-500',           label: 'Closed' },
      LOCKED:  { badge: 'bg-slate-200 text-slate-700',  icon: 'bi-lock-fill text-slate-600',     label: 'Locked' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', icon: 'bi-question', label: s };
  }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
}
