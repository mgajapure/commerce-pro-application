import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuditLogEntry } from '../../../core/models/identity';
import { IdentityService } from '../../../core/services/identity/identity.service';

@Component({
  selector: 'app-identity-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit.html',
  styleUrl: './audit.scss'
})
export class IdentityAudit implements OnInit {
  private readonly identityService = inject(IdentityService);

  readonly logs = signal<AuditLogEntry[]>([]);
  readonly isLoading = signal(false);
  readonly totalElements = signal(0);
  readonly currentPage = signal(0);
  readonly pageSize = 25;

  readonly searchQuery = signal('');
  readonly actionFilter = signal('');
  readonly successFilter = signal<'' | 'true' | 'false'>('');

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(page = 0): void {
    this.isLoading.set(true);
    this.currentPage.set(page);

    const successOnly = this.successFilter() === '' ? null : this.successFilter() === 'true';

    this.identityService
      .getAuditLogs(page, this.pageSize, this.searchQuery(), this.actionFilter(), successOnly)
      .subscribe(result => {
        this.logs.set(result.content);
        this.totalElements.set(result.totalElements);
        this.isLoading.set(false);
      });
  }

  applyFilters(): void {
    this.loadLogs(0);
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.actionFilter.set('');
    this.successFilter.set('');
    this.loadLogs(0);
  }

  nextPage(): void {
    this.loadLogs(this.currentPage() + 1);
  }

  prevPage(): void {
    if (this.currentPage() > 0) this.loadLogs(this.currentPage() - 1);
  }

  get hasMore(): boolean {
    return (this.currentPage() + 1) * this.pageSize < this.totalElements();
  }
}
