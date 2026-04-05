import { Component, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';

@Component({
  selector: 'app-system-status',
  standalone: true,
  imports: [CommonModule, HelpSidebar],
  templateUrl: './status.html',
  styles: [`:host { display: block; }`]
})
export class SystemStatus implements OnInit {
  loading = signal(true);
  lastUpdated = signal(new Date().toLocaleString());

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'System Status', content: 'This page shows real-time health status of all Commerce Pro services.' },
    { title: 'Status Indicators', content: 'Green = Operational, Yellow = Degraded performance, Red = Outage or service unavailable.' },
    { title: 'Incident History', content: 'Past incidents and maintenance events with resolution details and duration.' }
  ];

  services = signal([
    { name: 'API Server', status: 'operational', uptime: 99.99, responseTime: 45, icon: 'bi-hdd-rack' },
    { name: 'Database', status: 'operational', uptime: 99.98, responseTime: 12, icon: 'bi-database' },
    { name: 'Cache (Redis)', status: 'operational', uptime: 99.99, responseTime: 2, icon: 'bi-lightning' },
    { name: 'Search Engine', status: 'operational', uptime: 99.95, responseTime: 35, icon: 'bi-search' },
    { name: 'File Storage', status: 'operational', uptime: 99.99, responseTime: 85, icon: 'bi-cloud' },
    { name: 'Email Service', status: 'operational', uptime: 99.90, responseTime: 250, icon: 'bi-envelope' },
    { name: 'Payment Gateway', status: 'operational', uptime: 99.97, responseTime: 320, icon: 'bi-credit-card' },
    { name: 'CDN', status: 'operational', uptime: 99.99, responseTime: 18, icon: 'bi-globe' },
  ]);

  overallStatus = signal<'operational' | 'degraded' | 'outage'>('operational');

  incidents = signal([
    { date: '2026-03-28', title: 'Elevated API latency', status: 'resolved', duration: '23 minutes', description: 'Brief spike in API response times due to database maintenance.' },
    { date: '2026-03-15', title: 'Email delivery delays', status: 'resolved', duration: '1 hour 12 minutes', description: 'Third-party email provider experienced delays.' },
    { date: '2026-02-20', title: 'Scheduled maintenance', status: 'completed', duration: '45 minutes', description: 'Database migration and system upgrades.' },
  ]);

  operationalCount = computed(() => this.services().filter(s => s.status === 'operational').length);
  avgUptime = computed(() => {
    const svcs = this.services();
    return svcs.length ? (svcs.reduce((sum, s) => sum + s.uptime, 0) / svcs.length).toFixed(2) : '0';
  });

  ngOnInit() {
    setTimeout(() => this.loading.set(false), 300);
  }

  refresh() {
    this.loading.set(true);
    this.lastUpdated.set(new Date().toLocaleString());
    setTimeout(() => this.loading.set(false), 300);
  }

  statusBg(status: string): string {
    switch (status) {
      case 'operational': return 'bg-green-50 border-green-200';
      case 'degraded': return 'bg-yellow-50 border-yellow-200';
      case 'outage': return 'bg-red-50 border-red-200';
      default: return 'bg-gray-50 border-gray-200';
    }
  }

  statusText(status: string): string {
    switch (status) {
      case 'operational': return 'text-green-800';
      case 'degraded': return 'text-yellow-800';
      case 'outage': return 'text-red-800';
      default: return 'text-gray-800';
    }
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
