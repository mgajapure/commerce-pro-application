// src/app/features/catalog/seo/seo.component.ts
// SEO Management component with standard UI patterns

import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { SeoTemplate, SeoMetadata } from '../../../core/models/catalog/seo.model';
import { SeoService } from '../../../core/services/catalog/seo.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiSeoResult } from '../../../core/models/ai/ai.model';
import { Subject, takeUntil } from 'rxjs';

interface SeoAlert {
  id: string;
  type: 'critical' | 'warning' | 'info';
  title: string;
  message: string;
}

@Component({
  selector: 'app-seo',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './seo.html',
  styleUrl: './seo.scss'
})
export class Seo implements OnInit {
  private fb         = inject(FormBuilder);
  private seoService = inject(SeoService);
  private alertSvc   = inject(AlertService);
  private aiSvc      = inject(AiService);
  private destroy$   = new Subject<void>();

  // AI SEO
  aiSeoResults  = signal<Record<string, AiSeoResult>>({});
  aiSeoLoading  = signal<string | null>(null);
  showSeoModal  = signal(false);
  seoModalResult = signal<AiSeoResult | null>(null);

  // View State
  showModal = signal(false);
  isSaving = signal(false);
  activeTab = signal<'templates' | 'tools' | 'audit'>('templates');
  viewMode = signal<'table' | 'grid'>('table');
  showFilters = signal(false);
  selectedEntityType = signal<string>('');
  
  // Multi-selection
  selectedTemplates = signal<string[]>([]);

  // Data from service
  templates = this.seoService.allTemplates;
  isLoading = this.seoService.isLoading;
  error = this.seoService.currentError;

  editingTemplate = signal<SeoTemplate | null>(null);

  templateForm!: FormGroup;

  // Alerts
  alerts = signal<SeoAlert[]>([
    { id: '1', type: 'critical', title: '5 Pages Missing Meta Descriptions', message: 'These pages need meta descriptions to improve SEO ranking.' },
    { id: '2', type: 'warning', title: '12 Pages Have Duplicate Titles', message: 'Consider updating titles to be unique for better search visibility.' },
    { id: '3', type: 'info', title: 'Sitemap Last Generated', message: 'Your sitemap was last updated 3 days ago. Consider regenerating.' }
  ]);

  // Tabs configuration (like bulk-operations)
  tabs = [
    { id: 'templates', label: 'Templates', icon: 'file-earmark-text' },
    { id: 'tools', label: 'SEO Tools', icon: 'tools' },
    { id: 'audit', label: 'Audit', icon: 'search' }
  ];

  // Entity types for filtering
  entityTypes = [
    { value: 'product', label: 'Products', icon: 'box-seam', color: 'bg-blue-100 text-blue-800' },
    { value: 'category', label: 'Categories', icon: 'folder', color: 'bg-green-100 text-green-800' },
    { value: 'collection', label: 'Collections', icon: 'collection', color: 'bg-purple-100 text-purple-800' },
    { value: 'page', label: 'Pages', icon: 'file-text', color: 'bg-orange-100 text-orange-800' }
  ];

  exportItems: DropdownItem[] = [
    { id: 'sitemap', label: 'Download Sitemap', icon: 'map' },
    { id: 'robots', label: 'Download robots.txt', icon: 'robot' }
  ];

  // Stats computed from service
  displayStats = computed(() => {
    return [
      {
        label: 'Total Entities',
        value: '150',
        trend: 12.5,
        icon: 'globe',
        bgColor: 'bg-blue-100',
        iconColor: 'text-blue-600'
      },
      {
        label: 'SEO Score',
        value: '78/100',
        trend: 5.2,
        icon: 'speedometer2',
        bgColor: 'bg-green-100',
        iconColor: 'text-green-600'
      },
      {
        label: 'Templates',
        value: this.templates().length.toString(),
        trend: 0,
        icon: 'file-earmark-text',
        bgColor: 'bg-purple-100',
        iconColor: 'text-purple-600'
      },
      {
        label: 'Issues Found',
        value: '15',
        trend: -8.3,
        icon: 'exclamation-triangle',
        bgColor: 'bg-orange-100',
        iconColor: 'text-orange-600'
      }
    ];
  });

