import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SystemService, FeatureFlag } from '../system.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-feature-flags',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './feature-flags.html',
  styles: [`:host { display: block; }`]
})
export class FeatureFlags implements OnInit, OnDestroy {
  private svc = inject(SystemService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  flags = signal<FeatureFlag[]>([]);
  isLoading = signal(true);
  searchQuery = signal('');
  filterCategory = signal<string>('all');

  // Modal
  showModal = signal(false);
  editingFlag = signal<FeatureFlag | null>(null);
  formKey = signal('');
  formDescription = signal('');
  formCategory = signal<FeatureFlag['category']>('Features');
  formEnabled = signal(false);
  formRollout = signal(0);
  formTargetGroups = signal('');
  isSaving = signal(false);

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'What are Feature Flags?', content: 'Feature flags allow you to enable or disable features without deploying new code. Use them for gradual rollouts, A/B testing, and kill switches.' },
    { title: 'Rollout Percentage', content: 'Control what percentage of users see a feature. Start at a low percentage and gradually increase as you gain confidence.' },
    { title: 'Target Groups', content: 'Restrict features to specific user groups like beta-testers, premium users, or internal staff.' },
    { title: 'Categories', content: 'Features are for product functionality, Experiments are for A/B tests, and Ops are for operational controls like maintenance mode.' }
  ];

  // Pagination
  currentPage = signal(1);
  itemsPerPage = signal(10);

  filteredFlags = computed(() => {
    let result = this.flags();
    const q = this.searchQuery().toLowerCase().trim();
    if (q) result = result.filter(f => f.key.toLowerCase().includes(q) || f.description.toLowerCase().includes(q));
    if (this.filterCategory() !== 'all') result = result.filter(f => f.category === this.filterCategory());
    return result;
  });

  totalPages = computed(() => Math.ceil(this.filteredFlags().length / this.itemsPerPage()));
  paginatedFlags = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.filteredFlags().slice(start, start + this.itemsPerPage());
  });

  enabledCount = computed(() => this.flags().filter(f => f.enabled).length);
  disabledCount = computed(() => this.flags().filter(f => !f.enabled).length);

  ngOnInit() {
    this.svc.getFeatureFlags().pipe(takeUntil(this.destroy$)).subscribe(flags => {
      if (flags.length === 0) flags = this.defaultFlags();
      this.flags.set(flags);
      this.isLoading.set(false);
    });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  toggleFlag(flag: FeatureFlag) {
    const newEnabled = !flag.enabled;
    this.flags.update(list => list.map(f => f.key === flag.key ? { ...f, enabled: newEnabled } : f));
    this.svc.updateFeatureFlag(flag.key, { enabled: newEnabled }).pipe(takeUntil(this.destroy$)).subscribe(result => {
      if (result) this.alertSvc.success(`${flag.key}`, newEnabled ? 'Enabled' : 'Disabled');
      else this.alertSvc.error('Failed', 'Could not update flag');
    });
  }

  updatePercentage(flag: FeatureFlag, pct: number) {
    this.flags.update(list => list.map(f => f.key === flag.key ? { ...f, rolloutPercentage: pct } : f));
    this.svc.updateFeatureFlag(flag.key, { rolloutPercentage: pct }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  openCreate() {
    this.editingFlag.set(null);
    this.formKey.set('');
    this.formDescription.set('');
    this.formCategory.set('Features');
    this.formEnabled.set(false);
    this.formRollout.set(0);
    this.formTargetGroups.set('');
    this.showModal.set(true);
  }

  openEdit(flag: FeatureFlag) {
    this.editingFlag.set(flag);
    this.formKey.set(flag.key);
    this.formDescription.set(flag.description);
    this.formCategory.set(flag.category);
    this.formEnabled.set(flag.enabled);
    this.formRollout.set(flag.rolloutPercentage);
    this.formTargetGroups.set(flag.targetGroups.join(', '));
    this.showModal.set(true);
  }

  saveFlag() {
    if (!this.formKey().trim()) {
      this.alertSvc.warning('Validation', 'Flag key is required');
      return;
    }

    this.isSaving.set(true);
    const groups = this.formTargetGroups().split(',').map(g => g.trim()).filter(Boolean);
    const data: Partial<FeatureFlag> = {
      key: this.formKey(),
      description: this.formDescription(),
      category: this.formCategory(),
      enabled: this.formEnabled(),
      rolloutPercentage: this.formRollout(),
      targetGroups: groups
    };

    if (this.editingFlag()) {
      this.svc.updateFeatureFlag(this.editingFlag()!.key, data).pipe(takeUntil(this.destroy$)).subscribe(result => {
        this.isSaving.set(false);
        if (result) {
          this.flags.update(list => list.map(f => f.key === this.editingFlag()!.key ? { ...f, ...data, updatedAt: new Date().toISOString() } as FeatureFlag : f));
          this.alertSvc.success('Flag Updated');
          this.showModal.set(false);
        } else {
          this.alertSvc.error('Failed', 'Could not update flag');
        }
      });
    } else {
      const newFlag: FeatureFlag = {
        key: this.formKey(),
        description: this.formDescription(),
        category: this.formCategory(),
        enabled: this.formEnabled(),
        rolloutPercentage: this.formRollout(),
        targetGroups: groups,
        updatedAt: new Date().toISOString()
      };
      this.flags.update(list => [...list, newFlag]);
      this.isSaving.set(false);
      this.alertSvc.success('Flag Created');
      this.showModal.set(false);
    }
  }

  getCategoryColor(cat: string): string {
    const m: Record<string, string> = {
      Features: 'bg-blue-100 text-blue-700',
      Experiments: 'bg-purple-100 text-purple-700',
      Ops: 'bg-orange-100 text-orange-700'
    };
    return m[cat] || 'bg-gray-100 text-gray-700';
  }

  // Pagination
  goToPage(p: number) { this.currentPage.set(p); }
  previousPage() { if (this.currentPage() > 1) this.currentPage.update(p => p - 1); }
  nextPage() { if (this.currentPage() < this.totalPages()) this.currentPage.update(p => p + 1); }
  visiblePages(): (number | string)[] {
    const total = this.totalPages(), cur = this.currentPage();
    if (total <= 7) { const p: number[] = []; for (let i = 1; i <= total; i++) p.push(i); return p; }
    if (cur <= 3) return [1, 2, 3, 4, '...', total];
    if (cur >= total - 2) return [1, '...', total-3, total-2, total-1, total];
    return [1, '...', cur-1, cur, cur+1, '...', total];
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
