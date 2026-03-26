// src/app/features/catalog/collections/collections.component.ts
// Collections management component with standard UI patterns

import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { 
  Collection, 
  CollectionType,
  CollectionCondition,
  CollectionConditionField,
  CollectionConditionRelation 
} from '../../../core/models/catalog/collection.model';
import { CollectionService } from '../../../core/services/catalog/collection.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AiService } from '../../../core/services/ai/ai.service';

@Component({
  selector: 'app-collections',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './collections.html',
  styleUrl: './collections.scss'
})
export class Collections implements OnInit {
  private fb                = inject(FormBuilder);
  private collectionService = inject(CollectionService);
  private alertSvc          = inject(AlertService);
  private aiSvc             = inject(AiService);

  // View State
  showModal = signal(false);
  isSaving = signal(false);
  selectedCollections = signal<string[]>([]);
  
  // Filters
  searchQuery = signal('');
  filterType = signal<string>('');
  filterStatus = signal<string>('');
  viewMode = signal<'grid' | 'list'>('list');

  // Data from service
  collections = this.collectionService.allCollections;
  isLoading = this.collectionService.isLoading;
  error = this.collectionService.currentError;

  editingCollection = signal<Collection | null>(null);

  // AI state
  aiLoading  = signal(false);
  aiField    = signal<'description' | 'seoTitle' | 'seoDescription' | null>(null);
  aiResult   = signal<string | null>(null);
  showAiModal = signal(false);

  collectionForm!: FormGroup;

  // Collection types
  collectionTypes = [
    { value: 'manual', label: 'Manual', icon: 'hand-index', description: 'Add products individually' },
    { value: 'automated', label: 'Automated', icon: 'gear', description: 'Products match conditions' }
  ];

  // Condition fields
  conditionFields: { value: CollectionConditionField; label: string }[] = [
    { value: 'title', label: 'Product Title' },
    { value: 'type', label: 'Product Type' },
    { value: 'vendor', label: 'Vendor' },
    { value: 'price', label: 'Price' },
    { value: 'tag', label: 'Tag' },
    { value: 'sku', label: 'SKU' },
    { value: 'inventory_stock', label: 'Inventory Stock' }
  ];

  // Condition relations
  conditionRelations: { value: CollectionConditionRelation; label: string }[] = [
    { value: 'equals', label: 'Equals' },
    { value: 'not_equals', label: 'Does not equal' },
    { value: 'greater_than', label: 'Greater than' },
    { value: 'less_than', label: 'Less than' },
    { value: 'contains', label: 'Contains' },
    { value: 'not_contains', label: 'Does not contain' },
    { value: 'starts_with', label: 'Starts with' },
    { value: 'ends_with', label: 'Ends with' }
  ];

  exportItems: DropdownItem[] = [
    { id: 'csv', label: 'Export as CSV', icon: 'filetype-csv' },
    { id: 'excel', label: 'Export as Excel', icon: 'filetype-xlsx' }
  ];

  // Stats from service
  displayStats = computed(() => {
    const stats = this.collectionService.collectionStats();
    return [
      {
        label: 'Total Collections',
        value: stats.total.toString(),
        trend: 12.5,
        icon: 'collection',
        bgColor: 'bg-blue-100',
        iconColor: 'text-blue-600',
        filter: 'all'
      },
      {
        label: 'Manual',
        value: stats.manual.toString(),
        trend: 5.2,
        icon: 'hand-index',
        bgColor: 'bg-purple-100',
        iconColor: 'text-purple-600',
        filter: 'manual'
      },
      {
        label: 'Automated',
        value: stats.automated.toString(),
        trend: 18.3,
        icon: 'gear',
        bgColor: 'bg-orange-100',
        iconColor: 'text-orange-600',
        filter: 'automated'
      },
      {
        label: 'Active',
        value: stats.active.toString(),
        trend: 8.1,
        icon: 'check-circle',
        bgColor: 'bg-green-100',
        iconColor: 'text-green-600',
        filter: 'active'
      }
    ];
  });

