import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OutsideClickDirective } from '../../../../directives/outside-click.directive';
import { Message } from '../../../../../core/models/message.model';

@Component({
  selector: 'app-inbox',
  standalone: true,
  imports: [CommonModule, OutsideClickDirective],
  templateUrl: './inbox.html',
  styleUrl: './inbox.scss'
})
export class Inbox {
  isOpen = signal(false);

  // Messages are a separate concept (not backed by notification API yet)
  messages = signal<Message[]>([
    {
      id: '1',
      sender: 'Sarah Johnson',
      senderAvatar: 'https://i.pravatar.cc/150?img=32',
      subject: 'Order Inquiry',
      preview: 'Regarding my order #12342, when will it arrive?',
      timestamp: new Date(Date.now() - 3600000),
      type: 'customer',
      read: false
    },
    {
      id: '2',
      sender: 'Mike Chen',
      senderAvatar: 'https://i.pravatar.cc/150?img=12',
      subject: 'Bulk Order',
      preview: 'Can you help me with a bulk order inquiry?',
      timestamp: new Date(Date.now() - 86400000),
      type: 'supplier',
      read: true
    }
  ]);

  unreadCount = signal(1);

  toggle() {
    this.isOpen.update(v => !v);
  }

  markAllRead() {
    this.messages.update(msgs => msgs.map(m => ({ ...m, read: true })));
    this.unreadCount.set(0);
  }
}
