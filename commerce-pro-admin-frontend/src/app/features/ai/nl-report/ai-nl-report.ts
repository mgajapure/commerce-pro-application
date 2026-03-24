import { Component, signal, OnInit, OnDestroy, inject, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AiService } from '../../../core/services/ai/ai.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AlertComponent } from '../../../shared/components/alert/alert';
import { AiNlReportRequest, AiNlSessionSummary } from '../../../core/models/ai/ai.model';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

@Component({
  selector: 'app-ai-nl-report',
  standalone: true,
  imports: [CommonModule, FormsModule, AlertComponent],
  templateUrl: './ai-nl-report.html',
  styleUrl: './ai-nl-report.scss'
})
export class AiNlReport implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesEnd') private messagesEnd!: ElementRef;

  private aiSvc    = inject(AiService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  messages     = signal<ChatMessage[]>([]);
  sessionId    = signal<string | null>(null);
  sessions     = signal<AiNlSessionSummary[]>([]);
  isLoading    = signal(false);
  sessionsLoading = signal(false);
  showData     = signal(false);
  error        = signal<string | null>(null);

  // Plain properties for two-way ngModel binding (signals are not ngModel-compatible)
  inputValue  = '';
  rawData     = '';

  private shouldScroll = false;

  ngOnInit(): void {
    this.messages.set([{
      role: 'assistant',
      content: 'Hi! I\'m your AI Business Analyst. Ask me anything about your business data — revenue trends, customer behaviour, product performance — or paste raw data for me to analyse.',
      timestamp: new Date()
    }]);
    this.loadSessions();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  loadSessions(): void {
    this.sessionsLoading.set(true);
    this.aiSvc.getNlReportSessions().pipe(takeUntil(this.destroy$)).subscribe({
      next: s => { this.sessions.set(s); this.sessionsLoading.set(false); },
      error: () => this.sessionsLoading.set(false)
    });
  }

  loadSession(session: AiNlSessionSummary): void {
    this.sessionsLoading.set(true);
    this.aiSvc.getNlReportSession(session.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: detail => {
        this.sessionId.set(detail.id);
        const msgs: ChatMessage[] = [];
        detail.turns.forEach(t => {
          msgs.push({ role: 'user',      content: t.userMessage,      timestamp: new Date(detail.createdAt) });
          msgs.push({ role: 'assistant', content: t.assistantMessage, timestamp: new Date(detail.createdAt) });
        });
        this.messages.set(msgs);
        this.sessionsLoading.set(false);
        this.shouldScroll = true;
        this.alertSvc.info('Session loaded', session.preview);
      },
      error: () => {
        this.sessionsLoading.set(false);
        this.alertSvc.error('Failed to load session');
      }
    });
  }

  send(): void {
    const msg = this.inputValue.trim();
    if (!msg || this.isLoading()) return;

    this.messages.update(m => [...m, { role: 'user', content: msg, timestamp: new Date() }]);
    this.inputValue = '';
    this.isLoading.set(true);
    this.error.set(null);
    this.shouldScroll = true;

    const req: AiNlReportRequest = {
      message: msg,
      sessionId: this.sessionId() ?? undefined,
      rawData: this.rawData || undefined
    };

    this.aiSvc.nlReportChat(req).pipe(takeUntil(this.destroy$)).subscribe({
      next: res => {
        this.sessionId.set(res.sessionId);
        this.messages.update(m => [...m, {
          role: 'assistant',
          content: res.message,   // field is 'message' in the Java DTO
          timestamp: new Date()
        }]);
        this.isLoading.set(false);
        this.shouldScroll = true;
        this.loadSessions(); // refresh sidebar after new turn
      },
      error: () => {
        this.error.set('Failed to get a response. Please try again.');
        this.alertSvc.error('AI response failed', 'Could not reach the business analyst. Please try again.');
        this.isLoading.set(false);
      }
    });
  }

  onKeyDown(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); this.send(); }
  }

  toggleShowData(): void { this.showData.set(!this.showData()); }

  newSession(): void {
    this.sessionId.set(null);
    this.rawData = '';
    this.error.set(null);
    this.messages.set([{
      role: 'assistant',
      content: 'New session started. How can I help you analyse your business data?',
      timestamp: new Date()
    }]);
  }

  formatTime(d: Date): string {
    return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit' }).format(d);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: '2-digit' });
  }

  private scrollToBottom(): void {
    try { this.messagesEnd?.nativeElement?.scrollIntoView({ behavior: 'smooth' }); } catch {}
  }
}
