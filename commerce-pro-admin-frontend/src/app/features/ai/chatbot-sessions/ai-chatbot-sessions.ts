import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiChatSession } from '../../../core/models/ai/ai.model';

@Component({
  selector: 'app-ai-chatbot-sessions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chatbot-sessions.html',
  styleUrl: './ai-chatbot-sessions.scss'
})
export class AiChatbotSessions implements OnInit, OnDestroy {
  private aiSvc    = inject(AiService);
  private destroy$ = new Subject<void>();

  // Session list
  sessions       = signal<AiChatSession[]>([]);
  totalSessions  = signal(0);
  totalPages     = signal(0);
  currentPage    = signal(1);
  listLoading    = signal(false);
  searchQuery    = signal('');

  // Single session detail
  sessionId      = signal('');
  session        = signal<AiChatSession | null>(null);
  isLoading      = signal(false);
  error          = signal<string | null>(null);
  recentSessions = signal<string[]>([]);

  ngOnInit(): void { this.loadSessions(1); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  loadSessions(page: number): void {
    this.currentPage.set(page);
    this.listLoading.set(true);
    this.session.set(null);
    this.error.set(null);
    this.aiSvc.getChatbotSessions(page - 1, 20, this.searchQuery() || undefined)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: r => {
          this.sessions.set(r.content);
          this.totalSessions.set(r.totalElements);
          this.totalPages.set(r.totalPages);
          this.listLoading.set(false);
        },
        error: () => this.listLoading.set(false)
      });
  }

  onSearch(): void { this.loadSessions(1); }
  selectSession(s: AiChatSession): void { this.session.set(s); }
  clearDetail(): void { this.session.set(null); this.error.set(null); }

  lookup(): void {
    const id = this.sessionId().trim();
    if (!id) return;
    this.isLoading.set(true);
    this.error.set(null);
    this.session.set(null);
    this.aiSvc.getChatbotSession(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: s => {
        this.session.set(s);
        this.isLoading.set(false);
        this.recentSessions.update(rs => [id, ...rs.filter(r => r !== id)].slice(0, 8));
      },
      error: () => { this.error.set('Session not found or access denied'); this.isLoading.set(false); }
    });
  }

  loadRecent(id: string): void { this.sessionId.set(id); this.lookup(); }
  prevPage(): void { if (this.currentPage() > 1) this.loadSessions(this.currentPage() - 1); }
  nextPage(): void { if (this.currentPage() < this.totalPages()) this.loadSessions(this.currentPage() + 1); }

  formatDateTime(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  formatTime(iso: string): string {
    if (!iso) return '';
    return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }
}
