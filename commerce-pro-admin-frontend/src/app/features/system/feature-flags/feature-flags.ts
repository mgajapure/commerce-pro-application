import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SystemService, FeatureFlag } from '../system.service';

@Component({
  selector: 'app-feature-flags',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './feature-flags.html',
  styles: [`:host { display: block; }`]
})
export class FeatureFlags implements OnInit {
  private svc = inject(SystemService);

  flags = signal<FeatureFlag[]>([]);
  isLoading = signal(true);
  message = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  searchQuery = signal('');
  filterCategory = signal<string>('all');

  filteredFlags = computed(() => {
    let result = this.flags();
    const q = this.searchQuery().toLowerCase().trim();
    if (q) result = result.filter(f => f.key.toLowerCase().includes(q) || f.description.toLowerCase().includes(q));
    if (this.filterCategory() !== 'all') result = result.filter(f => f.category === this.filterCategory());
    return result;
  });

  ngOnInit() {
    this.svc.getFeatureFlags().subscribe(flags => {
      if (flags.length === 0) flags = this.defaultFlags();
      this.flags.set(flags);
      this.isLoading.set(false);
    });
  }

  toggleFlag(flag: FeatureFlag) {
    const newEnabled = !flag.enabled;
    this.flags.update(list => list.map(f => f.key === flag.key ? { ...f, enabled: newEnabled } : f));
    this.svc.updateFeatureFlag(flag.key, { enabled: newEnabled }).subscribe(result => {
      if (result) this.flash('success', `${flag.key} ${newEnabled ? 'enabled' : 'disabled'}`);
      else this.flash('error', 'Failed to update flag');
    });
  }

  updatePercentage(flag: FeatureFlag, pct: number) {
    this.flags.update(list => list.map(f => f.key === flag.key ? { ...f, rolloutPercentage: pct } : f));
    this.svc.updateFeatureFlag(flag.key, { rolloutPercentage: pct }).subscribe();
  }

  private flash(type: 'success' | 'error', text: string) {
    this.message.set({ type, text });
    setTimeout(() => this.message.set(null), 3000);
  }

  private defaultFlags(): FeatureFlag[] {
    return [
      { key: 'new-checkout-flow', description: 'Enable redesigned checkout experience', enabled: false, category: 'Features', rolloutPercentage: 0, targetGroups: [], updatedAt: '2026-04-01' },
      { key: 'ai-recommendations', description: 'AI-powered product recommendations', enabled: true, category: 'Features', rolloutPercentage: 100, targetGroups: ['premium', 'beta-testers'], updatedAt: '2026-03-28' },
      { key: 'dark-mode', description: 'Dark mode for admin panel', enabled: true, category: 'Experiments', rolloutPercentage: 50, targetGroups: ['internal'], updatedAt: '2026-03-25' },
      { key: 'advanced-search', description: 'Elasticsearch-powered search', enabled: false, category: 'Features', rolloutPercentage: 0, targetGroups: [], updatedAt: '2026-03-20' },
      { key: 'maintenance-mode', description: 'Enable site-wide maintenance mode', enabled: false, category: 'Ops', rolloutPercentage: 100, targetGroups: [], updatedAt: '2026-03-15' },
      { key: 'beta-analytics', description: 'Beta analytics dashboard', enabled: true, category: 'Experiments', rolloutPercentage: 25, targetGroups: ['beta-testers'], updatedAt: '2026-03-10' },
    ];
  }
}
