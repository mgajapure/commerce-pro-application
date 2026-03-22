import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { VendorSummaryDTO, VendorDTO, VendorStatus, CreateVendorRequest } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-vendors', standalone: true, imports: [CommonModule, FormsModule, RouterModule], templateUrl: './vendors.html', styleUrl: './vendors.scss' })
export class Vendors implements OnInit {
  private svc = inject(FinanceService);
  vendors = signal<VendorSummaryDTO[]>([]); detail = signal<VendorDTO | null>(null);
  isLoading = signal(false); actionLoading = signal<string | null>(null);
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
}
