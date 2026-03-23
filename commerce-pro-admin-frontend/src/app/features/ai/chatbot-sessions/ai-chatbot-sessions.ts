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

  sessionId      = signal('');
  session        = signal<AiChatSession | null>(null);
  isLoading      = signal(false);
  error          = signal<string | null>(null);
  recentSessions = signal<string[]>([]);

  ngOnInit(): void {}
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

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
      error: () => {
        this.error.set('Session not found or access denied');
        this.isLoading.set(false);
      }
    });
  }

  loadRecent(id: string): void {
    this.sessionId.set(id);
    this.lookup();
  }

  formatDateTime(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    }).format(new Date(iso));
  }

  formatTime(iso: string): string {
    if (!iso) return '';
    return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }
}
