import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { AuditLogEntry } from '../../../core/models/identity';
import { IdentityService } from '../../../core/services/identity/identity.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';

@Component({
  selector: 'app-identity-audit',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar],
  templateUrl: './audit.html',
  styles: [`:host { display: block; }`]
})
export class IdentityAudit implements OnInit, OnDestroy {
  private readonly svc = inject(IdentityService);
  private readonly alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  logs = signal<AuditLogEntry[]>([]);
  loading = signal(true);
  totalElements = signal(0);
  currentPage = signal(0);
  totalPages = signal(0);
  pageSize = 25;

  searchQuery = signal('');
  actionFilter = signal('');
  successFilter = signal<'' | 'true' | 'false'>('');
  dateFrom = signal('');
  dateTo = signal('');
  showFilters = signal(true);

  // Detail sidebar
  showDetail = signal(false);
  selectedLog = signal<AuditLogEntry | null>(null);

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Audit Logs', content: 'All identity-related activities are logged here for compliance and security monitoring.' },
    { title: 'Filters', content: 'Search by actor, target, or description. Filter by action type, outcome (success/failure), and date range.' },
    { title: 'Log Details', content: 'Click on any log entry to view full details including IP address, timestamps, and descriptions.' }
  ];

  ngOnInit() { this.loadLogs(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  loadLogs(page = 0) {
    this.loading.set(true);
    this.currentPage.set(page);
    const successOnly = this.successFilter() === '' ? null : this.successFilter() === 'true';

    this.svc.getAuditLogs(page, this.pageSize, this.searchQuery(), this.actionFilter(), successOnly)
      .pipe(takeUntil(this.destroy$)).subscribe(result => {
        this.logs.set(result.content);
        this.totalElements.set(result.totalElements);
        this.totalPages.set(result.totalPages);
        this.loading.set(false);
      });
  }

  reload() { this.loadLogs(this.currentPage()); }

  applyFilters() { this.loadLogs(0); }

  clearFilters() {
    this.searchQuery.set('');
    this.actionFilter.set('');
    this.successFilter.set('');
    this.dateFrom.set('');
    this.dateTo.set('');
    this.loadLogs(0);
  }

  viewDetail(log: AuditLogEntry) {
    this.selectedLog.set(log);
    this.showDetail.set(true);
  }

  goToPage(page: number) { if (page >= 0 && page < this.totalPages()) this.loadLogs(page); }
}
