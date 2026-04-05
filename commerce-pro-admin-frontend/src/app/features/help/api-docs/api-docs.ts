import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';

@Component({
  selector: 'app-api-docs',
  standalone: true,
  imports: [CommonModule, HelpSidebar],
  templateUrl: './api-docs.html',
  styles: [`:host { display: block; }`]
})
export class ApiDocs {
  expandedApi = signal<string | null>('auth');
  copiedUrl = signal(false);
  baseUrl = 'https://api.commercepro.com/api/v1';

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Authentication', content: 'All API requests (except login) require a Bearer token in the Authorization header.' },
    { title: 'Rate Limits', content: 'API requests are limited to 1000 requests per minute per API key.' },
    { title: 'Error Handling', content: 'All errors return standard HTTP status codes with a JSON error body containing a message field.' }
  ];

  apis = [
    {
      id: 'auth', name: 'Authentication', icon: 'bi-shield-lock',
      endpoints: [
        { method: 'POST', path: '/auth/login', description: 'Authenticate and receive JWT tokens', body: '{ "username": "admin", "password": "****" }' },
        { method: 'POST', path: '/auth/refresh', description: 'Refresh access token', body: '{ "refreshToken": "..." }' },
        { method: 'POST', path: '/auth/logout', description: 'Invalidate current session', body: null },
      ]
    },
    {
      id: 'products', name: 'Products API', icon: 'bi-box-seam',
      endpoints: [
        { method: 'GET', path: '/products', description: 'List all products (paginated)', body: null },
        { method: 'GET', path: '/products/{id}', description: 'Get product by ID', body: null },
        { method: 'POST', path: '/products', description: 'Create a new product', body: '{ "name": "...", "price": 29.99, "sku": "..." }' },
        { method: 'PUT', path: '/products/{id}', description: 'Update product', body: '{ "name": "...", "price": 39.99 }' },
        { method: 'DELETE', path: '/products/{id}', description: 'Delete product', body: null },
      ]
    },
    {
      id: 'orders', name: 'Orders API', icon: 'bi-bag',
      endpoints: [
        { method: 'GET', path: '/orders', description: 'List orders with filters', body: null },
        { method: 'GET', path: '/orders/{id}', description: 'Get order details', body: null },
        { method: 'PUT', path: '/orders/{id}/status', description: 'Update order status', body: '{ "status": "PROCESSING" }' },
      ]
    },
    {
      id: 'customers', name: 'Customers API', icon: 'bi-people',
      endpoints: [
        { method: 'GET', path: '/customers', description: 'List customers', body: null },
        { method: 'GET', path: '/customers/{id}', description: 'Get customer details', body: null },
        { method: 'POST', path: '/customers', description: 'Create customer', body: '{ "email": "...", "firstName": "...", "lastName": "..." }' },
      ]
    },
    {
      id: 'inventory', name: 'Inventory API', icon: 'bi-boxes',
      endpoints: [
        { method: 'GET', path: '/inventory', description: 'Get stock levels', body: null },
        { method: 'PUT', path: '/inventory/{productId}', description: 'Update stock', body: '{ "quantity": 100, "warehouseId": "..." }' },
      ]
    },
  ];

  toggleApi(id: string) {
    this.expandedApi.set(this.expandedApi() === id ? null : id);
  }

  copyBaseUrl() {
    navigator.clipboard.writeText(this.baseUrl);
    this.copiedUrl.set(true);
    setTimeout(() => this.copiedUrl.set(false), 2000);
  }

  methodClass(method: string): string {
    switch (method) {
      case 'GET': return 'bg-green-100 text-green-700';
      case 'POST': return 'bg-blue-100 text-blue-700';
      case 'PUT': return 'bg-yellow-100 text-yellow-700';
      case 'DELETE': return 'bg-red-100 text-red-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  }

  methodDot(method: string): string {
    switch (method) {
      case 'GET': return 'bg-green-500';
      case 'POST': return 'bg-blue-500';
      case 'PUT': return 'bg-yellow-500';
      case 'DELETE': return 'bg-red-500';
      default: return 'bg-gray-500';
    }
  }
}
