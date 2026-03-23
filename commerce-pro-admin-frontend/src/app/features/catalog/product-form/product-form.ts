// src/app/features/catalog/product-form/product-form.ts
// Product Form Component - Integrated with Spring Boot Backend API

import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil, switchMap, of, catchError, map } from 'rxjs';

// Import models from new locations
import {  
  ProductVariant,
  Product,
  ProductStatus,
  ProductDimensions
} from '../../../core/models/catalog/product.model';

import {  
  Category
} from '../../../core/models/catalog/category.model';


import { ProductRequest } from '../../../core/models/catalog/product-request.model';

// Import services
import { ProductService } from '../../../core/services/catalog/product.service';
import { CategoryService } from '../../../core/services/catalog/category.service';
import { BrandService } from '../../../core/services/catalog/brand.service';
import { CollectionService } from '../../../core/services/catalog/collection.service';
import { AttributeService } from '../../../core/services/catalog/attribute.service';
import { AlertService } from '../../../shared/services/alert.service';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiDescriptionResult, AiDemandForecastResult, AiPricingResult, AiReturnPatternResult } from '../../../core/models/ai/ai.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss'
})
export class ProductForm implements OnInit, OnDestroy {
  private fb               = inject(FormBuilder);
  private router           = inject(Router);
  private route            = inject(ActivatedRoute);
  private productService   = inject(ProductService);
  private categoryService  = inject(CategoryService);
  private brandService     = inject(BrandService);
  private collectionService= inject(CollectionService);
  private attributeService = inject(AttributeService);
  private alertSvc         = inject(AlertService);
  private aiSvc            = inject(AiService);

  // Cleanup subject
  private destroy$ = new Subject<void>();

  // AI Signals
  aiDescResult    = signal<AiDescriptionResult | null>(null);
  aiForecastResult= signal<AiDemandForecastResult | null>(null);
  aiPricingResult = signal<AiPricingResult | null>(null);
  aiReturnResult  = signal<AiReturnPatternResult | null>(null);
  aiDescLoading   = signal(false);
  aiForecastLoading = signal(false);
  aiPricingLoading  = signal(false);
  aiReturnLoading   = signal(false);
  showAiPanel     = signal(false);
  private skuCheckSubject = new Subject<string>();

  productId = signal<string | null>(null);
  isEditMode = computed(() => !!this.productId());

  productForm!: FormGroup;
  currentStep = signal(0);
  isSubmitting = signal(false);
  isLoading = signal(false);
  skuChecking = signal(false);
  skuExists = signal<boolean | null>(null);

  steps = signal([
    { id: 'basic', label: 'Basic Info' },
    { id: 'pricing', label: 'Pricing' },
    { id: 'variants', label: 'Variants' },
    { id: 'images', label: 'Images' },
    { id: 'seo', label: 'SEO' }
  ]);

  // Data from services
  categories = this.categoryService.allCategories;
  brands = computed(() => 
    this.brandService.allBrands().map(b => b.name)
  );
  collections = this.collectionService.allCollections;
  attributes = this.attributeService.allAttributes;

  selectedTags = signal<string[]>([]);
  tagInput = signal('');

  variants = signal<ProductVariant[]>([]);

  featuredImage = signal<string | null>(null);
  galleryImages = signal<string[]>([]);

  selectedCollections = signal<string[]>([]);

  ngOnInit() {
    this.initForm();
    this.setupSkuValidation();
    this.loadReferenceData();
    
    // Check for edit mode
    this.route.paramMap.subscribe(paramMap => {
      const id = paramMap.get('id');
      if (id && id !== 'new') {
        this.productId.set(id);
        this.loadProduct(id);
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm() {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', Validators.required],
      shortDescription: [''],
      category: ['', Validators.required],
      brand: ['', Validators.required],
      price: [null, [Validators.required, Validators.min(0)]],
      comparePrice: [null],
      cost: [null],
      sku: ['', [Validators.required], [this.skuAsyncValidator.bind(this)]],
      barcode: [''],
      quantity: [0, [Validators.required, Validators.min(0)]],
      lowStockThreshold: [10, [Validators.required, Validators.min(0)]],
      weight: [null],
      trackInventory: [true],
      allowBackorders: [false],
      visibility: ['visible'],
      publishDate: [''],
      status: ['draft' as ProductStatus],
      productType: ['Physical'],
      vendor: [''],
      seoTitle: [''],
      seoDescription: [''],
      urlHandle: ['', Validators.required],
      imageAlt: [''],
      // Dimensions
      dimensions: this.fb.group({
        length: [null],
        width: [null],
        height: [null]
      })
    });

    // Auto-generate URL handle from name
    this.productForm.get('name')?.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(300)
    ).subscribe(name => {
      if (name && !this.isEditMode()) {
        const handle = this.generateUrlHandle(name);
        this.productForm.patchValue({ urlHandle: handle }, { emitEvent: false });
      }
    });

    // Auto-generate SKU from name and category
    this.productForm.get('name')?.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(500)
    ).subscribe(name => {
      if (name && !this.isEditMode() && !this.productForm.get('sku')?.value) {
        this.generateSKU();
      }
    });
  }

