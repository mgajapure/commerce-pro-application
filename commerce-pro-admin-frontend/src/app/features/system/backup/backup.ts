import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SystemService, BackupStatus, BackupEntry, BackupSchedule } from '../system.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-backup-restore',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './backup.html',
  styles: [`:host { display: block; }`]
})
export class BackupRestore implements OnInit, OnDestroy {
  private svc = inject(SystemService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  status = signal<BackupStatus>({ lastBackupAt: null, lastBackupSize: '0 MB', status: 'idle', progress: 0 });
  history = signal<BackupEntry[]>([]);
  schedule = signal<BackupSchedule>({ frequency: 'daily', time: '02:00', retentionCount: 30 });
  isLoading = signal(true);
  backing = signal(false);
  restoreConfirm = signal('');
  restoring = signal(false);
  savingSchedule = signal(false);

  // Detail sidebar
  showDetail = signal(false);
  detailItem = signal<BackupEntry | null>(null);

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Automatic Backups', content: 'Configure the schedule to automatically back up your data at regular intervals. Backups run during off-peak hours.' },
    { title: 'Manual Backups', content: 'Click "Backup Now" to create an immediate backup. This is recommended before major changes.' },
    { title: 'Restore', content: 'Restoring from a backup replaces all current data. Type RESTORE to confirm this destructive action.' },
    { title: 'Retention', content: 'Set how many backups to keep. Older backups beyond the retention count are automatically deleted.' }
  ];

  // Pagination
  currentPage = signal(1);
  itemsPerPage = signal(10);
  totalPages = computed(() => Math.ceil(this.history().length / this.itemsPerPage()));
  paginatedHistory = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.history().slice(start, start + this.itemsPerPage());
  });

  statusConfig = computed(() => {
    const s = this.status();
    const configs: Record<string, { color: string; icon: string; label: string }> = {
      idle: { color: 'bg-green-100 text-green-800', icon: 'bi-check-circle', label: 'Idle' },
      completed: { color: 'bg-green-100 text-green-800', icon: 'bi-check-circle-fill', label: 'Completed' },
      running: { color: 'bg-blue-100 text-blue-800', icon: 'bi-arrow-repeat', label: 'Running' },
      failed: { color: 'bg-red-100 text-red-800', icon: 'bi-x-circle', label: 'Failed' }
    };
    return configs[s.status] || configs['idle'];
  });

  ngOnInit() { this.load(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  load() {
    this.isLoading.set(true);
    this.svc.getBackupStatus().pipe(takeUntil(this.destroy$)).subscribe(s => {
      this.status.set(s);
      this.isLoading.set(false);
    });
    this.svc.getBackupHistory().pipe(takeUntil(this.destroy$)).subscribe(h => {
      if (h.length === 0) h = this.defaultHistory();
      this.history.set(h);
    });
    this.svc.getBackupSchedule().pipe(takeUntil(this.destroy$)).subscribe(s => this.schedule.set(s));
  }

  triggerBackup() {
    this.backing.set(true);
    this.svc.triggerBackup().pipe(takeUntil(this.destroy$)).subscribe(ok => {
      this.backing.set(false);
      if (ok) {
        this.alertSvc.success('Backup Started', 'Backup has been initiated successfully');
        this.status.update(s => ({ ...s, status: 'running' }));
      } else {
        this.alertSvc.error('Failed', 'Could not initiate backup');
      }
    });
  }

  triggerRestore() {
    if (this.restoreConfirm() !== 'RESTORE') {
      this.alertSvc.warning('Confirmation Required', 'Type RESTORE to confirm');
      return;
    }

    this.alertSvc.confirm({
      title: 'Confirm Restore',
      message: 'This will replace ALL current data with the backup. This action CANNOT be undone. Are you absolutely sure?',
      confirmLabel: 'Restore Now',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.restoring.set(true);
      this.svc.triggerRestore(this.restoreConfirm()).pipe(takeUntil(this.destroy$)).subscribe(success => {
        this.restoring.set(false);
        this.restoreConfirm.set('');
        if (success) this.alertSvc.success('Restore Started', 'System restore has been initiated');
        else this.alertSvc.error('Failed', 'Could not initiate restore');
      });
    });
  }

  saveSchedule() {
    this.savingSchedule.set(true);
    this.svc.updateBackupSchedule(this.schedule()).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.savingSchedule.set(false);
      if (res) this.alertSvc.success('Schedule Saved', 'Backup schedule updated successfully');
      else this.alertSvc.error('Failed', 'Could not save schedule');
    });
  }

  updateSchedule(field: keyof BackupSchedule, value: any) {
    this.schedule.update(s => ({ ...s, [field]: value }));
  }

  openDetail(entry: BackupEntry) {
    this.detailItem.set(entry);
    this.showDetail.set(true);
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  goToPage(p: number) { this.currentPage.set(p); }
  previousPage() { if (this.currentPage() > 1) this.currentPage.update(p => p - 1); }
  nextPage() { if (this.currentPage() < this.totalPages()) this.currentPage.update(p => p + 1); }

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