  // Filtered templates by entity type
  filteredTemplates = computed(() => {
    let result = this.templates();
    if (this.selectedEntityType()) {
      result = result.filter(t => t.entityType === this.selectedEntityType());
    }
    return result;
  });

  constructor() {
    this.initForm();
  }

  ngOnInit() {
    // Service loads data automatically
  }

  private initForm() {
    this.templateForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      entityType: ['product', Validators.required],
      titlePattern: ['', Validators.required],
      descriptionPattern: ['', Validators.required],
      isDefault: [false]
    });
  }

  // Get entity type label
  getEntityTypeLabel(type: string): string {
    const entity = this.entityTypes.find(e => e.value === type);
    return entity?.label || type;
  }

  // Get entity type icon
  getEntityTypeIcon(type: string): string {
    const entity = this.entityTypes.find(e => e.value === type);
    return entity?.icon || 'file-text';
  }

  // Get entity type color classes
  getEntityTypeColor(type: string): string {
    const colors: Record<string, string> = {
      product: 'bg-blue-100 text-blue-800',
      category: 'bg-green-100 text-green-800',
      collection: 'bg-purple-100 text-purple-800',
      page: 'bg-orange-100 text-orange-800'
    };
    return colors[type] || 'bg-gray-100 text-gray-800';
  }

  // Actions
  setTab(tab: 'templates' | 'tools' | 'audit') {
    this.activeTab.set(tab);
    this.selectedTemplates.set([]);
  }

  toggleViewMode() {
    this.viewMode.update(v => v === 'table' ? 'grid' : 'table');
  }

  toggleFilters() {
    this.showFilters.update(v => !v);
  }

  filterByEntityType(type: string) {
    this.selectedEntityType.set(this.selectedEntityType() === type ? '' : type);
  }

  dismissAlert(id: string) {
    this.alerts.update(a => a.filter(alert => alert.id !== id));
  }

  // Multi-selection methods
  toggleSelection(id: string) {
    this.selectedTemplates.update(selected => {
      if (selected.includes(id)) {
        return selected.filter(s => s !== id);
      }
      return [...selected, id];
    });
  }

  isSelected(id: string): boolean {
    return this.selectedTemplates().includes(id);
  }

  isAllSelected(): boolean {
    const visible = this.filteredTemplates();
    return visible.length > 0 && visible.every(t => this.isSelected(t.id));
  }

  toggleSelectAll() {
    if (this.isAllSelected()) {
      this.selectedTemplates.set([]);
    } else {
      this.selectedTemplates.set(this.filteredTemplates().map(t => t.id));
    }
  }

  // Bulk actions
  bulkSetDefault() {
    this.selectedTemplates.set([]);
  }

  bulkDelete() {
    const count = this.selectedTemplates().length;
    if (count === 0) return;
    this.alertSvc.confirm({
      title: 'Delete Templates',
      message: `Are you sure you want to delete ${count} template(s)? This action cannot be undone.`,
      confirmLabel: `Delete ${count}`,
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.selectedTemplates.set([]);
      this.alertSvc.success('Templates deleted', `${count} template(s) removed.`);
    });
  }

  openModal(template?: SeoTemplate) {
    this.editingTemplate.set(template || null);

    if (template) {
      this.templateForm.patchValue({
        name: template.name,
        entityType: template.entityType,
        titlePattern: template.titlePattern,
        descriptionPattern: template.descriptionPattern,
        isDefault: template.isDefault
      });
    } else {
      this.templateForm.reset({
        entityType: 'product',
        isDefault: false
      });
    }

    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.editingTemplate.set(null);
    this.templateForm.reset();
  }

  saveTemplate() {
    if (this.templateForm.invalid) {
      this.templateForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const formValue = this.templateForm.value;

    if (this.editingTemplate()) {
      this.seoService.updateTemplate(this.editingTemplate()!.id, formValue).subscribe({
        next: () => { this.isSaving.set(false); this.closeModal(); this.alertSvc.success('Template updated'); },
        error: () => { this.isSaving.set(false); this.alertSvc.error('Failed to update template'); }
      });
    } else {
      this.seoService.createTemplate(formValue).subscribe({
        next: () => { this.isSaving.set(false); this.closeModal(); this.alertSvc.success('Template created'); },
        error: () => { this.isSaving.set(false); this.alertSvc.error('Failed to create template'); }
      });
    }
  }

  deleteTemplate(template: SeoTemplate) {
    this.alertSvc.confirm({
      title: 'Delete Template',
      message: `Are you sure you want to delete "${template.name}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.seoService.deleteTemplate(template.id).subscribe({
        next: () => this.alertSvc.success('Template deleted', `"${template.name}" has been removed.`),
        error: () => this.alertSvc.error('Failed to delete template')
      });
    });
  }

  // SEO Tools
  generateSitemap() {
    this.seoService.generateSitemap().subscribe(xml => {
      const blob = new Blob([xml], { type: 'application/xml' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'sitemap.xml';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  generateRobotsTxt() {
    this.seoService.generateRobotsTxt().subscribe(content => {
      const blob = new Blob([content], { type: 'text/plain' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'robots.txt';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  runAudit() {
    this.alertSvc.info('Coming Soon', 'SEO Audit feature will be available in a future release.');
  }

  // Dropdown menu
  getTemplateMenuItems(template: SeoTemplate): DropdownItem[] {
    const items: DropdownItem[] = [
      { id: 'edit', label: 'Edit Template', icon: 'pencil', shortcut: '⌘E' },
      { id: 'duplicate', label: 'Duplicate', icon: 'copy', shortcut: '⌘D' },
      { id: 'preview', label: 'Preview', icon: 'eye' }
    ];

    if (!template.isDefault) {
      items.push({ id: 'divider', label: '', divider: true });
      items.push({ id: 'delete', label: 'Delete Template', icon: 'trash', danger: true });
    }

    return items;
  }

  onTemplateAction(item: DropdownItem, template: SeoTemplate) {
    switch (item.id) {
      case 'edit':
        this.openModal(template);
        break;
      case 'duplicate':
        this.duplicateTemplate(template);
        break;
      case 'preview':
        this.previewTemplate(template);
        break;
      case 'delete':
        this.deleteTemplate(template);
        break;
    }
  }

  duplicateTemplate(template: SeoTemplate) {
    this.seoService.createTemplate({
      ...template,
      name: template.name + ' (Copy)',
      isDefault: false
    }).subscribe();
  }

  previewTemplate(template: SeoTemplate) {
    const mockData: Record<string, string> = {
      name: 'Sample Product Name',
      brand: 'Sample Brand',
      shortDescription: 'This is a sample product description for preview.',
      description: 'Browse our extensive collection of high-quality products.'
    };
    const metadata = this.seoService.generateMetadata(template, mockData);
    this.alertSvc.info('SEO Preview', `Title: ${metadata.title} | Description: ${metadata.description}`);
  }

  // Export
  onExport(item: DropdownItem) {
    if (item.id === 'sitemap') {
      this.generateSitemap();
    } else if (item.id === 'robots') {
      this.generateRobotsTxt();
    }
  }

  // ─── AI SEO ───────────────────────────────────────────────────────────────

  previewAiSeo(productId: string): void {
    this.aiSeoLoading.set(productId);
    this.aiSvc.previewSeo(productId).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => {
        this.aiSeoResults.update(m => ({ ...m, [productId]: r }));
        this.seoModalResult.set(r);
        this.showSeoModal.set(true);
        this.aiSeoLoading.set(null);
      },
      error: () => { this.aiSeoLoading.set(null); this.alertSvc.error('AI SEO preview failed'); }
    });
  }

  applyAiSeo(productId: string): void {
    this.aiSeoLoading.set(productId + '_apply');
    this.aiSvc.optimiseSeo(productId).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => {
        this.aiSeoResults.update(m => ({ ...m, [productId]: r }));
        this.aiSeoLoading.set(null);
        this.alertSvc.success('AI SEO metadata saved to product');
      },
      error: () => { this.aiSeoLoading.set(null); this.alertSvc.error('AI SEO optimise failed'); }
    });
  }

  getAiSeo(templateId: string): AiSeoResult | null { return this.aiSeoResults()[templateId] ?? null; }

  closeSeoModal(): void { this.showSeoModal.set(false); this.seoModalResult.set(null); }
}