  private setupSkuValidation() {
    // Debounced SKU uniqueness check
    this.skuCheckSubject.pipe(
      takeUntil(this.destroy$),
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(sku => {
        if (!sku || sku.length < 3) return of(false);
        this.skuChecking.set(true);
        return this.productService.checkSkuExists(sku, this.productId() || undefined).pipe(
          catchError(() => of(false)),
          map(exists => {
            this.skuChecking.set(false);
            this.skuExists.set(exists);
            return exists;
          })
        );
      })
    ).subscribe();

    // Subscribe to SKU changes
    this.productForm.get('sku')?.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(sku => {
      if (sku && sku.length >= 3) {
        this.skuCheckSubject.next(sku);
      }
    });
  }

  private skuAsyncValidator(control: AbstractControl): Promise<ValidationErrors | null> {
    return new Promise((resolve) => {
      const sku = control.value;
      if (!sku || sku.length < 3) {
        resolve(null);
        return;
      }

      this.productService.checkSkuExists(sku, this.productId() || undefined).pipe(
        takeUntil(this.destroy$),
        catchError(() => of(false))
      ).subscribe(exists => {
        this.skuExists.set(exists);
        resolve(exists ? { skuExists: true } : null);
      });
    });
  }

  private loadReferenceData() {
    // Load brands if needed
    if (this.brandService.allBrands().length === 0) {
      this.productService.getAllBrands().pipe(
        takeUntil(this.destroy$)
      ).subscribe(brands => {
        // Brands are loaded via service signals
      });
    }
  }

  private loadProduct(id: string) {
    this.isLoading.set(true);
    this.productService.getProduct(id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (product) => {
        if (product) {
          this.populateForm(product);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load product:', err);
        this.isLoading.set(false);
        this.router.navigate(['/catalog/products']);
      }
    });
  }

  private populateForm(product: Product) {
    this.productForm.patchValue({
      name: product.name,
      description: product.description,
      shortDescription: product.shortDescription,
      category: product.category,
      brand: product.brand,
      price: product.price,
      comparePrice: product.compareAtPrice,
      cost: product.cost,
      sku: product.sku,
      barcode: product.barcode,
      quantity: product.stock,
      lowStockThreshold: product.lowStockThreshold,
      weight: product.weight,
      trackInventory: product.trackInventory ?? true,
      allowBackorders: product.allowBackorders ?? false,
      visibility: product.visibility,
      status: product.status,
      productType: product.productType,
      vendor: product.vendor,
      seoTitle: product.seoTitle,
      seoDescription: product.seoDescription,
      urlHandle: product.urlHandle,
      imageAlt: product.imageAlt,
      dimensions: {
        length: product.dimensions?.length,
        width: product.dimensions?.width,
        height: product.dimensions?.height
      }
    });
    
    this.featuredImage.set(product.image);
    this.galleryImages.set(product.gallery || []);
    this.selectedTags.set(product.tags || []);
    this.variants.set(product.variants || []);
  }

  getStepClass(index: number): string {
    if (this.currentStep() === index) {
      return 'bg-indigo-600 text-white ring-4 ring-indigo-100';
    }
    if (this.isStepComplete(index)) {
      return 'bg-green-500 text-white';
    }
    return 'bg-gray-100 text-gray-500';
  }

  isStepComplete(index: number): boolean {
    return index < this.currentStep();
  }

  goToStep(index: number) {
    if (index <= this.currentStep() + 1) {
      this.currentStep.set(index);
    }
  }

  nextStep() {
    if (this.currentStep() < this.steps().length - 1) {
      // Validate current step before proceeding
      if (this.validateCurrentStep()) {
        this.currentStep.update(s => s + 1);
      }
    }
  }

