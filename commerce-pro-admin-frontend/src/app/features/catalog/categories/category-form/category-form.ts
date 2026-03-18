// src/app/features/products/components/category-form/category-form.component.ts
import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { 
  Category, 
  CategoryTemplate 
} from '../../../../core/models/catalog/category.model';
import { AlertService } from '../../../../shared/services/alert.service';

@Component({
  selector: 'app-category-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: './category-form.html',
  styleUrl: './category-form.scss'
})
export class CategoryForm implements OnInit {
  private fb       = inject(FormBuilder);
  private router   = inject(Router);
  private route    = inject(ActivatedRoute);
  private alertSvc = inject(AlertService);
  
  // State
  isEditMode = signal(false);
  categoryId = signal<string | null>(null);
  isSaving = signal(false);
  showSaveOptions = signal(false);
  showMobileNav = signal(false);
  showAdvancedSeo = signal(false);
  activeSection = signal('basic');
  previewImage = signal<string | null>(null);
  
  // Form
  categoryForm: FormGroup;
  
  // Data
  allCategories = signal<Category[]>([
    { id: '1', name: 'Electronics', slug: 'electronics', color: '#6366f1', isActive: true, isFeatured: true, sortOrder: 0, showInMenu: true, showInFooter: true, productCount: 1250, subcategories: [] },
    { id: '2', name: 'Smartphones', slug: 'smartphones', parentId: '1', color: '#8b5cf6', isActive: true, isFeatured: false, sortOrder: 0, showInMenu: true, showInFooter: false, productCount: 450, subcategories: [] },
    { id: '3', name: 'Laptops', slug: 'laptops', parentId: '1', color: '#ec4899', isActive: true, isFeatured: true, sortOrder: 1, showInMenu: true, showInFooter: false, productCount: 320, subcategories: [] },
    { id: '4', name: 'Clothing', slug: 'clothing', color: '#f59e0b', isActive: true, isFeatured: true, sortOrder: 1, showInMenu: true, showInFooter: true, productCount: 890, subcategories: [] },
    { id: '5', name: "Men's Wear", slug: 'mens-wear', parentId: '4', color: '#10b981', isActive: true, isFeatured: false, sortOrder: 0, showInMenu: true, showInFooter: false, productCount: 420, subcategories: [] },
  ]);
  
  // Templates
  categoryTemplates = signal<CategoryTemplate[]>([
    {
      id: 'electronics',
      name: 'Electronics',
      description: 'For gadgets and devices',
      icon: 'laptop',
      color: '#6366f1',
      defaultFields: { icon: 'laptop', color: '#6366f1', isFeatured: true }
    },
    {
      id: 'fashion',
      name: 'Fashion',
      description: 'For clothing and apparel',
      icon: 'shirt',
      color: '#f59e0b',
      defaultFields: { icon: 'shirt', color: '#f59e0b', isFeatured: true }
    },
    {
      id: 'home',
      name: 'Home & Garden',
      description: 'For home and living',
      icon: 'house',
      color: '#10b981',
      defaultFields: { icon: 'house', color: '#10b981', isFeatured: false }
    },
    {
      id: 'sports',
      name: 'Sports',
      description: 'For sports and fitness',
      icon: 'bicycle',
      color: '#f43f5e',
      defaultFields: { icon: 'bicycle', color: '#f43f5e', isFeatured: false }
    }
  ]);
  
  // Presets
  presetColors = signal([
    '#6366f1', '#8b5cf6', '#ec4899', '#f43f5e', '#f59e0b',
    '#10b981', '#06b6d4', '#3b82f6', '#ef4444', '#84cc16'
  ]);
  
  commonIcons = signal([
    'laptop', 'phone', 'tablet', 'camera', 'headphones',
    'house', 'sofa', 'lamp', 'flower1',
    'shirt', 'bag', 'watch', 'eyeglasses',
    'bicycle', 'ball', 'dumbbell', 'heart-pulse'
  ]);
  
  // Sections
  formSections = signal([
    { id: 'basic', label: 'Basic Information', icon: 'bi-info-circle', required: true },
    { id: 'visual', label: 'Visual Identity', icon: 'bi-palette', required: false },
    { id: 'settings', label: 'Settings', icon: 'bi-gear', required: false },
    { id: 'seo', label: 'SEO', icon: 'bi-globe', required: false }
  ]);
  
  // Computed
  availableParents = computed(() => {
    const currentId = this.categoryId();
    return this.allCategories().filter(c => c.id !== currentId);
  });
  
  categorySlug = computed(() => this.categoryForm.get('slug')?.value || '');
  
  completionPercentage = computed(() => {
    const items = this.completionItems();
    const complete = items.filter(i => i.complete).length;
    return Math.round((complete / items.length) * 100);
  });
  
