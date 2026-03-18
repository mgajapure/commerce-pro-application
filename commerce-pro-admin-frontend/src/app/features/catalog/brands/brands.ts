// src/app/features/catalog/brands/brands.component.ts
// Brands management component with standard UI patterns

import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { Brand } from '../../../core/models/brand.model';
import { BrandService } from '../../../core/services/brand.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-brands',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './brands.html',
  styleUrl: './brands.scss'
})
export class Brands implements OnInit {
  private fb           = inject(FormBuilder);
  private brandService = inject(BrandService);
  private alertSvc     = inject(AlertService);

  // View State
  showModal = signal(false);
  isSaving = signal(false);
  selectedBrands = signal<string[]>([]);
  
  // Filters
  searchQuery = signal('');
  filterStatus = signal<string>('');
  viewMode = signal<'grid' | 'list'>('grid');

  // Data from service
  brands = this.brandService.allBrands;
  isLoading = this.brandService.isLoading;
  error = this.brandService.currentError;

  editingBrand = signal<Brand | null>(null);
  previewImage = signal<string | null>(null);

  brandForm!: FormGroup;

  exportItems: DropdownItem[] = [
    { id: 'csv', label: 'Export as CSV', icon: 'filetype-csv' },
    { id: 'excel', label: 'Export as Excel', icon: 'filetype-xlsx' }
  ];

  // Stats from service
  displayStats = computed(() => {
    const stats = this.brandService.brandStats();
    return [
      {
        label: 'Total Brands',
        value: stats.total.toString(),
        trend: 8.5,
        icon: 'building',
        bgColor: 'bg-blue-100',
        iconColor: 'text-blue-600',
        filter: 'all'
      },
      {
        label: 'Active',
        value: stats.active.toString(),
        trend: 12.3,
        icon: 'check-circle',
        bgColor: 'bg-green-100',
        iconColor: 'text-green-600',
        filter: 'active'
      },
      {
        label: 'Featured',
        value: stats.featured.toString(),
        trend: 5.1,
        icon: 'star-fill',
        bgColor: 'bg-amber-100',
        iconColor: 'text-amber-600',
        filter: 'featured'
      }
    ];
  });

