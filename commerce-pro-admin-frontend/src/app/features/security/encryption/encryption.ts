import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SecurityService, EncryptionKey } from '../security.service';
import { AlertService } from '../../../shared/services/alert.service';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-encryption-keys',
  standalone: true,
  imports: [CommonModule, FormsModule, Dropdown, HelpSidebar, TooltipLabel],
  templateUrl: './encryption.html',
  styles: [':host { display: block; }']
})
export class EncryptionKeys implements OnInit, OnDestroy {
  private svc = inject(SecurityService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  loading = signal(true);
  keys = signal<EncryptionKey[]>([]);
  rotating = signal(false);

  // Filters
  searchQuery = signal('');
  filterStatus = signal('');

  // Detail sidebar
  showDetail = signal(false);
  detailItem = signal<EncryptionKey | null>(null);

  // Add/Edit modal
  showModal = signal(false);
  formName = signal('');
  formAlgorithm = signal('AES-256-GCM');

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Encryption Keys', content: 'Encryption keys protect sensitive data at rest and in transit. Keys should be rotated periodically for security.' },
    { title: 'Key Rotation', content: 'Rotating a key creates a new key and marks the old one as "rotating". Existing data is re-encrypted with the new key.' },
    { title: 'Algorithms', content: 'AES-256-GCM is recommended for data encryption. RSA-4096 is used for token signing and key wrapping.' },
    { title: 'Expiry', content: 'Keys have an expiry date. Monitor and rotate keys before they expire to avoid service disruption.' }
  ];

  // Pagination
  currentPage = signal(1);
  itemsPerPage = signal(10);

  filtered = computed(() => {
    let list = this.keys();
    const q = this.searchQuery().toLowerCase();
    const s = this.filterStatus();
    if (q) list = list.filter(k => k.name.toLowerCase().includes(q) || k.algorithm.toLowerCase().includes(q));
    if (s) list = list.filter(k => k.status === s);
    return list;
  });

  totalPages = computed(() => Math.ceil(this.filtered().length / this.itemsPerPage()));
  paginatedItems = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.filtered().slice(start, start + this.itemsPerPage());
  });

  activeCount = computed(() => this.keys().filter(k => k.status === 'active').length);
  expiredCount = computed(() => this.keys().filter(k => k.status === 'expired').length);

  ngOnInit() {
    this.svc.getEncryptionKeys().pipe(takeUntil(this.destroy$)).subscribe(keys => {
      if (keys.length === 0) this.keys.set(this.defaultKeys());
      else this.keys.set(keys);
      this.loading.set(false);
    });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  rotateKey(key: EncryptionKey) {
    this.alertSvc.confirm({
      title: 'Rotate Encryption Key',
      message: `Rotate "${key.name}"? A new key will be created and existing data will be re-encrypted.`,
      confirmLabel: 'Rotate',
      danger: false
    }).then(ok => {
      if (!ok) return;
      this.rotating.set(true);
      this.svc.rotateEncryptionKey(key.id).pipe(takeUntil(this.destroy$)).subscribe(newKey => {
        this.rotating.set(false);
        if (newKey) {
          this.keys.update(list => list.map(k => k.id === key.id ? { ...k, status: 'rotating' as const } : k));
          this.keys.update(list => [...list, newKey]);
          this.alertSvc.success('Key Rotated', 'New encryption key is now active');
        } else {
          this.alertSvc.error('Failed', 'Could not rotate encryption key');
        }
      });
    });
  }

  openDetail(key: EncryptionKey) {
    this.detailItem.set(key);
    this.showDetail.set(true);
  }

  openAddKey() {
    this.formName.set('');
    this.formAlgorithm.set('AES-256-GCM');
    this.showModal.set(true);
  }

  addKey() {
    if (!this.formName().trim()) {
      this.alertSvc.warning('Validation', 'Key name is required');
      return;
    }
    const newKey: EncryptionKey = {
      id: 'key-' + Math.random().toString(36).slice(2, 7),
      name: this.formName(),
      algorithm: this.formAlgorithm(),
      status: 'active',
      createdDate: new Date().toISOString().split('T')[0],
      expiryDate: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
    };
    this.keys.update(list => [...list, newKey]);
    this.showModal.set(false);
    this.alertSvc.success('Key Created', `"${newKey.name}" has been added`);
  }

  statusClass(status: string): string {
    switch (status) {
      case 'active': return 'bg-green-100 text-green-800';
      case 'expired': return 'bg-red-100 text-red-800';
      case 'rotating': return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  statusDot(status: string): string {
    switch (status) {
      case 'active': return 'bg-green-500';
      case 'expired': return 'bg-red-500';
      case 'rotating': return 'bg-yellow-500';
      default: return 'bg-gray-400';
    }
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso));
  }

  getRowActions(key: EncryptionKey): DropdownItem[] {
    const items: DropdownItem[] = [
      { id: 'view', label: 'View Details', icon: 'eye' }
    ];
    if (key.status === 'active') {
      items.push({ id: 'divider1', label: '', divider: true });
      items.push({ id: 'rotate', label: 'Rotate Key', icon: 'arrow-repeat' });
    }
    return items;
  }

  onRowAction(action: DropdownItem, key: EncryptionKey) {
    switch (action.id) {
      case 'view': this.openDetail(key); break;
      case 'rotate': this.rotateKey(key); break;
    }
  }

  goToPage(p: number) { this.currentPage.set(p); }
  previousPage() { if (this.currentPage() > 1) this.currentPage.update(p => p - 1); }
  nextPage() { if (this.currentPage() < this.totalPages()) this.currentPage.update(p => p + 1); }

  private defaultKeys(): EncryptionKey[] {
    return [
      { id: 'key-001', name: 'Data Encryption Key', algorithm: 'AES-256-GCM', status: 'active', createdDate: '2025-01-15', expiryDate: '2026-01-15' },
      { id: 'key-002', name: 'Token Signing Key', algorithm: 'RSA-4096', status: 'active', createdDate: '2025-03-01', expiryDate: '2026-03-01' },
      { id: 'key-003', name: 'Backup Encryption Key', algorithm: 'AES-256-GCM', status: 'active', createdDate: '2025-06-01', expiryDate: '2026-06-01' },
    ];
  }
}
