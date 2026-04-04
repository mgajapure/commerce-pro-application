import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SecurityService, PciComplianceItem } from '../security.service';

@Component({
  selector: 'app-pci-compliance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pci.html',
  styles: [':host { display: block; }']
})
export class PciCompliance implements OnInit {
  private svc = inject(SecurityService);

  loading = signal(true);
  saving = signal(false);
  success = signal('');
  error = signal('');
  items = signal<PciComplianceItem[]>([]);

  compliantCount = computed(() => this.items().filter(i => i.status === 'compliant').length);

  complianceRate = computed(() => {
    const all = this.items();
    if (all.length === 0) return 0;
    return Math.round((this.compliantCount() / all.length) * 100);
  });

  ngOnInit() {
    this.svc.getPciStatus().subscribe(items => {
      if (items.length === 0) {
        this.items.set(this.defaultItems());
      } else {
        this.items.set(items);
      }
      this.loading.set(false);
    });
  }

  updateItemStatus(id: string, status: string) {
    this.items.update(list => list.map(i => i.id === id ? { ...i, status: status as any } : i));
  }

  updateItemNotes(id: string, notes: string) {
    this.items.update(list => list.map(i => i.id === id ? { ...i, notes } : i));
  }

  save() {
    this.saving.set(true);
    this.success.set('');
    this.error.set('');
    this.svc.updatePciStatus(this.items()).subscribe(res => {
      this.saving.set(false);
      if (res) this.success.set('PCI compliance status saved.');
      else this.error.set('Failed to save PCI compliance status.');
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