  completionItems = computed(() => [
    { field: 'name', label: 'Category Name', complete: !!this.categoryForm.get('name')?.value },
    { field: 'slug', label: 'URL Slug', complete: !!this.categoryForm.get('slug')?.value },
    { field: 'description', label: 'Description', complete: !!this.categoryForm.get('description')?.value },
    { field: 'color', label: 'Category Color', complete: !!this.categoryForm.get('color')?.value },
    { field: 'seo', label: 'SEO Settings', complete: !!this.categoryForm.get('seoTitle')?.value }
  ]);

  constructor() {
    this.categoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      slug: ['', [Validators.required, Validators.pattern('^[a-z0-9-]+$')]],
      parentId: [null],
      description: ['', Validators.maxLength(500)],
      color: ['#6366f1'],
      icon: ['folder'],
      image: [''],
      imageAlt: [''],
      isActive: [true],
      isFeatured: [false],
      showInMenu: [true],
      showInFooter: [false],
      sortOrder: [0],
      seoTitle: ['', Validators.maxLength(60)],
      seoDescription: ['', Validators.maxLength(160)],
      metaKeywords: [''],
      canonicalUrl: [''],
      robotsMeta: ['']
    });
  }

  ngOnInit() {
    // Check for edit mode
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.categoryId.set(id);
      this.loadCategory(id);
    }
    
    // Set up scroll spy
    this.setupScrollSpy();
  }

  loadCategory(id: string) {
    const category = this.allCategories().find(c => c.id === id);
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
        showInMenu: category.showInMenu,
        showInFooter: category.showInFooter,
        sortOrder: category.sortOrder,
        seoTitle: category.seoTitle,
        seoDescription: category.seoDescription,
        metaKeywords: category.metaKeywords,
        canonicalUrl: category.canonicalUrl,
        robotsMeta: category.robotsMeta
      });
      this.previewImage.set(category.image || null);
    }
  }

  setupScrollSpy() {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            this.activeSection.set(entry.target.id);
          }
        });
      },
      { rootMargin: '-20% 0px -80% 0px' }
    );

    setTimeout(() => {
      document.querySelectorAll('section[id]').forEach(section => {
        observer.observe(section);
      });
    }, 100);
  }

  scrollToSection(sectionId: string) {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  onNameChange() {
    if (!this.isEditMode()) {
      this.generateSlug();
    }
  }

  generateSlug() {
    const name = this.categoryForm.get('name')?.value;
    if (name) {
      const slug = name.toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
      this.categoryForm.patchValue({ slug });
    }
  }

  getCategoryLevel(id: string): number {
    const cat = this.allCategories().find(c => c.id === id);
    if (!cat || !cat.parentId) return 0;
    return 1 + this.getCategoryLevel(cat.parentId);
  }

  applyTemplate(template: CategoryTemplate) {
    this.categoryForm.patchValue(template.defaultFields);
  }

  onFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.handleImage(file);
  }

  onImageDrop(event: DragEvent) {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) this.handleImage(file);
  }

  handleImage(file: File) {
    if (file.size > 2 * 1024 * 1024) {
      this.alertSvc.warning('Image too large', 'Please choose an image smaller than 2MB.');
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      this.previewImage.set(e.target?.result as string);
      this.categoryForm.patchValue({ image: e.target?.result });
    };
    reader.readAsDataURL(file);
  }

  removeImage() {
    this.previewImage.set(null);
    this.categoryForm.patchValue({ image: '', imageAlt: '' });
  }

  saveCategory(action: 'draft' | 'publish' | 'continue') {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      // Scroll to first error
      const firstError = document.querySelector('.border-red-300');
      if (firstError) {
        firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      return;
    }

    this.isSaving.set(true);
    this.showSaveOptions.set(false);

    const formData = {
      ...this.categoryForm.value,
      id: this.categoryId() || Math.random().toString(36).substr(2, 9),
      image: this.previewImage(),
      updatedAt: new Date(),
      createdAt: this.isEditMode() ? undefined : new Date()
    };

    // Simulate API call
    setTimeout(() => {
      console.log('Saving category:', formData);
      
      this.isSaving.set(false);
      
      switch (action) {
        case 'draft':
          this.router.navigate(['/products/categories']);
          break;
        case 'publish':
          this.router.navigate(['/products/categories']);
          break;
        case 'continue':
          // Stay on page, show success message
          break;
      }
    }, 1500);
  }

  deleteCategory() {
    const name = this.categoryForm.get('name')?.value;
    this.alertSvc.confirm({
      title: 'Delete Category',
      message: `Are you sure you want to delete "${name}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      console.log('Deleting category:', this.categoryId());
      this.router.navigate(['/products/categories']);
      this.alertSvc.success('Category deleted');
    });
  }
}