  previousStep() {
    if (this.currentStep() > 0) {
      this.currentStep.update(s => s - 1);
    }
  }

  private validateCurrentStep(): boolean {
    const step = this.currentStep();
    let controlsToCheck: string[] = [];

    switch (step) {
      case 0: // Basic Info
        controlsToCheck = ['name', 'description', 'category', 'brand'];
        break;
      case 1: // Pricing
        controlsToCheck = ['price', 'sku', 'quantity'];
        break;
      case 3: // Images
        // Featured image is optional for draft
        return true;
      case 4: // SEO
        controlsToCheck = ['urlHandle'];
        break;
      default:
        return true;
    }

    let valid = true;
    controlsToCheck.forEach(controlName => {
      const control = this.productForm.get(controlName);
      if (control) {
        control.markAsTouched();
        if (control.invalid) {
          valid = false;
        }
      }
    });

    return valid;
  }

  addTag() {
    const tag = this.tagInput().trim();
    if (tag && !this.selectedTags().includes(tag)) {
      this.selectedTags.update(tags => [...tags, tag]);
      this.tagInput.set('');
    }
  }

  removeTag(tag: string) {
    this.selectedTags.update(tags => tags.filter(t => t !== tag));
  }

  addVariant() {
    const id = Math.random().toString(36).substr(2, 9);
    this.variants.update(v => [...v, { id, name: '', options: [] }]);
  }

  removeVariant(index: number) {
    this.variants.update(v => v.filter((_, i) => i !== index));
  }

  updateVariantName(index: number, name: string) {
    this.variants.update(v => {
      const updated = [...v];
      updated[index] = { ...updated[index], name };
      return updated;
    });
  }

  addVariantOption(variantIndex: number, option: string) {
    const trimmed = option.trim();
    if (trimmed && !this.variants()[variantIndex].options.includes(trimmed)) {
      this.variants.update(v => {
        const updated = [...v];
        updated[variantIndex] = {
          ...updated[variantIndex],
          options: [...updated[variantIndex].options, trimmed]
        };
        return updated;
      });
    }
  }

  removeVariantOption(variantIndex: number, option: string) {
    this.variants.update(v => {
      const updated = [...v];
      updated[variantIndex] = {
        ...updated[variantIndex],
        options: updated[variantIndex].options.filter(o => o !== option)
      };
      return updated;
    });
  }

  variantCombinations = computed(() => {
    const vars = this.variants();
    if (vars.length === 0) return [];

    const generateCombos = (index: number, current: string[]): string[] => {
      if (index === vars.length) return [current.join(' / ')];
      const combos: string[] = [];
      for (const option of vars[index].options) {
        combos.push(...generateCombos(index + 1, [...current, option]));
      }
      return combos;
    };

    return generateCombos(0, []);
  });

