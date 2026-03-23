import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { VendorSummaryDTO, VendorDTO, VendorStatus, CreateVendorRequest } from '../../../core/models/finance/finance.model';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiVendorResult } from '../../../core/models/ai/ai.model';
import { Subject, takeUntil } from 'rxjs';

@Component({ selector: 'app-vendors', standalone: true, imports: [CommonModule, FormsModule, RouterModule], templateUrl: './vendors.html', styleUrl: './vendors.scss' })
export class Vendors implements OnInit, OnDestroy {
  private svc    = inject(FinanceService);
  private aiSvc  = inject(AiService);
  private destroy$ = new Subject<void>();
  vendors = signal<VendorSummaryDTO[]>([]); detail = signal<VendorDTO | null>(null);
  isLoading = signal(false); actionLoading = signal<string | null>(null);

  // AI Vendor Analysis
  vendorAiResults  = signal<Record<string, AiVendorResult>>({});
  vendorAiLoading  = signal<string | null>(null);
  showVendorAiModal = signal(false);
  vendorAiModalResult = signal<AiVendorResult | null>(null);
  totalElements = signal(0); totalPages = signal(0); currentPage = signal(1);
  filterStatus = signal(''); filterSearch = signal('');
  showDetail = signal(false); showModal = signal(false);
  newVendor = signal<Partial<CreateVendorRequest>>({ paymentTerms: 'Net 30', preferredCurrency: 'USD' });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    this.svc.getVendors(this.filterSearch() || undefined, this.filterStatus() as VendorStatus || undefined, undefined, { page: this.currentPage()-1, size: 20 }).subscribe(p => {
      this.vendors.set(p.content); this.totalElements.set(p.totalElements); this.totalPages.set(p.totalPages); this.isLoading.set(false);
    });
  }

  onSearch(): void { this.currentPage.set(1); this.load(); }

  openDetail(id: string): void { this.svc.getVendorById(id).subscribe(d => { this.detail.set(d); this.showDetail.set(true); }); }

  submitCreate(): void {
    const v = this.newVendor();
    if (!v.name || !v.email || !v.paymentTerms) return;
    this.actionLoading.set('create');
    this.svc.createVendor(v as CreateVendorRequest).subscribe(() => {
      this.actionLoading.set(null); this.showModal.set(false); this.newVendor.set({ paymentTerms: 'Net 30', preferredCurrency: 'USD' }); this.load();
    });
  }

  patchVendor(patch: Partial<CreateVendorRequest>): void { this.newVendor.update(v => ({ ...v, ...patch })); }
  goToPage(p: number): void { this.currentPage.set(p); this.load(); }
  prevPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p-1); this.load(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p+1); this.load(); } }
  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  analyseVendorAi(vendorId: string): void {
    this.vendorAiLoading.set(vendorId);
    this.aiSvc.analyseVendor(vendorId).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => {
        this.vendorAiResults.update(m => ({ ...m, [vendorId]: r }));
        this.vendorAiModalResult.set(r);
        this.showVendorAiModal.set(true);
        this.vendorAiLoading.set(null);
      },
      error: () => this.vendorAiLoading.set(null)
    });
  }

  getVendorAi(id: string): AiVendorResult | null { return this.vendorAiResults()[id] ?? null; }

  vendorRiskBadge(r: string): string {
    if (r === 'HIGH') return 'bg-red-100 text-red-700';
    if (r === 'MEDIUM') return 'bg-yellow-100 text-yellow-700';
    return 'bg-green-100 text-green-700';
  }
}
