import { Component, signal, computed, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule, DecimalPipe, CurrencyPipe } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, forkJoin } from 'rxjs';
import { CustomerApiService } from '../../../core/services/customers/customers.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiChurnResult, AiMarketingResult } from '../../../core/models/ai/ai.model';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import {
  CustomerResponse, CustomerAddress, CommunicationLog,
  Wishlist, SavedCart, CustomerOrderHistory,
  AddressRequest, CommunicationLogRequest,
  BlacklistRequest, FraudFlagRequest, FraudResolveRequest, LoyaltyAdjustRequest
} from '../../../core/models/customers/customers.model';
import { PageResponse } from '../../../core/models/common';

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, Dropdown],
  templateUrl: './customer-detail.html',
  styleUrl: './customer-detail.scss'
})
export class CustomerDetail implements OnInit, OnDestroy {
  private route       = inject(ActivatedRoute);
  public router       = inject(Router);
  private customerSvc = inject(CustomerApiService);
  private alertSvc    = inject(AlertService);
  private aiSvc       = inject(AiService);
  private destroy$    = new Subject<void>();

  // AI
  churnResult      = signal<AiChurnResult | null>(null);
  marketingResult  = signal<AiMarketingResult | null>(null);
  aiChurnLoading   = signal(false);
  aiMktLoading     = signal(false);
  showAiPanel      = signal(false);
  aiError          = signal<string | null>(null);

  customerId    = signal('');
  customer      = signal<CustomerResponse | null>(null);
  isLoading     = signal(true);
  actionLoading = signal(false);
  error         = signal<string | null>(null);
  activeTab     = signal<'overview' | 'orders' | 'addresses' | 'comms' | 'wishlists' | 'carts'>('overview');

  // Tab data
  addresses       = signal<CustomerAddress[]>([]);
  commLogs        = signal<CommunicationLog[]>([]);
  wishlists       = signal<Wishlist[]>([]);
  savedCarts      = signal<SavedCart[]>([]);
  orderHistory    = signal<CustomerOrderHistory[]>([]);
  orderTotal      = signal(0);

  // Modals
  showBlacklistModal   = signal(false);
  showFraudModal       = signal(false);
  showFraudResolveModal= signal(false);
  showLoyaltyModal     = signal(false);
  showCommModal        = signal(false);
  showAddressModal     = signal(false);
  showTagModal         = signal(false);

  // Form fields
  blacklistReason   = signal('');
  fraudReason       = signal('');
  fraudEvidence     = signal('');
  fraudResolution   = signal('');
  loyaltyPoints     = signal(0);
  loyaltyDesc       = signal('');
  newTagInput       = signal('');
  editingTags       = signal<string[]>([]);

  // Comm form
  commChannel   = signal('EMAIL');
  commDirection = signal('OUTBOUND');
  commSubject   = signal('');
  commBody      = signal('');
  commOrderId   = signal('');

  // Address form (simplified)
  addrFullName   = signal('');
  addrLine1      = signal('');
  addrCity       = signal('');
  addrState      = signal('');
  addrPostal     = signal('');
  addrCountry    = signal('US');
  addrType       = signal('SHIPPING');
  addrDefault    = signal(false);

  tabs = [
    { id: 'overview',   label: 'Overview',       icon: 'person-badge' },
    { id: 'orders',     label: 'Orders',         icon: 'bag-check' },
    { id: 'addresses',  label: 'Addresses',      icon: 'geo-alt' },
    { id: 'comms',      label: 'Communications', icon: 'chat-dots' },
    { id: 'wishlists',  label: 'Wishlists',      icon: 'heart' },
    { id: 'carts',      label: 'Saved Carts',    icon: 'cart' },
  ];

