import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SecurityService, PciComplianceItem } from '../security.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';

@Component({
  selector: 'app-pci-compliance',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar],
  templateUrl: './pci.html',
  styles: [':host { display: block; }']
})
export class PciCompliance implements OnInit, OnDestroy {
  private svc = inject(SecurityService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  loading = signal(true);
  saving = signal(false);
  items = signal<PciComplianceItem[]>([]);

  // Detail sidebar
  showDetail = signal(false);
  detailItem = signal<PciComplianceItem | null>(null);

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'PCI DSS', content: 'The Payment Card Industry Data Security Standard (PCI DSS) is a set of requirements to ensure secure handling of credit card information.' },
    { title: 'Compliance Status', content: 'Track each of the 12 PCI DSS requirements. Update status as your organization achieves compliance.' },
    { title: 'Audits', content: 'Regular audits verify compliance. Keep audit dates current and add notes about remediation progress.' }
  ];

  // Pagination
  currentPage = signal(1);
  itemsPerPage = signal(12);

  compliantCount = computed(() => this.items().filter(i => i.status === 'compliant').length);
  inProgressCount = computed(() => this.items().filter(i => i.status === 'in-progress').length);
  nonCompliantCount = computed(() => this.items().filter(i => i.status === 'non-compliant').length);

  complianceRate = computed(() => {
    const all = this.items();
    if (all.length === 0) return 0;
    return Math.round((this.compliantCount() / all.length) * 100);
  });

  totalPages = computed(() => Math.ceil(this.items().length / this.itemsPerPage()));
  paginatedItems = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.items().slice(start, start + this.itemsPerPage());
  });

  ngOnInit() {
    this.svc.getPciStatus().pipe(takeUntil(this.destroy$)).subscribe(items => {
      if (items.length === 0) this.items.set(this.defaultItems());
      else this.items.set(items);
      this.loading.set(false);
    });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  updateItemStatus(id: string, status: string) {
    this.items.update(list => list.map(i => i.id === id ? { ...i, status: status as any } : i));
  }

  updateItemNotes(id: string, notes: string) {
    this.items.update(list => list.map(i => i.id === id ? { ...i, notes } : i));
  }

  openDetail(item: PciComplianceItem) {
    this.detailItem.set(item);
    this.showDetail.set(true);
  }

  save() {
    this.saving.set(true);
    this.svc.updatePciStatus(this.items()).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.saving.set(false);
      if (res) this.alertSvc.success('PCI Status Saved');
      else this.alertSvc.error('Failed', 'Could not save PCI compliance status');
    });
  }

  statusClass(status: string): string {
    switch (status) {
      case 'compliant': return 'bg-green-100 text-green-800';
      case 'non-compliant': return 'bg-red-100 text-red-800';
      case 'in-progress': return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  statusDot(status: string): string {
    switch (status) {
      case 'compliant': return 'bg-green-500';
      case 'non-compliant': return 'bg-red-500';
      case 'in-progress': return 'bg-yellow-500';
      default: return 'bg-gray-400';
    }
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso));
  }

  goToPage(p: number) { this.currentPage.set(p); }
  previousPage() { if (this.currentPage() > 1) this.currentPage.update(p => p - 1); }
  nextPage() { if (this.currentPage() < this.totalPages()) this.currentPage.update(p => p + 1); }

  private defaultItems(): PciComplianceItem[] {
    return [
      { id: 'pci-1', name: 'Install and maintain a firewall configuration', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
      { id: 'pci-2', name: 'Do not use vendor-supplied defaults for passwords', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
      { id: 'pci-3', name: 'Protect stored cardholder data', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
      { id: 'pci-4', name: 'Encrypt transmission of cardholder data', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
      { id: 'pci-5', name: 'Use and regularly update anti-virus software', status: 'in-progress', lastAuditDate: '2026-03-10', notes: 'Update scheduled' },
      { id: 'pci-6', name: 'Develop and maintain secure systems and applications', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
      { id: 'pci-7', name: 'Restrict access to cardholder data by business need', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
      { id: 'pci-8', name: 'Assign a unique ID to each person with computer access', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
      { id: 'pci-9', name: 'Restrict physical access to cardholder data', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
      { id: 'pci-10', name: 'Track and monitor all access to network resources', status: 'in-progress', lastAuditDate: '2026-03-10', notes: 'Enhanced logging being added' },
      { id: 'pci-11', name: 'Regularly test security systems and processes', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
      { id: 'pci-12', name: 'Maintain a policy that addresses information security', status: 'compliant', lastAuditDate: '2026-03-15', notes: '' },
    ];
  }
}
