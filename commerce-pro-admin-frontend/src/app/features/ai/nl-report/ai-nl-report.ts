import { Component, signal, OnInit, OnDestroy, inject, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiNlReportRequest } from '../../../core/models/ai/ai.model';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

@Component({
  selector: 'app-ai-nl-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-nl-report.html',
  styleUrl: './ai-nl-report.scss'
})
export class AiNlReport implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesEnd') private messagesEnd!: ElementRef;

  private aiSvc    = inject(AiService);
  private destroy$ = new Subject<void>();

  messages  = signal<ChatMessage[]>([]);
  sessionId = signal<string | null>(null);
  input     = signal('');
  rawData   = signal('');
  isLoading = signal(false);
  showData  = signal(false);
  error     = signal<string | null>(null);

  private shouldScroll = false;

  ngOnInit(): void {
    this.messages.set([{
      role: 'assistant',
      content: 'Hi! I\'m your AI Business Analyst. Ask me anything about your business data — revenue trends, customer behaviour, product performance — or paste raw data for me to analyse.',
      timestamp: new Date()
    }]);
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  send(): void {
    const msg = this.input().trim();
    if (!msg || this.isLoading()) return;

    this.messages.update(m => [...m, { role: 'user', content: msg, timestamp: new Date() }]);
    this.input.set('');
    this.isLoading.set(true);
    this.error.set(null);
    this.shouldScroll = true;

    const req: AiNlReportRequest = {
      message: msg,
      sessionId: this.sessionId() ?? undefined,
      rawData: this.rawData() || undefined
    };

    this.aiSvc.nlReportChat(req).pipe(takeUntil(this.destroy$)).subscribe({
      next: res => {
        this.sessionId.set(res.sessionId);
        this.messages.update(m => [...m, { role: 'assistant', content: res.reply, timestamp: new Date() }]);
        this.isLoading.set(false);
        this.shouldScroll = true;
      },
      error: () => {
        this.error.set('Failed to get a response. Please try again.');
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
    this.rawData.set('');
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

  private scrollToBottom(): void {
    try { this.messagesEnd?.nativeElement?.scrollIntoView({ behavior: 'smooth' }); } catch {}
  }
}