  actionItems = computed((): DropdownItem[] => {
    const c = this.customer();
    if (!c) return [];
    const items: DropdownItem[] = [
      { id: 'edit',          label: 'Edit Profile',      icon: 'pencil' },
      { id: 'evalTier',      label: 'Re-evaluate Tier',  icon: 'award' },
      { id: 'loyalty',       label: 'Adjust Loyalty',    icon: 'star' },
      { id: 'divider1', label: '', divider: true },
    ];
    if (!c.isBlacklisted) {
      items.push({ id: 'blacklist', label: 'Blacklist Customer', icon: 'person-fill-slash', danger: true });
    } else {
      items.push({ id: 'unblacklist', label: 'Remove from Blacklist', icon: 'person-check' });
    }
    if (!c.isFraudFlagged || c.fraudResolved) {
      items.push({ id: 'fraud', label: 'Flag for Fraud', icon: 'flag-fill', danger: true });
    } else {
      items.push({ id: 'resolvefraud', label: 'Resolve Fraud Flag', icon: 'shield-check' });
    }
    items.push({ id: 'divider2', label: '', divider: true });
    items.push({ id: 'delete', label: 'Delete Customer', icon: 'trash', danger: true });
    return items;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.router.navigate(['/customers/all']); return; }
    this.customerId.set(id);
    this.loadCustomer(id);
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  loadCustomer(id: string): void {
    this.isLoading.set(true);
    this.customerSvc.getCustomerById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: c => { this.customer.set(c); this.isLoading.set(false); this.loadTabData(); },
      error: () => { this.error.set('Customer not found'); this.isLoading.set(false); }
    });
  }

  loadTabData(): void {
    const id = this.customerId();
    this.customerSvc.getAddresses(id).subscribe(a => this.addresses.set(a));
    this.customerSvc.getCommunicationLogs(id).subscribe(p => this.commLogs.set(p.content));
    this.customerSvc.getWishlists(id).subscribe(w => this.wishlists.set(w));
    this.customerSvc.getSavedCarts(id).subscribe(c => this.savedCarts.set(c));
    this.customerSvc.getOrderHistory(id).subscribe(p => {
      this.orderHistory.set(p.content);
      this.orderTotal.set(p.totalElements);
    });
  }

  reload(): void { this.loadCustomer(this.customerId()); }

  onAction(item: DropdownItem): void {
    switch(item.id) {
      case 'edit':         this.router.navigate(['/customers/edit', this.customerId()]); break;
      case 'evalTier':     this.evalTier(); break;
      case 'loyalty':      this.showLoyaltyModal.set(true); break;
      case 'blacklist':    this.showBlacklistModal.set(true); break;
      case 'unblacklist':  this.doUnblacklist(); break;
      case 'fraud':        this.showFraudModal.set(true); break;
      case 'resolvefraud': this.showFraudResolveModal.set(true); break;
      case 'delete':       this.doDelete(); break;
    }
  }

  evalTier(): void {
    this.actionLoading.set(true);
    this.customerSvc.evaluateTier(this.customerId()).subscribe({
      next: c => { this.customer.set(c); this.actionLoading.set(false); this.alertSvc.success('Tier updated', `Now: ${c.tier}`); },
      error: () => { this.actionLoading.set(false); this.alertSvc.error('Tier evaluation failed'); }
    });
  }

  doBlacklist(): void {
    if (!this.blacklistReason().trim()) { this.alertSvc.warning('Reason required'); return; }
    this.actionLoading.set(true);
    const req: BlacklistRequest = { reason: this.blacklistReason() };
    this.customerSvc.blacklist(this.customerId(), req).subscribe({
      next: c => {
        this.customer.set(c); this.showBlacklistModal.set(false);
        this.blacklistReason.set(''); this.actionLoading.set(false);
        this.alertSvc.success('Customer blacklisted');
      },
      error: () => { this.actionLoading.set(false); this.alertSvc.error('Blacklist failed'); }
    });
  }

  doUnblacklist(): void {
    this.alertSvc.confirm({ title: 'Remove from Blacklist', message: 'Remove this customer from the blacklist?', confirmLabel: 'Remove' })
      .then(ok => {
        if (!ok) return;
        this.customerSvc.unblacklist(this.customerId(), 'Admin removed from blacklist').subscribe({
          next: c => { this.customer.set(c); this.alertSvc.success('Customer unblacklisted'); },
          error: () => this.alertSvc.error('Failed')
        });
      });
  }

  doFlagFraud(): void {
    if (!this.fraudReason().trim()) { this.alertSvc.warning('Reason required'); return; }
    this.actionLoading.set(true);
    const req: FraudFlagRequest = { reason: this.fraudReason(), evidence: this.fraudEvidence() };
    this.customerSvc.flagFraud(this.customerId(), req).subscribe({
      next: c => {
        this.customer.set(c); this.showFraudModal.set(false);
        this.fraudReason.set(''); this.fraudEvidence.set(''); this.actionLoading.set(false);
        this.alertSvc.warning('Customer flagged for fraud');
      },
      error: () => { this.actionLoading.set(false); this.alertSvc.error('Flag failed'); }
    });
  }

  doResolveFraud(): void {
    if (!this.fraudResolution().trim()) { this.alertSvc.warning('Resolution required'); return; }
    this.actionLoading.set(true);
    const req: FraudResolveRequest = { resolution: this.fraudResolution() };
    this.customerSvc.resolveFraud(this.customerId(), req).subscribe({
      next: c => {
        this.customer.set(c); this.showFraudResolveModal.set(false);
        this.fraudResolution.set(''); this.actionLoading.set(false);
        this.alertSvc.success('Fraud flag resolved');
      },
      error: () => { this.actionLoading.set(false); this.alertSvc.error('Resolve failed'); }
    });
  }

  doAdjustLoyalty(): void {
    if (!this.loyaltyDesc().trim()) { this.alertSvc.warning('Description required'); return; }
    this.actionLoading.set(true);
    const req: LoyaltyAdjustRequest = { points: this.loyaltyPoints(), description: this.loyaltyDesc() };
    this.customerSvc.adjustLoyalty(this.customerId(), req).subscribe({
      next: c => {
        this.customer.set(c); this.showLoyaltyModal.set(false);
        this.loyaltyPoints.set(0); this.loyaltyDesc.set(''); this.actionLoading.set(false);
        this.alertSvc.success('Loyalty points adjusted');
      },
      error: () => { this.actionLoading.set(false); this.alertSvc.error('Adjustment failed'); }
    });
  }

  doAddComm(): void {
    if (!this.commSubject().trim()) { this.alertSvc.warning('Subject required'); return; }
    this.actionLoading.set(true);
    const req: CommunicationLogRequest = {
      channel: this.commChannel(), direction: this.commDirection(),
      subject: this.commSubject(), body: this.commBody(), orderId: this.commOrderId() || undefined
    };
    this.customerSvc.addCommunicationLog(this.customerId(), req).subscribe({
      next: log => {
        this.commLogs.update(logs => [log, ...logs]);
        this.showCommModal.set(false);
        this.commSubject.set(''); this.commBody.set(''); this.commOrderId.set('');
        this.actionLoading.set(false);
        this.alertSvc.success('Communication logged');
      },
      error: () => { this.actionLoading.set(false); this.alertSvc.error('Failed to log communication'); }
    });
  }

  doResolveComm(log: CommunicationLog): void {
    this.customerSvc.resolveCommunicationLog(this.customerId(), log.id).subscribe({
      next: updated => {
        this.commLogs.update(logs => logs.map(l => l.id === updated.id ? updated : l));
        this.alertSvc.success('Marked as resolved');
      },
      error: () => this.alertSvc.error('Failed')
    });
  }

  doAddAddress(): void {
    if (!this.addrFullName().trim() || !this.addrLine1().trim()) {
      this.alertSvc.warning('Name and address line 1 required'); return;
    }
    this.actionLoading.set(true);
    const req: AddressRequest = {
      fullName: this.addrFullName(), addressLine1: this.addrLine1(),
      city: this.addrCity(), state: this.addrState(), postalCode: this.addrPostal(),
      country: this.addrCountry(), addressType: this.addrType() as any, isDefault: this.addrDefault()
    };
    this.customerSvc.addAddress(this.customerId(), req).subscribe({
      next: a => {
        this.addresses.update(arr => [...arr, a]);
        this.showAddressModal.set(false); this.actionLoading.set(false);
        this.addrFullName.set(''); this.addrLine1.set(''); this.addrCity.set('');
        this.alertSvc.success('Address added');
      },
      error: () => { this.actionLoading.set(false); this.alertSvc.error('Failed to add address'); }
    });
  }

  deleteAddress(a: CustomerAddress): void {
    this.alertSvc.confirm({ title: 'Delete Address', message: 'Remove this address?', confirmLabel: 'Delete', danger: true })
      .then(ok => {
        if (!ok) return;
        this.customerSvc.deleteAddress(this.customerId(), a.id).subscribe({
          next: () => {
            this.addresses.update(arr => arr.filter(x => x.id !== a.id));
            this.alertSvc.success('Address removed');
          },
          error: () => this.alertSvc.error('Failed')
        });
      });
  }

  setDefaultAddress(a: CustomerAddress): void {
    this.customerSvc.setDefaultAddress(this.customerId(), a.id).subscribe({
      next: updated => {
        this.addresses.update(arr => arr.map(x => ({ ...x, isDefault: x.id === updated.id })));
        this.alertSvc.success('Default address updated');
      },
      error: () => this.alertSvc.error('Failed')
    });
  }

  openTagModal(): void {
    const c = this.customer();
    this.editingTags.set(c ? [...c.tags] : []);
    this.showTagModal.set(true);
  }

  addTag(): void {
    const t = this.newTagInput().trim().toLowerCase();
    if (t && !this.editingTags().includes(t)) {
      this.editingTags.update(tags => [...tags, t]);
      this.newTagInput.set('');
    }
  }

  removeTag(tag: string): void { this.editingTags.update(tags => tags.filter(t => t !== tag)); }

  saveTags(): void {
    this.customerSvc.updateTags(this.customerId(), { tags: this.editingTags() }).subscribe({
      next: c => { this.customer.set(c); this.showTagModal.set(false); this.alertSvc.success('Tags updated'); },
      error: () => this.alertSvc.error('Failed to update tags')
    });
  }

  doDelete(): void {
    const c = this.customer();
    this.alertSvc.confirm({
      title: 'Delete Customer',
      message: `Delete "${c?.fullName}"? This cannot be undone.`,
      confirmLabel: 'Delete', danger: true
    }).then(ok => {
      if (!ok) return;
      this.customerSvc.deleteCustomer(this.customerId()).subscribe({
        next: () => { this.alertSvc.success('Customer deleted'); this.router.navigate(['/customers/all']); },
        error: () => this.alertSvc.error('Delete failed')
      });
    });
  }

  doSetStatus(status: string): void {
    this.customerSvc.setStatus(this.customerId(), status).subscribe({
      next: c => { this.customer.set(c); this.alertSvc.success('Status updated to ' + status); },
      error: () => this.alertSvc.error('Status update failed')
    });
  }

  getStatusConfig(s: string): { badge: string; label: string } {
    const m: Record<string, any> = {
      ACTIVE:               { badge: 'bg-green-100 text-green-800',   label: 'Active' },
      INACTIVE:             { badge: 'bg-gray-100 text-gray-700',     label: 'Inactive' },
      SUSPENDED:            { badge: 'bg-orange-100 text-orange-800', label: 'Suspended' },
      PENDING_VERIFICATION: { badge: 'bg-yellow-100 text-yellow-800', label: 'Pending' },
      BLACKLISTED:          { badge: 'bg-red-100 text-red-800',       label: 'Blacklisted' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', label: s };
  }

  getTierConfig(t: string): { badge: string; icon: string; label: string } {
    const m: Record<string, any> = {
      REGULAR:  { badge: 'bg-gray-100 text-gray-700',     icon: 'person',    label: 'Regular' },
      SILVER:   { badge: 'bg-slate-200 text-slate-700',   icon: 'star',      label: 'Silver' },
      GOLD:     { badge: 'bg-yellow-100 text-yellow-800', icon: 'star-fill', label: 'Gold' },
      PLATINUM: { badge: 'bg-indigo-100 text-indigo-800', icon: 'gem',       label: 'Platinum' },
    };
    return m[t] ?? { badge: 'bg-gray-100 text-gray-700', icon: 'person', label: t };
  }

  getOrderStatusBadge(s: string): string {
    const m: Record<string, string> = {
      DELIVERED: 'bg-green-100 text-green-800',
      SHIPPED:   'bg-purple-100 text-purple-800',
      CONFIRMED: 'bg-blue-100 text-blue-800',
      CANCELLED: 'bg-red-100 text-red-800',
      PROCESSING:'bg-indigo-100 text-indigo-800',
    };
    return m[s] ?? 'bg-gray-100 text-gray-700';
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso));
  }

  formatDateTime(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0 }).format(val ?? 0);
  }

  // ─── AI: Churn + Marketing ────────────────────────────────────────────────

  runChurnPrediction(): void {
    const id = this.customerId();
    if (!id) return;
    this.aiChurnLoading.set(true);
    this.aiError.set(null);
    this.aiSvc.predictChurn(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => { this.churnResult.set(r); this.aiChurnLoading.set(false); },
      error: () => { this.aiError.set('Churn prediction failed'); this.aiChurnLoading.set(false); }
    });
  }

  runMarketing(): void {
    const id = this.customerId();
    if (!id) return;
    this.aiMktLoading.set(true);
    this.aiError.set(null);
    this.aiSvc.personaliseMarketing(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => { this.marketingResult.set(r); this.aiMktLoading.set(false); },
      error: () => { this.aiError.set('Marketing personalisation failed'); this.aiMktLoading.set(false); }
    });
  }

  churnBadge(score: number): string {
    if (score >= 70) return 'bg-red-100 text-red-700';
    if (score >= 40) return 'bg-yellow-100 text-yellow-700';
    return 'bg-green-100 text-green-700';
  }

  toggleAiPanel(){
    this.showAiPanel.update(v => !v);
  }
}
