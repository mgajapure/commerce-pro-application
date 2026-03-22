import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { PaymentTransactionSummaryDTO } from '../../../core/models/payment/payment.model';

@Component({
  selector: 'app-pending',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pending.html',
  styleUrl: './pending.scss'
})
export class Pending implements OnInit {
  readonly parseFloat = parseFloat;
  private svc = inject(PaymentService);

  pending      = signal<PaymentTransactionSummaryDTO[]>([]);
  expiring     = signal<PaymentTransactionSummaryDTO[]>([]);
  isLoading    = signal(false);
  activeTab    = signal<'all' | 'expiring'>('all');
  selected     = signal<string[]>([]);
  actionLoading = signal<string | null>(null);
  captureModal = signal<PaymentTransactionSummaryDTO | null>(null);
  captureAmount = signal('');
  error        = signal<string | null>(null);

  displayList = computed(() => this.activeTab() === 'expiring' ? this.expiring() : this.pending());
  isAllSelected = computed(() => this.displayList().length > 0 && this.displayList().every(t => this.selected().includes(t.id)));

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    this.svc.getPendingPayments().subscribe(p => { this.pending.set(p); this.isLoading.set(false); });
    this.svc.getExpiringAuthorizations().subscribe(e => this.expiring.set(e));
  }

  openCapture(txn: PaymentTransactionSummaryDTO): void {
    this.captureModal.set(txn);
    this.captureAmount.set(String(txn.amount));
  }

  submitCapture(): void {
    const t = this.captureModal();
    if (!t) return;
    this.actionLoading.set(t.id);
    const amt = parseFloat(this.captureAmount()) || undefined;
    this.svc.captureTransaction(t.id, amt).subscribe(() => {
      this.actionLoading.set(null); this.captureModal.set(null); this.load();
    });
  }

  voidTxn(id: string): void {
    this.actionLoading.set(id);
    this.svc.voidPending(id).subscribe(() => { this.actionLoading.set(null); this.load(); });
  }

  toggleSelect(id: string): void { this.selected.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]); }
  toggleAll(): void { this.isAllSelected() ? this.selected.set([]) : this.selected.set(this.displayList().map(t => t.id)); }

  expiresIn(iso?: string): string {
    if (!iso) return '—';
    const diff = new Date(iso).getTime() - Date.now();
    if (diff <= 0) return 'Expired';
    const h = Math.floor(diff / 3600000);
    const m = Math.floor((diff % 3600000) / 60000);
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
  }

  expiryClass(iso?: string): string {
    if (!iso) return 'text-gray-500';
    const diff = new Date(iso).getTime() - Date.now();
    if (diff <= 0) return 'text-red-600 font-semibold';
    if (diff < 3600000 * 6) return 'text-orange-600 font-semibold';
    if (diff < 3600000 * 24) return 'text-yellow-600';
    return 'text-gray-500';
  }

  fmt(n: number): string {
    return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  fmtDate(iso: string): string {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }
}
