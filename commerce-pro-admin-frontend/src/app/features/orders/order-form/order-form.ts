import { Component, signal, computed, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { OrderService } from '../../../core/services/order/order.service';
import { CreateOrderRequest, UpdateOrderRequest, OrderItemRequest, OrderAddress } from '../../../core/models/order/order.model';

@Component({
  selector: 'app-order-form',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './order-form.html',
  styleUrl: './order-form.scss'
})
export class OrderForm implements OnInit, OnDestroy {
  private fb          = inject(FormBuilder);
  private route       = inject(ActivatedRoute);
  private router      = inject(Router);
  private orderService= inject(OrderService);
  private destroy$    = new Subject<void>();

  isEditMode   = signal(false);
  orderId      = signal<string | null>(null);
  isSubmitting = signal(false);
  submitError  = signal<string | null>(null);
  activeStep   = signal(0);

  steps = ['Customer', 'Items', 'Shipping', 'Review'];

  // Sample products for quick-add — in a real app this would call GET /api/products
  sampleProducts = [
    { id: 'prod_001', name: 'Wireless Bluetooth Headphones Pro', sku: 'ELEC-HP-001', price: 199.99,
      image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=60&h=60&fit=crop' },
    { id: 'prod_002', name: 'Smart Watch Series 8',              sku: 'ELEC-SW-002', price: 399.99,
      image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=60&h=60&fit=crop' },
    { id: 'prod_003', name: 'Mechanical Gaming Keyboard RGB',    sku: 'ELEC-KB-003', price: 149.99,
      image: 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?w=60&h=60&fit=crop' },
    { id: 'prod_004', name: 'Portable Bluetooth Speaker',        sku: 'ELEC-SP-005', price: 79.99,
      image: 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=60&h=60&fit=crop' },
    { id: 'prod_005', name: 'Classic Leather Backpack',          sku: 'FASH-BP-006', price: 149.99,
      image: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=60&h=60&fit=crop' }
  ];

  form: FormGroup = this.fb.group({
    // Step 0
    customerName:  ['', [Validators.required, Validators.minLength(2)]],
    customerEmail: ['', [Validators.required, Validators.email]],
    customerPhone: [''],
    customerId:    [''],
    source:        ['MANUAL'],
    customerNotes: [''],
    internalNotes: [''],
    // Step 1
    items: this.fb.array([]),
    // Step 2
    shippingAddress: this.fb.group({
      fullName:     ['', Validators.required],
      addressLine1: ['', Validators.required],
      addressLine2: [''],
      city:         ['', Validators.required],
      state:        ['', Validators.required],
      postalCode:   ['', Validators.required],
      country:      ['USA', Validators.required],
      phone:        ['']
    }),
    sameAsBilling:  [true],
    billingAddress: this.fb.group({
      fullName: [''], addressLine1: [''], addressLine2: [''],
      city: [''], state: [''], postalCode: [''], country: ['USA'], phone: ['']
    }),
    shippingMethod: ['Standard Shipping'],
    shippingCost:   [9.99, [Validators.min(0)]],
    discountAmount: [0,    [Validators.min(0)]],
    couponCode:     [''],
    currency:       ['USD']
  });

  get itemsArray(): FormArray { return this.form.get('items') as FormArray; }

  orderTotal = computed(() => {
    const subtotal = this.itemsArray.controls.reduce((sum, c) => {
      return sum + (c.get('unitPrice')?.value ?? 0) * (c.get('quantity')?.value ?? 0);
    }, 0);
    const tax      = subtotal * 0.08;
    const shipping = this.form.get('shippingCost')?.value ?? 0;
    const discount = this.form.get('discountAmount')?.value ?? 0;
    return { subtotal, tax, shipping, discount, total: subtotal + tax + shipping - discount };
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.orderId.set(id);
      this.loadForEdit(id);
    } else {
      this.addItem(); // start with one blank row
    }
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  loadForEdit(id: string): void {
    this.orderService.getOrderById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(order => {
        if (!order) return;
        this.form.patchValue({
          customerName: order.customerName, customerEmail: order.customerEmail,
          customerPhone: order.customerPhone ?? '', source: order.source,
          customerNotes: order.customerNotes ?? '', internalNotes: order.internalNotes ?? '',
          shippingMethod: order.shippingMethod ?? 'Standard Shipping',
          shippingCost: order.shippingCost, discountAmount: order.discountAmount,
          couponCode: order.couponCode ?? '', currency: order.currency,
          shippingAddress: order.shippingAddress ?? {}
        });
        // Populate items
        this.itemsArray.clear();
        order.items.forEach(item => {
          this.itemsArray.push(this.fb.group({
            productId: [item.productId], productName: [item.productName],
            sku: [item.sku], quantity: [item.quantity], unitPrice: [item.unitPrice],
            itemDiscount: [item.itemDiscount], taxRate: [item.taxRate], image: [item.productImageUrl ?? '']
          }));
        });
      });
  }

  addItem(product?: typeof this.sampleProducts[0]): void {
    this.itemsArray.push(this.fb.group({
      productId:   [product?.id ?? '',   Validators.required],
      productName: [product?.name ?? ''],
      sku:         [product?.sku ?? ''],
      quantity:    [1, [Validators.required, Validators.min(1)]],
      unitPrice:   [product?.price ?? 0, [Validators.required, Validators.min(0)]],
      itemDiscount:[0],
      taxRate:     [0.08],
      image:       [product?.image ?? '']
    }));
  }

  selectProduct(idx: number, p: typeof this.sampleProducts[0]): void {
    this.itemsArray.at(idx).patchValue({ productId: p.id, productName: p.name, sku: p.sku, unitPrice: p.price, image: p.image });
  }

  removeItem(idx: number): void { if (this.itemsArray.length > 1) this.itemsArray.removeAt(idx); }

  nextStep(): void { if (this.activeStep() < this.steps.length - 1 && this.canProceed()) this.activeStep.update(s => s + 1); }
  prevStep(): void { if (this.activeStep() > 0) this.activeStep.update(s => s - 1); }

  canProceed(): boolean {
    switch (this.activeStep()) {
      case 0: return (this.form.get('customerName')?.valid ?? false) && (this.form.get('customerEmail')?.valid ?? false);
      case 1: return this.itemsArray.length > 0 && this.itemsArray.controls.every(c => c.get('productId')?.value && c.get('quantity')?.valid);
      case 2: return this.form.get('shippingAddress')?.valid ?? false;
      default: return true;
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting()) return;
    this.isSubmitting.set(true);
    this.submitError.set(null);

    const v = this.form.value;
    const shippingAddr: OrderAddress = v.shippingAddress;
    const billingAddr: OrderAddress  = v.sameAsBilling ? shippingAddr : v.billingAddress;

    const items: OrderItemRequest[] = v.items.map((i: any) => ({
      productId: i.productId, quantity: i.quantity,
      unitPrice: i.unitPrice, itemDiscount: i.itemDiscount, taxRate: i.taxRate
    }));

    if (this.isEditMode() && this.orderId()) {
      const req: UpdateOrderRequest = {
        customerName: v.customerName, customerEmail: v.customerEmail,
        customerPhone: v.customerPhone || undefined,
        shippingAddress: shippingAddr, billingAddress: billingAddr,
        items, shippingCost: v.shippingCost, discountAmount: v.discountAmount,
        couponCode: v.couponCode || undefined, shippingMethod: v.shippingMethod,
        customerNotes: v.customerNotes || undefined, internalNotes: v.internalNotes || undefined
      };
      this.orderService.updateOrder(this.orderId()!, req)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => this.router.navigate(['/orders/all']),
          error: err => {
            this.submitError.set(err?.error?.message ?? 'Failed to update order. Please try again.');
            this.isSubmitting.set(false);
          }
        });
    } else {
      const req: CreateOrderRequest = {
        customerName: v.customerName, customerEmail: v.customerEmail,
        customerPhone: v.customerPhone || undefined, customerId: v.customerId || undefined,
        source: v.source, currency: v.currency,
        shippingAddress: shippingAddr, billingAddress: billingAddr,
        items, shippingCost: v.shippingCost, discountAmount: v.discountAmount,
        couponCode: v.couponCode || undefined, shippingMethod: v.shippingMethod,
        customerNotes: v.customerNotes || undefined, internalNotes: v.internalNotes || undefined
      };
      this.orderService.createOrder(req)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: order => this.router.navigate(['/orders/details', order.id]),
          error: err => {
            this.submitError.set(err?.error?.message ?? 'Failed to create order. Please try again.');
            this.isSubmitting.set(false);
          }
        });
    }
  }

  goBack(): void { this.router.navigate(['/orders/all']); }
}
