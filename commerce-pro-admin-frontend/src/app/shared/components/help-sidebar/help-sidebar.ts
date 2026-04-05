import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface HelpSection {
  title: string;
  content: string;
}

@Component({
  selector: 'app-help-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './help-sidebar.html'
})
export class HelpSidebar {
  @Input() title = 'Help';
  @Input() subtitle = '';
  @Input() sections: HelpSection[] = [];
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }
}