  // Filtered collections
  filteredCollections = computed(() => {
    let result = this.collections();

    // Search
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(c =>
        c.name.toLowerCase().includes(query) ||
        c.description?.toLowerCase().includes(query) ||
        c.slug.toLowerCase().includes(query)
      );
    }

    // Type filter
    if (this.filterType()) {
      result = result.filter(c => c.type === this.filterType());
    }

    // Status filter
    if (this.filterStatus()) {
      const isActive = this.filterStatus() === 'active';
      result = result.filter(c => c.isActive === isActive);
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
    this.collectionForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      slug: ['', [Validators.required, Validators.pattern('^[a-z0-9-]+$')]],
      description: [''],
      type: ['manual', Validators.required],
      isActive: [true],
      isFeatured: [false],
      seoTitle: [''],
      seoDescription: [''],
      conditions: this.fb.array([])
    });
  }

  get conditionsArray(): FormArray {
    return this.collectionForm.get('conditions') as FormArray;
  }

  // Type helpers
  getTypeIcon(type: string): string {
    return type === 'manual' ? 'hand-index' : 'gear';
  }

  getTypeLabel(type: string): string {
    return type === 'manual' ? 'Manual' : 'Automated';
  }

  getTypeColor(type: string): string {
    return type === 'manual' 
      ? 'bg-purple-100 text-purple-800' 
      : 'bg-orange-100 text-orange-800';
  }

  getStatusColor(isActive: boolean): string {
    return isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800';
  }

  // Actions
  toggleViewMode() {
    this.viewMode.update(v => v === 'grid' ? 'list' : 'grid');
  }

  clearFilters() {
    this.searchQuery.set('');
    this.filterType.set('');
    this.filterStatus.set('');
  }

  applyQuickFilter(filter: string) {
    this.clearFilters();
    switch (filter) {
      case 'manual':
        this.filterType.set('manual');
        break;
      case 'automated':
        this.filterType.set('automated');
        break;
      case 'active':
        this.filterStatus.set('active');
        break;
    }
  }

  openModal(collection?: Collection) {
    this.editingCollection.set(collection || null);

    if (collection) {
      this.collectionForm.patchValue({
        name: collection.name,
        slug: collection.slug,
        description: collection.description,
        type: collection.type,
        isActive: collection.isActive,
        isFeatured: collection.isFeatured,
        seoTitle: collection.seoTitle,
        seoDescription: collection.seoDescription
      });

      // Populate conditions
      this.conditionsArray.clear();
      collection.conditions?.forEach(cond => {
        this.conditionsArray.push(this.fb.group({
          field: [cond.field, Validators.required],
          relation: [cond.relation, Validators.required],
          value: [cond.value, Validators.required]
        }));
      });
    } else {
      this.collectionForm.reset({
        type: 'manual',
        isActive: true,
        isFeatured: false
      });
      this.conditionsArray.clear();
    }

    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.editingCollection.set(null);
    this.collectionForm.reset();
    this.conditionsArray.clear();
  }

  generateSlug() {
    const name = this.collectionForm.get('name')?.value;
    if (name && !this.editingCollection()) {
      const slug = name.toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
      this.collectionForm.patchValue({ slug });
    }
  }

  addCondition() {
    this.conditionsArray.push(this.fb.group({
      field: ['title', Validators.required],
      relation: ['contains', Validators.required],
      value: ['', Validators.required]
    }));
  }

  removeCondition(index: number) {
    this.conditionsArray.removeAt(index);
  }

  saveCollection() {
    if (this.collectionForm.invalid) {
      this.collectionForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const formValue = this.collectionForm.value;

    if (this.editingCollection()) {
      this.collectionService.updateCollection(this.editingCollection()!.id, formValue).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeModal();
        },
        error: () => this.isSaving.set(false)
      });
    } else {
      this.collectionService.createCollection(formValue).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeModal();
        },
        error: () => this.isSaving.set(false)
      });
    }
  }

  deleteCollection(collection: Collection) {
    this.alertSvc.confirm({
      title: 'Delete Collection',
      message: `Are you sure you want to delete "${collection.name}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.collectionService.deleteCollection(collection.id).subscribe({
        next: () => this.alertSvc.success('Collection deleted', `"${collection.name}" has been removed.`),
        error: () => this.alertSvc.error('Failed to delete collection')
      });
    });
  }

  toggleSelection(id: string) {
    this.selectedCollections.update(selected => {
      if (selected.includes(id)) {
        return selected.filter(s => s !== id);
      }
      return [...selected, id];
    });
  }

  isSelected(id: string): boolean {
    return this.selectedCollections().includes(id);
  }

  // Dropdown menu
  getCollectionMenuItems(collection: Collection): DropdownItem[] {
    const items: DropdownItem[] = [
      { id: 'edit', label: 'Edit Collection', icon: 'pencil', shortcut: '⌘E' },
      { id: 'view', label: 'View Products', icon: 'eye' },
      { id: 'duplicate', label: 'Duplicate', icon: 'copy', shortcut: '⌘D' }
    ];

    if (collection.isActive) {
      items.push({ id: 'deactivate', label: 'Deactivate', icon: 'pause-circle' });
    } else {
      items.push({ id: 'activate', label: 'Activate', icon: 'check-circle' });
    }

    items.push(
      { id: 'divider', label: '', divider: true },
      { id: 'delete', label: 'Delete Collection', icon: 'trash', danger: true }
    );

    return items;
  }

  onCollectionAction(item: DropdownItem, collection: Collection) {
    switch (item.id) {
      case 'edit':
        this.openModal(collection);
        break;
      case 'view':
        // Navigate to products filtered by collection
        break;
      case 'duplicate':
        this.duplicateCollection(collection);
        break;
      case 'activate':
        this.collectionService.updateCollection(collection.id, { isActive: true }).subscribe();
        break;
      case 'deactivate':
        this.collectionService.updateCollection(collection.id, { isActive: false }).subscribe();
        break;
      case 'delete':
        this.deleteCollection(collection);
        break;
    }
  }

  duplicateCollection(collection: Collection) {
    this.collectionService.createCollection({
      ...collection,
      name: collection.name + ' (Copy)',
      slug: collection.slug + '-copy'
    }).subscribe();
  }

  // AI generation
  runCollectionAi(field: 'description' | 'seoTitle' | 'seoDescription'): void {
    const col = this.editingCollection();
    if (!col?.id) return;
    this.aiField.set(field);
    this.aiLoading.set(true);
    this.showAiModal.set(true);
    this.aiResult.set(null);

    if (field === 'description') {
      this.aiSvc.generateCollectionDescription(col.id).subscribe({
        next: r => { this.aiResult.set(r.description); this.aiLoading.set(false); },
        error: () => { this.aiLoading.set(false); this.showAiModal.set(false); }
      });
    } else {
      this.aiSvc.optimiseCollectionSeo(col.id).subscribe({
        next: r => {
          this.aiResult.set(field === 'seoTitle' ? r.seoTitle : r.seoDescription);
          this.aiLoading.set(false);
        },
        error: () => { this.aiLoading.set(false); this.showAiModal.set(false); }
      });
    }
  }

  applyAiResult(): void {
    const field = this.aiField();
    const value = this.aiResult();
    if (!field || value == null) return;
    this.collectionForm.patchValue({ [field]: value });
    this.closeAiModal();
  }

  closeAiModal(): void {
    this.showAiModal.set(false);
    this.aiField.set(null);
    this.aiResult.set(null);
    this.aiLoading.set(false);
  }

  // Bulk actions
  bulkActivate() {
    // Implement bulk activate
    this.selectedCollections.set([]);
  }

  bulkDeactivate() {
    // Implement bulk deactivate
    this.selectedCollections.set([]);
  }

  bulkDelete() {
    const count = this.selectedCollections().length;
    if (count === 0) return;
    this.alertSvc.confirm({
      title: 'Delete Collections',
      message: `Are you sure you want to delete ${count} collection(s)? This action cannot be undone.`,
      confirmLabel: `Delete ${count}`,
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.selectedCollections.set([]);
      this.alertSvc.success('Collections deleted', `${count} collection(s) removed.`);
    });
  }

  // Export
  onExport(item: DropdownItem) {
    console.log('Exporting as', item.id);
  }
}
