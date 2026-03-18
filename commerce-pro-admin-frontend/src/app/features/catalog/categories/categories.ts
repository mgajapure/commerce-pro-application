// src/app/features/products/components/categories/categories.component.ts
import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { 
  Category, 
  CategoryTreeNode 
} from '../../../core/models/catalog/category.model';
import { CategoryService } from '../../../core/services/catalog/category.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './categories.html',
  styleUrl: './categories.scss'
})
export class Categories implements OnInit {
  private fb              = inject(FormBuilder);
  private categoryService = inject(CategoryService);
  private alertSvc        = inject(AlertService);
  
  // View state
  viewMode = signal<'tree' | 'grid'>('tree');
  showViewOptions = signal(false);
  showModal = signal(false);
  isSaving = signal(false);
  
  // Filters
  searchQuery = signal('');
  filterStatus = signal('');
  sortBy = signal('name');
  
  // Data from service
  categories = this.categoryService.allCategories;
  categoryTree = this.categoryService.categoryTree;
  isLoading = this.categoryService.isLoading;
  error = this.categoryService.currentError;
  
  selectedCategories = signal<string[]>([]);
  expandedNodes = signal<Set<string>>(new Set());
  editingCategory = signal<Category | null>(null);
  previewImage = signal<string | null>(null);
  
  categoryForm: FormGroup;
  
  // Stats from service
  categoryStats = this.categoryService.categoryStats;

  // Flatten tree for table view
  visibleTreeNodes = computed(() => {
    const nodes: CategoryTreeNode[] = [];
    const traverse = (treeNodes: CategoryTreeNode[]) => {
      for (const node of treeNodes) {
        const isExpanded = this.expandedNodes().has(node.id);
        nodes.push({ ...node, isExpanded });
        if (isExpanded && node.subcategories) {
          traverse(node.subcategories as CategoryTreeNode[]);
        }
      }
    };
    traverse(this.categoryTree());
    return nodes;
  });
  
  flatCategories = computed(() => {
    const flatten = (cats: CategoryTreeNode[]): CategoryTreeNode[] => {
      return cats.flatMap(c => [c, ...flatten(c.subcategories as CategoryTreeNode[] || [])]);
    };
    return flatten(this.categoryTree());
  });
  
