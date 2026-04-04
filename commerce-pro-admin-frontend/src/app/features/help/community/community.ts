import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-community',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './community.html',
  styles: [`:host { display: block; }`]
})
export class Community {
  links = [
    { name: 'GitHub Repository', description: 'Browse source code, report issues, and contribute', icon: 'github', url: '#' },
    { name: 'Discord Community', description: 'Chat with other developers and get real-time help', icon: 'chat', url: '#' },
    { name: 'Stack Overflow', description: 'Find answers tagged with commerce-pro', icon: 'stack', url: '#' },
    { name: 'Community Forum', description: 'Discuss features, share tips, and get advice', icon: 'forum', url: '#' },
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
