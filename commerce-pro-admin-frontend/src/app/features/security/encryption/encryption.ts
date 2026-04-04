import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SecurityService, EncryptionKey } from '../security.service';

@Component({
  selector: 'app-encryption-keys',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './encryption.html',
  styles: [':host { display: block; }']
})
export class EncryptionKeys implements OnInit {
  private svc = inject(SecurityService);

  loading = signal(true);
  success = signal('');
  error = signal('');
  keys = signal<EncryptionKey[]>([]);
  confirmRotateId = signal<string | null>(null);
  rotating = signal(false);

  ngOnInit() {
    this.svc.getEncryptionKeys().subscribe(keys => {
      if (keys.length === 0) {
        this.keys.set(this.defaultKeys());
      } else {
        this.keys.set(keys);
      }
      this.loading.set(false);
    });
  }

  promptRotate(keyId: string) {
    this.confirmRotateId.set(keyId);
  }

  cancelRotate() {
    this.confirmRotateId.set(null);
  }

  confirmRotate(keyId: string) {
    this.rotating.set(true);
    this.success.set('');
    this.error.set('');
    this.svc.rotateEncryptionKey(keyId).subscribe(newKey => {
      this.rotating.set(false);
      this.confirmRotateId.set(null);
      if (newKey) {
        this.keys.update(list => list.map(k => k.id === keyId ? { ...k, status: 'rotating' as const } : k));
        this.keys.update(list => [...list, newKey]);
        this.success.set('Encryption key rotated successfully. New key is now active.');
      } else {
        this.error.set('Failed to rotate encryption key.');
      }
    });
  }

  statusClass(status: string): string {
    switch (status) {
      case 'active': return 'bg-green-100 text-green-800';
      case 'expired': return 'bg-red-100 text-red-800';
      case 'rotating': return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  private defaultKeys(): EncryptionKey[] {
    return [
      { id: 'key-001', name: 'Data Encryption Key', algorithm: 'AES-256-GCM', status: 'active', createdDate: '2025-01-15', expiryDate: '2026-01-15' },
      { id: 'key-002', name: 'Token Signing Key', algorithm: 'RSA-4096', status: 'active', createdDate: '2025-03-01', expiryDate: '2026-03-01' },
      { id: 'key-003', name: 'Backup Encryption Key', algorithm: 'AES-256-GCM', status: 'active', createdDate: '2025-06-01', expiryDate: '2026-06-01' },
    ];
  }
}
