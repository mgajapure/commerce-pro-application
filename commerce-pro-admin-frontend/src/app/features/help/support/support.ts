import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-contact-support',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './support.html',
  styles: [`:host { display: block; }`]
})
export class ContactSupport {
  private alertSvc = inject(AlertService);

  name = signal('');
  email = signal('');
  subject = signal('');
  category = signal('general');
  priority = signal('medium');
  message = signal('');
  submitting = signal(false);

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Submitting Tickets', content: 'Fill out the form with details about your issue. Choose the appropriate category and priority for faster routing.' },
    { title: 'Priority Levels', content: 'Low: General questions. Medium: Non-urgent issues. High: Business-impacting issues. Critical: System down or data loss.' },
    { title: 'Response Times', content: 'Critical: 1 hour. High: 4 hours. Medium: 24 hours. Low: 48 hours.' }
  ];

  categories = [
    { value: 'bug', label: 'Bug Report', icon: 'bi-bug' },
    { value: 'feature', label: 'Feature Request', icon: 'bi-lightbulb' },
    { value: 'billing', label: 'Billing', icon: 'bi-credit-card' },
    { value: 'general', label: 'General Inquiry', icon: 'bi-chat-dots' },
  ];

  recentTickets = [
    { id: 'TKT-1042', subject: 'Payment gateway timeout issues', status: 'open', date: '2026-04-01' },
    { id: 'TKT-1038', subject: 'Bulk import CSV format question', status: 'resolved', date: '2026-03-28' },
    { id: 'TKT-1035', subject: 'Feature request: Custom reports', status: 'in-progress', date: '2026-03-25' },
  ];

  submit() {
    if (!this.name() || !this.email() || !this.subject() || !this.message()) {
      this.alertSvc.warning('Missing Fields', 'Please fill in all required fields');
      return;
    }
    this.submitting.set(true);
    setTimeout(() => {
      this.submitting.set(false);
      this.alertSvc.success('Ticket Submitted', 'We\'ll get back to you within 24 hours');
      this.name.set(''); this.email.set('');
      this.subject.set(''); this.message.set('');
    }, 1000);
  }

  statusClass(status: string): string {
    switch (status) {
      case 'open': return 'bg-blue-100 text-blue-700';
      case 'resolved': return 'bg-green-100 text-green-700';
      case 'in-progress': return 'bg-yellow-100 text-yellow-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  }

  statusIcon(status: string): string {
    switch (status) {
      case 'open': return 'bi-circle';
      case 'resolved': return 'bi-check-circle-fill';
      case 'in-progress': return 'bi-arrow-repeat';
      default: return 'bi-circle';
    }
  }
}
