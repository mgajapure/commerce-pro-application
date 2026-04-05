import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';

@Component({
  selector: 'app-documentation',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HelpSidebar],
  templateUrl: './documentation.html',
  styles: [`:host { display: block; }`]
})
export class Documentation {
  searchQuery = signal('');
  expandedSection = signal<string | null>('getting-started');

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Using Documentation', content: 'Browse through categories or use the search bar to find specific topics. Click on any article to navigate to that feature.' },
    { title: 'Search Tips', content: 'Search by keyword, feature name, or topic. Results update in real-time as you type.' }
  ];

  sections = [
    {
      id: 'getting-started', title: 'Getting Started', icon: 'bi-rocket-takeoff',
      items: [
        { title: 'Platform Overview', description: 'Introduction to Commerce Pro and its core features', link: '/dashboard' },
        { title: 'Initial Setup', description: 'Configure your store, payment gateways, and shipping', link: '/settings/general' },
        { title: 'User Roles & Permissions', description: 'Set up team access with role-based permissions', link: '/identity/users' },
      ]
    },
    {
      id: 'catalog', title: 'Products & Catalog', icon: 'bi-box-seam',
      items: [
        { title: 'Managing Products', description: 'Create, edit, and organize your product catalog', link: '/catalog/products' },
        { title: 'Categories & Attributes', description: 'Organize products with categories and custom attributes', link: '/catalog/categories' },
        { title: 'Bulk Operations', description: 'Import, export, and bulk edit products', link: '/catalog/bulk-operations' },
      ]
    },
    {
      id: 'orders', title: 'Orders & Fulfillment', icon: 'bi-bag-check',
      items: [
        { title: 'Processing Orders', description: 'Manage the complete order lifecycle', link: '/orders/all' },
        { title: 'Returns & Refunds', description: 'Handle return requests and process refunds', link: '/orders/returns' },
        { title: 'Fulfillment', description: 'Pick, pack, and ship orders efficiently', link: '/fulfillment/queue' },
      ]
    },
    {
      id: 'inventory', title: 'Inventory Management', icon: 'bi-boxes',
      items: [
        { title: 'Stock Overview', description: 'Monitor stock levels across warehouses', link: '/inventory/overview' },
        { title: 'Warehouses', description: 'Manage multiple warehouse locations', link: '/inventory/warehouses' },
        { title: 'Transfers & Adjustments', description: 'Move stock between locations', link: '/inventory/transfers' },
      ]
    },
    {
      id: 'customers', title: 'Customers', icon: 'bi-people',
      items: [
        { title: 'Customer Management', description: 'View and manage customer accounts', link: '/customers/all' },
        { title: 'Customer Groups', description: 'Segment customers for targeted pricing and promotions', link: '/customers/groups' },
      ]
    },
    {
      id: 'payments', title: 'Payments & Finance', icon: 'bi-credit-card',
      items: [
        { title: 'Payment Gateways', description: 'Configure payment providers and methods', link: '/payments/gateways' },
        { title: 'Financial Reports', description: 'P&L statements, invoicing, and tax management', link: '/finance/overview' },
      ]
    },
    {
      id: 'settings', title: 'Settings & Security', icon: 'bi-gear',
      items: [
        { title: 'General Settings', description: 'Site name, timezone, and localization', link: '/settings/general' },
        { title: 'Security Settings', description: 'Password policies, MFA, and access control', link: '/security/settings' },
        { title: 'API Management', description: 'Create and manage API keys', link: '/system/api' },
      ]
    },
  ];

  toggleSection(id: string) {
    this.expandedSection.set(this.expandedSection() === id ? null : id);
  }

  filteredSections() {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return this.sections;
    return this.sections
      .map(s => ({
        ...s,
        items: s.items.filter(i => i.title.toLowerCase().includes(q) || i.description.toLowerCase().includes(q))
      }))
      .filter(s => s.items.length > 0);
  }
}