  filteredCategories = computed(() => {
    let result = this.flatCategories();
    
    // Search
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(c => 
        c.name.toLowerCase().includes(query) ||
        c.slug.toLowerCase().includes(query) ||
        c.description?.toLowerCase().includes(query)
      );
    }
    
    // Status filter
    if (this.filterStatus()) {
      const isActive = this.filterStatus() === 'active';
      result = result.filter(c => c.isActive === isActive);
    }
    
    // Sort
    result = [...result].sort((a, b) => {
      switch (this.sortBy()) {
        case 'name': return a.name.localeCompare(b.name);
        case 'products': return b.productCount - a.productCount;
        case 'date': return (b.updatedAt?.getTime() || 0) - (a.updatedAt?.getTime() || 0);
        case 'order': return a.sortOrder - b.sortOrder;
        default: return 0;
      }
    });
    
    return result;
  });
  
  availableParents = computed(() => {
    const currentId = this.editingCategory()?.id;
    return this.flatCategories().filter(c => c.id !== currentId);
  });
  
  // Display stats
  displayStats = computed(() => {
    const stats = this.categoryStats();
    return [
      { label: 'Total Categories', value: stats.total, icon: 'bi-folder', bgColor: 'bg-blue-100', iconColor: 'text-blue-600', trend: 12 },
      { label: 'Active', value: stats.active, icon: 'bi-check-circle', bgColor: 'bg-green-100', iconColor: 'text-green-600', trend: 8 },
      { label: 'Featured', value: stats.featured, icon: 'bi-star', bgColor: 'bg-yellow-100', iconColor: 'text-yellow-600', trend: 5 },
      { label: 'Top Level', value: stats.topLevel, icon: 'bi-layers', bgColor: 'bg-purple-100', iconColor: 'text-purple-600', trend: 2 }
    ];
  });

  constructor() {
    this.categoryForm = this.fb.group({
      name: ['', Validators.required],
      slug: ['', Validators.required],
      parentId: [null],
      description: [''],
      color: ['#6366f1'],
      icon: ['folder'],
      image: [''],
      isActive: [true],
      isFeatured: [false],
      seoTitle: [''],
      seoDescription: [''],
      metaKeywords: ['']
    });
    
    // Expand root categories by default
    setTimeout(() => {
      this.categoryTree().forEach(node => {
        this.expandedNodes.update(set => new Set([...set, node.id]));
      });
    }, 100);
  }

  ngOnInit() {
    // Service loads data automatically
  }

  // Selection
  isSelected(id: string): boolean {
    return this.selectedCategories().includes(id);
  }
  
  toggleSelection(id: string) {
    this.selectedCategories.update(selected => {
      if (selected.includes(id)) {
        return selected.filter(s => s !== id);
      }
      return [...selected, id];
    });
  }
  
  isAllSelected(): boolean {
    const visible = this.visibleTreeNodes();
    return visible.length > 0 && visible.every(n => this.isSelected(n.id));
  }
  
  toggleSelectAll() {
    const visible = this.visibleTreeNodes().map(n => n.id);
    if (this.isAllSelected()) {
      this.selectedCategories.update(selected => selected.filter(id => !visible.includes(id)));
    } else {
      this.selectedCategories.update(selected => [...new Set([...selected, ...visible])]);
    }
  }

  // Tree operations
  toggleExpand(id: string) {
    this.expandedNodes.update(set => {
      const newSet = new Set(set);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  }
  
  expandAll() {
    const allIds = this.flatCategories().map(c => c.id);
    this.expandedNodes.update(() => new Set(allIds));
  }

  // CRUD operations
  openModal(category?: Category) {
    this.editingCategory.set(category || null);
    this.previewImage.set(category?.image || null);
    
    if (category) {
      this.categoryForm.patchValue({
        name: category.name,
        slug: category.slug,
        parentId: category.parentId,
        description: category.description,
        color: category.color,
        icon: category.icon,
        isActive: category.isActive,
        isFeatured: category.isFeatured,
        seoTitle: category.seoTitle,
        seoDescription: category.seoDescription,
        metaKeywords: category.metaKeywords
      });
    } else {
      this.categoryForm.reset({
        color: '#6366f1',
        icon: 'folder',
        isActive: true,
        isFeatured: false
      });
    }
    
    this.showModal.set(true);
  }
  
  closeModal() {
    this.showModal.set(false);
    this.editingCategory.set(null);
    this.previewImage.set(null);
    this.categoryForm.reset();
  }
  
  generateSlug() {
    const name = this.categoryForm.get('name')?.value;
    if (name && !this.editingCategory()) {
      const slug = name.toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
      this.categoryForm.patchValue({ slug });
    }
  }
  
  onImageSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.handleImage(file);
  }
  
  onImageDrop(event: DragEvent) {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) this.handleImage(file);
  }
  
  handleImage(file: File) {
    const reader = new FileReader();
    reader.onload = (e) => {
      this.previewImage.set(e.target?.result as string);
    };
    reader.readAsDataURL(file);
  }
  
  clearImage() {
    this.previewImage.set(null);
    this.categoryForm.patchValue({ image: '' });
  }
  
  saveCategory() {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }
    
    this.isSaving.set(true);
    const formValue = this.categoryForm.value;
    
    if (this.editingCategory()) {
      // Update existing
      this.categoryService.updateCategory(
        this.editingCategory()!.id,
        { ...formValue, image: this.previewImage() }
      ).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeModal();
        },
        error: () => this.isSaving.set(false)
      });
    } else {
      // Create new
      this.categoryService.createCategory({
        ...formValue,
        image: this.previewImage()
      }).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeModal();
        },
        error: () => this.isSaving.set(false)
      });
    }
  }
  
  editCategory(category: Category) {
    this.openModal(category);
  }
  
  addSubcategory(parent: Category) {
    this.openModal();
    this.categoryForm.patchValue({ parentId: parent.id });
  }
  
  deleteCategory(category: Category) {
    const hasSubcategories = this.categories().some(c => c.parentId === category.id);
    const hasProducts      = category.productCount > 0;
    let detail = '';
    if (hasSubcategories) detail += 'This category has subcategories that will also be deleted. ';
    if (hasProducts)      detail += `This category contains ${category.productCount} product(s).`;

    this.alertSvc.confirm({
      title: `Delete "${category.name}"`,
      message: `Are you sure you want to delete this category?${detail ? ' ' + detail : ''}`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.categoryService.deleteCategory(category.id).subscribe({
        next: () => this.alertSvc.success('Category deleted'),
        error: () => this.alertSvc.error('Failed to delete category')
      });
    });
  }
  
  toggleStatus(category: Category) {
    this.categoryService.updateCategory(
      category.id,
      { isActive: !category.isActive }
    ).subscribe();
  }

  // Reordering
  moveUp(node: CategoryTreeNode) {
    if (node.sortOrder === 0) return;
    
    this.categoryService.reorderCategory(node.id, node.sortOrder - 1).subscribe();
  }
  
  moveDown(node: CategoryTreeNode) {
    const siblings = this.categories().filter(c => c.parentId === node.parentId);
    if (node.sortOrder >= siblings.length - 1) return;
    
    this.categoryService.reorderCategory(node.id, node.sortOrder + 1).subscribe();
  }

  // Bulk actions
  bulkActivate() {
    const ids = this.selectedCategories();
    if (ids.length === 0) return;
    
    this.categoryService.bulkUpdateStatus(ids, true).subscribe(() => {
      this.selectedCategories.set([]);
    });
  }
  
  bulkDeactivate() {
    const ids = this.selectedCategories();
    if (ids.length === 0) return;
    
    this.categoryService.bulkUpdateStatus(ids, false).subscribe(() => {
      this.selectedCategories.set([]);
    });
  }
  
  bulkDelete() {
    const ids = this.selectedCategories();
    if (ids.length === 0) return;
    this.alertSvc.confirm({
      title: 'Delete Categories',
      message: `Are you sure you want to delete ${ids.length} category(s)? This action cannot be undone.`,
      confirmLabel: `Delete ${ids.length}`,
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.categoryService.bulkDelete(ids).subscribe({
        next: () => { this.selectedCategories.set([]); this.alertSvc.success('Categories deleted', `${ids.length} category(s) removed.`); },
        error: () => this.alertSvc.error('Failed to delete categories')
      });
    });
  }

  // Filters
  applyFilters() {
    // Triggered by input changes
  }
  
  clearFilters() {
    this.searchQuery.set('');
    this.filterStatus.set('');
    this.sortBy.set('name');
  }
}