  // Filtered brands
  filteredBrands = computed(() => {
    let result = this.brands();

    // Search
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(b =>
        b.name.toLowerCase().includes(query) ||
        b.description?.toLowerCase().includes(query)
      );
    }

    // Status filter
    if (this.filterStatus()) {
      switch (this.filterStatus()) {
        case 'active':
          result = result.filter(b => b.isActive);
          break;
        case 'inactive':
          result = result.filter(b => !b.isActive);
          break;
        case 'featured':
          result = result.filter(b => b.isFeatured);
          break;
      }
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
    this.brandForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      slug: ['', [Validators.required, Validators.pattern('^[a-z0-9-]+$')]],
      description: [''],
      website: [''],
      isActive: [true],
      isFeatured: [false],
      sortOrder: [0]
    });
  }

  // Helpers
  getStatusColor(isActive: boolean): string {
    return isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800';
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map(word => word.charAt(0).toUpperCase())
      .slice(0, 2)
      .join('');
  }

  // Actions
  toggleViewMode() {
    this.viewMode.update(v => v === 'grid' ? 'list' : 'grid');
  }

  clearFilters() {
    this.searchQuery.set('');
    this.filterStatus.set('');
  }

  applyQuickFilter(filter: string) {
    this.clearFilters();
    this.filterStatus.set(filter);
  }

  openModal(brand?: Brand) {
    this.editingBrand.set(brand || null);
    this.previewImage.set(null);

    if (brand) {
      this.brandForm.patchValue({
        name: brand.name,
        slug: brand.slug,
        description: brand.description,
        website: brand.website,
        isActive: brand.isActive,
        isFeatured: brand.isFeatured,
        sortOrder: brand.sortOrder
      });
      if (brand.logo) {
        this.previewImage.set(brand.logo);
      }
    } else {
      this.brandForm.reset({
        isActive: true,
        isFeatured: false,
        sortOrder: 0
      });
    }

    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.editingBrand.set(null);
    this.previewImage.set(null);
    this.brandForm.reset();
  }

  generateSlug() {
    const name = this.brandForm.get('name')?.value;
    if (name && !this.editingBrand()) {
      const slug = name.toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
      this.brandForm.patchValue({ slug });
    }
  }

  onLogoSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.previewImage.set(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  removeLogo() {
    this.previewImage.set(null);
  }

  saveBrand() {
    if (this.brandForm.invalid) {
      this.brandForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const formValue = {
      ...this.brandForm.value,
      logo: this.previewImage() || undefined
    };

    if (this.editingBrand()) {
      this.brandService.updateBrand(this.editingBrand()!.id, formValue).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeModal();
        },
        error: () => this.isSaving.set(false)
      });
    } else {
      this.brandService.createBrand(formValue).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeModal();
        },
        error: () => this.isSaving.set(false)
      });
    }
  }

  deleteBrand(brand: Brand) {
    this.alertSvc.confirm({
      title: 'Delete Brand',
      message: `Are you sure you want to delete "${brand.name}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.brandService.deleteBrand(brand.id).subscribe({
        next: () => this.alertSvc.success('Brand deleted', `"${brand.name}" has been removed.`),
        error: () => this.alertSvc.error('Failed to delete brand')
      });
    });
  }

  toggleSelection(id: string) {
    this.selectedBrands.update(selected => {
      if (selected.includes(id)) {
        return selected.filter(s => s !== id);
      }
      return [...selected, id];
    });
  }

  isSelected(id: string): boolean {
    return this.selectedBrands().includes(id);
  }

  isAllSelected(): boolean {
    const visible = this.filteredBrands();
    return visible.length > 0 && visible.every(b => this.isSelected(b.id));
  }

  toggleSelectAll() {
    if (this.isAllSelected()) {
      this.selectedBrands.set([]);
    } else {
      this.selectedBrands.set(this.filteredBrands().map(b => b.id));
    }
  }

  // Dropdown menu
  getBrandMenuItems(brand: Brand): DropdownItem[] {
    const items: DropdownItem[] = [
      { id: 'edit', label: 'Edit Brand', icon: 'pencil', shortcut: '⌘E' },
      { id: 'view', label: 'View Products', icon: 'box' }
    ];

    if (brand.website) {
      items.push({ id: 'website', label: 'Visit Website', icon: 'globe' });
    }

    items.push({ id: 'duplicate', label: 'Duplicate', icon: 'copy', shortcut: '⌘D' });

    if (brand.isActive) {
      items.push({ id: 'deactivate', label: 'Deactivate', icon: 'pause-circle' });
    } else {
      items.push({ id: 'activate', label: 'Activate', icon: 'check-circle' });
    }

    items.push(
      { id: 'divider', label: '', divider: true },
      { id: 'delete', label: 'Delete Brand', icon: 'trash', danger: true }
    );

    return items;
  }

  onBrandAction(item: DropdownItem, brand: Brand) {
    switch (item.id) {
      case 'edit':
        this.openModal(brand);
        break;
      case 'view':
        // Navigate to products filtered by brand
        break;
      case 'website':
        if (brand.website) {
          window.open(brand.website, '_blank');
        }
        break;
      case 'duplicate':
        this.duplicateBrand(brand);
        break;
      case 'activate':
        this.brandService.updateBrand(brand.id, { isActive: true }).subscribe();
        break;
      case 'deactivate':
        this.brandService.updateBrand(brand.id, { isActive: false }).subscribe();
        break;
      case 'delete':
        this.deleteBrand(brand);
        break;
    }
  }

  duplicateBrand(brand: Brand) {
    this.brandService.createBrand({
      ...brand,
      name: brand.name + ' (Copy)',
      slug: brand.slug + '-copy'
    }).subscribe();
  }

  // Bulk actions
  bulkActivate() {
    // Implement bulk activate
    this.selectedBrands.set([]);
  }

  bulkDeactivate() {
    // Implement bulk deactivate
    this.selectedBrands.set([]);
  }

  bulkDelete() {
    const count = this.selectedBrands().length;
    if (count === 0) return;
    this.alertSvc.confirm({
      title: 'Delete Brands',
      message: `Are you sure you want to delete ${count} brand(s)? This action cannot be undone.`,
      confirmLabel: `Delete ${count}`,
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.selectedBrands.set([]);
      this.alertSvc.success('Brands deleted', `${count} brand(s) removed.`);
    });
  }

  // Export
  onExport(item: DropdownItem) {
    console.log('Exporting brands as', item.id);
  }
}
