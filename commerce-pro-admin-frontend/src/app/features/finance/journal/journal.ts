import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { JournalEntrySummaryDTO, JournalEntryDTO, JournalEntryType } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-journal', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './journal.html', styleUrl: './journal.scss' })
export class Journal implements OnInit {
  private svc = inject(FinanceService);
  entries = signal<JournalEntrySummaryDTO[]>([]); detail = signal<JournalEntryDTO | null>(null);
  isLoading = signal(false); actionLoading = signal<string | null>(null);
  totalElements = signal(0); totalPages = signal(0); currentPage = signal(1);
  filterType = signal<JournalEntryType | ''>(''); filterStatus = signal('');
  showDetail = signal(false); showReverseModal = signal<string | null>(null); reverseReason = signal('');
  entryTypes = ['ORDER_REVENUE','ORDER_REFUND','ORDER_TAX','COGS','EXPENSE','AP_PAYMENT','AR_RECEIPT','PAYMENT_FEE','CHARGEBACK','MANUAL_ADJUSTMENT'];
  ngOnInit(): void { this.load(); }
  load(): void {
    this.isLoading.set(true);
    this.svc.getJournalEntries({ entryType: this.filterType() as JournalEntryType || undefined, status: this.filterStatus() || undefined },
      { page: this.currentPage()-1, size: 20, sort: 'entryDate', direction: 'desc' }).subscribe(p => {
      this.entries.set(p.content); this.totalElements.set(p.totalElements); this.totalPages.set(p.totalPages); this.isLoading.set(false);
    });
  }
  openDetail(id: string): void { this.svc.getJournalEntryById(id).subscribe(d => { this.detail.set(d); this.showDetail.set(true); }); }
  submitReverse(): void {
    const id = this.showReverseModal(); if (!id) return;
    this.actionLoading.set(id); this.svc.reverseJournalEntry(id, this.reverseReason() || 'Reversal').subscribe(() => { this.actionLoading.set(null); this.showReverseModal.set(null); this.showDetail.set(false); this.load(); });
  }
  goToPage(p: number): void { this.currentPage.set(p); this.load(); }
  prevPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p-1); this.load(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p+1); this.load(); } }
  typeColor(t: string): string {
    const m: Record<string,string> = { ORDER_REVENUE:'bg-emerald-100 text-emerald-700', ORDER_REFUND:'bg-rose-100 text-rose-700', EXPENSE:'bg-orange-100 text-orange-700', AP_PAYMENT:'bg-red-100 text-red-700', AR_RECEIPT:'bg-green-100 text-green-700', PAYMENT_FEE:'bg-gray-100 text-gray-600', MANUAL_ADJUSTMENT:'bg-purple-100 text-purple-700' };
    return m[t] ?? 'bg-gray-100 text-gray-600';
  }
  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
}
