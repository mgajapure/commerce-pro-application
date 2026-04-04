import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-system-status',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status.html',
  styles: [`:host { display: block; }`]
})
export class SystemStatus implements OnInit {
  loading = signal(true);
  lastUpdated = signal(new Date().toLocaleString());

  services = signal([
    { name: 'API Server', status: 'operational', uptime: 99.99, responseTime: 45 },
    { name: 'Database', status: 'operational', uptime: 99.98, responseTime: 12 },
    { name: 'Cache (Redis)', status: 'operational', uptime: 99.99, responseTime: 2 },
    { name: 'Search Engine', status: 'operational', uptime: 99.95, responseTime: 35 },
    { name: 'File Storage', status: 'operational', uptime: 99.99, responseTime: 85 },
    { name: 'Email Service', status: 'operational', uptime: 99.90, responseTime: 250 },
    { name: 'Payment Gateway', status: 'operational', uptime: 99.97, responseTime: 320 },
    { name: 'CDN', status: 'operational', uptime: 99.99, responseTime: 18 },
  ]);

  overallStatus = signal<'operational' | 'degraded' | 'outage'>('operational');

  incidents = signal([
    { date: '2026-03-28', title: 'Elevated API latency', status: 'resolved', duration: '23 minutes', description: 'Brief spike in API response times due to database maintenance.' },
    { date: '2026-03-15', title: 'Email delivery delays', status: 'resolved', duration: '1 hour 12 minutes', description: 'Third-party email provider experienced delays.' },
    { date: '2026-02-20', title: 'Scheduled maintenance', status: 'completed', duration: '45 minutes', description: 'Database migration and system upgrades.' },
  ]);

  ngOnInit() {
    setTimeout(() => this.loading.set(false), 300);
  }

  statusClass(status: string): string {
    switch (status) {
      case 'operational': return 'bg-green-100 text-green-800';
      case 'degraded': return 'bg-yellow-100 text-yellow-800';
      case 'outage': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  statusDot(status: string): string {
    switch (status) {
      case 'operational': return 'bg-green-500';
      case 'degraded': return 'bg-yellow-500';
      case 'outage': return 'bg-red-500';
      default: return 'bg-gray-500';
    }
  }
}
