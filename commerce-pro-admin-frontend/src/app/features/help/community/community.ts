import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';

@Component({
  selector: 'app-community',
  standalone: true,
  imports: [CommonModule, HelpSidebar],
  templateUrl: './community.html',
  styles: [`:host { display: block; }`]
})
export class Community {
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Community Links', content: 'Connect with other Commerce Pro users and developers across various platforms.' },
    { title: 'Discussions', content: 'Browse and participate in featured community discussions about best practices.' },
    { title: 'Release Notes', content: 'Stay up to date with the latest features and improvements in each release.' }
  ];

  links = [
    { name: 'GitHub Repository', description: 'Browse source code, report issues, and contribute', icon: 'bi-github', color: 'bg-gray-900', url: '#' },
    { name: 'Discord Community', description: 'Chat with other developers and get real-time help', icon: 'bi-discord', color: 'bg-indigo-600', url: '#' },
    { name: 'Stack Overflow', description: 'Find answers tagged with commerce-pro', icon: 'bi-stack-overflow', color: 'bg-orange-500', url: '#' },
    { name: 'Community Forum', description: 'Discuss features, share tips, and get advice', icon: 'bi-chat-square-dots', color: 'bg-green-600', url: '#' },
  ];

  discussions = [
    { title: 'Best practices for multi-warehouse inventory management', author: 'warehouse_pro', replies: 24, date: '2026-04-02' },
    { title: 'How to customize the checkout flow with webhooks', author: 'dev_sarah', replies: 18, date: '2026-04-01' },
    { title: 'Performance optimization for large product catalogs', author: 'speed_demon', replies: 31, date: '2026-03-30' },
    { title: 'Integrating Commerce Pro with external ERP systems', author: 'erp_expert', replies: 15, date: '2026-03-28' },
  ];

  changelog = [
    { version: 'v3.2.0', date: '2026-04-01', changes: ['Added AI-powered product recommendations', 'New financial reporting dashboard', 'Improved webhook management UI'] },
    { version: 'v3.1.0', date: '2026-03-15', changes: ['Multi-warehouse support', 'Bulk order processing', 'Enhanced security settings'] },
    { version: 'v3.0.0', date: '2026-03-01', changes: ['Complete UI redesign', 'New analytics engine', 'Role-based access control overhaul'] },
  ];
}