  onDrop(event: DragEvent, type: string) {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files && files[0]) {
      this.handleImage(files[0], type);
    }
  }

  onFileSelected(event: Event, type: string) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      this.handleImage(file, type);
    }
  }

  onGallerySelected(event: Event) {
    const files = (event.target as HTMLInputElement).files;
    if (files) {
      Array.from(files).forEach(file => {
        if (this.galleryImages().length < 8) {
          const reader = new FileReader();
          reader.onload = (e) => {
            this.galleryImages.update(imgs => [...imgs, e.target?.result as string]);
          };
          reader.readAsDataURL(file);
        }
      });
    }
  }

  handleImage(file: File, type: string) {
    // TODO: Upload to backend and get URL
    // For now, use base64 preview (replace with actual upload)
    const reader = new FileReader();
    reader.onload = (e) => {
      if (type === 'featured') {
        this.featuredImage.set(e.target?.result as string);
      }
    };
    reader.readAsDataURL(file);
    
    // TODO: Implement actual file upload
    // this.fileStorageService.uploadProductImage(file).subscribe(url => {
    //   this.featuredImage.set(url);
    // });
  }

  removeImage(type: string) {
    if (type === 'featured') {
      this.featuredImage.set(null);
    }
  }

  setAsFeatured(index: number) {
    const imgs = this.galleryImages();
    const featured = imgs[index];
    this.galleryImages.update(imgs => [featured, ...imgs.filter((_, i) => i !== index)]);
  }

  removeGalleryImage(index: number) {
    this.galleryImages.update(imgs => imgs.filter((_, i) => i !== index));
  }

  generateSKU() {
    const name = this.productForm.get('name')?.value || '';
    const category = this.productForm.get('category')?.value || '';
    const prefix = category.substring(0, 3).toUpperCase() || 'PRD';
    const random = Math.random().toString(36).substr(2, 4).toUpperCase();
    const timestamp = Date.now().toString(36).substr(-3).toUpperCase();
    this.productForm.patchValue({ sku: `${prefix}-${random}${timestamp}` });
  }

  private generateUrlHandle(name: string): string {
    return name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
  }

  adjustQuantity(delta: number) {
    const current = this.productForm.get('quantity')?.value || 0;
    const newValue = Math.max(0, current + delta);
    this.productForm.patchValue({ quantity: newValue });
  }

  showProfitPreview = computed(() => {
    const price = this.productForm.get('price')?.value;
    const cost = this.productForm.get('cost')?.value;
    return price && cost && price > cost;
  });

  calculateProfit(): number {
    const price = this.productForm.get('price')?.value || 0;
    const cost = this.productForm.get('cost')?.value || 0;
    return price - cost;
  }

  calculateMargin(): number {
    const price = this.productForm.get('price')?.value || 0;
    const profit = this.calculateProfit();
    return price > 0 ? (profit / price) * 100 : 0;
  }

  addCollection(name: string) {
    const trimmed = name.trim();
    if (trimmed && !this.selectedCollections().includes(trimmed)) {
      this.selectedCollections.update(c => [...c, trimmed]);
    }
  }

  removeCollection(collection: string) {
    this.selectedCollections.update(c => c.filter(col => col !== collection));
  }

  saveAsDraft() {
    this.productForm.patchValue({ status: 'draft' as ProductStatus });
    this.submitProduct();
  }

  previewProduct() {
    console.log('Preview:', this.productForm.value);
  }

  publishProduct() {
    if (this.productForm.valid) {
      this.productForm.patchValue({ status: 'active' as ProductStatus });
      this.submitProduct();
    } else {
      this.markAllAsTouched();
      // Show validation errors
      const invalidControls: string[] = [];
      Object.keys(this.productForm.controls).forEach(key => {
        const control = this.productForm.get(key);
        if (control?.invalid) {
          invalidControls.push(key);
        }
      });
      console.error('Invalid controls:', invalidControls);
    }
  }

  private buildProductRequest(): ProductRequest {
    const formValue = this.productForm.value;
    
    // Build dimensions only if at least one value exists
    let dimensions: ProductDimensions | undefined;
    if (formValue.dimensions?.length || formValue.dimensions?.width || formValue.dimensions?.height) {
      dimensions = {
        length: formValue.dimensions.length || 0,
        width: formValue.dimensions.width || 0,
        height: formValue.dimensions.height || 0
      };
    }

    // Ensure all required fields have proper values
    const request: ProductRequest = {
      // Required fields
      name: formValue.name?.trim() || '',
      sku: formValue.sku?.trim() || '',
      category: formValue.category || '',
      brand: formValue.brand || '',
      price: formValue.price || 0,
      stock: formValue.quantity || 0,
      lowStockThreshold: formValue.lowStockThreshold || 10,
      status: formValue.status || 'draft',
      visibility: formValue.visibility || 'visible',
      featured: false,
      trackInventory: formValue.trackInventory ?? true,
      allowBackorders: formValue.allowBackorders ?? false,
      
      // Optional fields with defaults
      description: formValue.description?.trim() || '',
      shortDescription: formValue.shortDescription?.trim() || '',
      image: this.featuredImage() || '',
      gallery: this.galleryImages() || [],
      featuredImage: this.featuredImage() || '',
      tags: this.selectedTags() || [],
      variants: (this.variants() || []).map(v => {
        // When editing (isEditMode), keep the variant ID if it exists
        // When creating new product, don't send ID (backend will generate)
        const variant: any = {
          name: v.name || '',
          options: v.options || []
        };
        
        // Keep ID if:
        // 1. We're in edit mode AND
        // 2. The variant has an ID that looks like a UUID (has dashes and is long enough)
        if (this.isEditMode() && v.id && v.id.length > 10 && v.id.includes('-')) {
          variant.id = v.id;
        }
        
        return variant;
      }),
      
      // Optional nullable fields
      compareAtPrice: formValue.comparePrice || undefined,
      cost: formValue.cost || undefined,
      weight: formValue.weight || undefined,
      dimensions: dimensions,
      vendor: formValue.vendor?.trim() || formValue.brand || '',
      productType: formValue.productType || 'Physical',
      barcode: formValue.barcode?.trim() || undefined,
      urlHandle: formValue.urlHandle?.trim() || '',
      seoTitle: formValue.seoTitle?.trim() || undefined,
      seoDescription: formValue.seoDescription?.trim() || undefined,
      imageAlt: formValue.imageAlt?.trim() || undefined
    };

    // Remove undefined values to avoid Jackson issues
    return this.cleanObject(request);
  }

  private cleanObject<T extends Record<string, any>>(obj: T): T {
    const cleaned: any = { ...obj };
    Object.keys(cleaned).forEach(key => {
      const value = cleaned[key];
      // Only remove undefined, keep null, empty arrays, etc.
      if (value === undefined) {
        delete cleaned[key];
      }
    });
    return cleaned as T;
  }

  submitProduct() {
    if (this.productForm.invalid) {
      this.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const productRequest = this.buildProductRequest();

    if (this.isEditMode()) {
      this.productService.updateProduct(this.productId()!, productRequest).pipe(
        takeUntil(this.destroy$)
      ).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.router.navigate(['/catalog/products']);
        },
        error: (err) => {
          console.error('Failed to update product:', err);
          this.isSubmitting.set(false);
          // TODO: Show error notification
        }
      });
    } else {
      this.productService.createProduct(productRequest).pipe(
        takeUntil(this.destroy$)
      ).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.router.navigate(['/catalog/products']);
        },
        error: (err) => {
          console.error('Failed to create product:', err);
          this.isSubmitting.set(false);
          // TODO: Show error notification
        }
      });
    }
  }

  markAllAsTouched() {
    Object.values(this.productForm.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        Object.values((control as any).controls).forEach((c: any) => c.markAsTouched());
      }
    });
  }

  deleteProduct() {
    if (!this.isEditMode()) return;
    this.alertSvc.confirm({
      title: 'Delete Product',
      message: 'Are you sure you want to delete this product? This action cannot be undone.',
      confirmLabel: 'Delete Product',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.productService.deleteProduct(this.productId()!).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => { this.router.navigate(['/catalog/products']); this.alertSvc.success('Product deleted'); },
        error: (err) => { console.error('Failed to delete product:', err); this.alertSvc.error('Failed to delete product', err?.message); }
      });
    });
  }

  // ─── AI Actions ───────────────────────────────────────────────────────────

  previewAiDescription(): void {
    const id = this.productId(); if (!id) return;
    this.aiDescLoading.set(true);
    this.aiSvc.previewDescription(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => { this.aiDescResult.set(r); this.aiDescLoading.set(false); },
      error: () => this.aiDescLoading.set(false)
    });
  }

  applyAiDescription(): void {
    const id = this.productId(); if (!id) return;
    this.aiDescLoading.set(true);
    this.aiSvc.generateDescription(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => {
        this.aiDescResult.set(r);
        this.productForm.patchValue({ description: r.description, shortDescription: r.shortDescription });
        this.aiDescLoading.set(false);
        this.alertSvc.success('AI description applied');
      },
      error: () => { this.alertSvc.error('Failed to generate description'); this.aiDescLoading.set(false); }
    });
  }

  runAiForecast(): void {
    const id = this.productId(); if (!id) return;
    this.aiForecastLoading.set(true);
    this.aiSvc.forecastDemand(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => { this.aiForecastResult.set(r); this.aiForecastLoading.set(false); },
      error: () => this.aiForecastLoading.set(false)
    });
  }

  runAiPricing(): void {
    const id = this.productId(); if (!id) return;
    this.aiPricingLoading.set(true);
    this.aiSvc.recommendPricing(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => { this.aiPricingResult.set(r); this.aiPricingLoading.set(false); },
      error: () => this.aiPricingLoading.set(false)
    });
  }

  runAiReturns(): void {
    const id = this.productId(); if (!id) return;
    this.aiReturnLoading.set(true);
    this.aiSvc.analyseReturnPattern(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => { this.aiReturnResult.set(r); this.aiReturnLoading.set(false); },
      error: () => this.aiReturnLoading.set(false)
    });
  }

  fmtUsd(n: number): string {
    return '$' + (n ?? 0).toFixed(2);
  }
}
