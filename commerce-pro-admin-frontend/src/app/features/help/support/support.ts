import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-contact-support',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support.html',
  styles: [`:host { display: block; }`]
})
export class ContactSupport {
  name = signal('');
  email = signal('');
  subject = signal('');
  category = signal('general');
  priority = signal('medium');
  message = signal('');
  submitted = signal(false);
  submitting = signal(false);

  categories = [
    { value: 'bug', label: 'Bug Report' },
    { value: 'feature', label: 'Feature Request' },
    { value: 'billing', label: 'Billing' },
    { value: 'general', label: 'General Inquiry' },
  ];

  recentTickets = [
    { id: 'TKT-1042', subject: 'Payment gateway timeout issues', status: 'open', date: '2026-04-01' },
    { id: 'TKT-1038', subject: 'Bulk import CSV format question', status: 'resolved', date: '2026-03-28' },
    { id: 'TKT-1035', subject: 'Feature request: Custom reports', status: 'in-progress', date: '2026-03-25' },
  ];

  submit() {
    if (!this.name() || !this.email() || !this.subject() || !this.message()) return;
    this.submitting.set(true);
    setTimeout(() => {
      this.submitting.set(false);
      this.submitted.set(true);
      this.name.set('');
      this.email.set('');
      this.subject.set('');
      this.message.set('');
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
}
