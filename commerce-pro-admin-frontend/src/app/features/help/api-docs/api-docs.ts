import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-api-docs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './api-docs.html',
  styles: [`:host { display: block; }`]
})
export class ApiDocs {
  expandedApi = signal<string | null>('auth');
  copiedUrl = signal(false);
  baseUrl = 'https://api.commercepro.com/api/v1';

  apis = [
    {
      id: 'auth',
      name: 'Authentication',
      endpoints: [
        { method: 'POST', path: '/auth/login', description: 'Authenticate and receive JWT tokens', body: '{ "username": "admin", "password": "****" }' },
        { method: 'POST', path: '/auth/refresh', description: 'Refresh access token', body: '{ "refreshToken": "..." }' },
        { method: 'POST', path: '/auth/logout', description: 'Invalidate current session', body: null },
      ]
    },
    {
      id: 'products',
      name: 'Products API',
      endpoints: [
        { method: 'GET', path: '/products', description: 'List all products (paginated)', body: null },
        { method: 'GET', path: '/products/{id}', description: 'Get product by ID', body: null },
        { method: 'POST', path: '/products', description: 'Create a new product', body: '{ "name": "...", "price": 29.99, "sku": "..." }' },
        { method: 'PUT', path: '/products/{id}', description: 'Update product', body: '{ "name": "...", "price": 39.99 }' },
        { method: 'DELETE', path: '/products/{id}', description: 'Delete product', body: null },
      ]
    },
    {
      id: 'orders',
      name: 'Orders API',
      endpoints: [
        { method: 'GET', path: '/orders', description: 'List orders with filters', body: null },
        { method: 'GET', path: '/orders/{id}', description: 'Get order details', body: null },
        { method: 'PUT', path: '/orders/{id}/status', description: 'Update order status', body: '{ "status": "PROCESSING" }' },
      ]
    },
    {
      id: 'customers',
      name: 'Customers API',
      endpoints: [
        { method: 'GET', path: '/customers', description: 'List customers', body: null },
        { method: 'GET', path: '/customers/{id}', description: 'Get customer details', body: null },
        { method: 'POST', path: '/customers', description: 'Create customer', body: '{ "email": "...", "firstName": "...", "lastName": "..." }' },
      ]
    },
    {
      id: 'inventory',
      name: 'Inventory API',
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
}
