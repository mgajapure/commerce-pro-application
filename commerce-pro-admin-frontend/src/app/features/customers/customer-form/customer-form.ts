import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntil, Subject } from 'rxjs';
import { CustomerApiService } from '../../../core/services/customers/customers.service';
import { AlertService } from '../../../shared/services/alert.service';
import { CustomerGroup } from '../../../core/models/customers/customers.model';

@Component({
  selector: 'app-customer-form',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './customer-form.html',
  styleUrl: './customer-form.scss'
})
export class CustomerForm implements OnInit {
  private fb          = inject(FormBuilder);
  private route       = inject(ActivatedRoute);
  private router      = inject(Router);
  private customerSvc = inject(CustomerApiService);
  private alertSvc    = inject(AlertService);
  private destroy$    = new Subject<void>();

  editId      = signal<string | null>(null);
  isLoading   = signal(false);
  isSaving    = signal(false);
  groups      = signal<CustomerGroup[]>([]);
  activeTab   = signal<'basic' | 'preferences' | 'notes'>('basic');

  form!: FormGroup;

  tabs = [
    { id: 'basic',       label: 'Basic Info',   icon: 'person' },
    { id: 'preferences', label: 'Preferences',  icon: 'gear' },
    { id: 'notes',       label: 'Notes',        icon: 'file-text' },
  ];

  ngOnInit(): void {
    this.buildForm();
    this.loadGroups();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId.set(id);
      this.loadCustomer(id);
    }
  }

  buildForm(): void {
    this.form = this.fb.group({
      firstName:         ['', [Validators.required, Validators.maxLength(100)]],
      lastName:          ['', [Validators.required, Validators.maxLength(100)]],
      email:             ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      phone:             ['', Validators.maxLength(30)],
      secondaryPhone:    ['', Validators.maxLength(30)],
      dateOfBirth:       [''],
      gender:            [''],
      avatarUrl:         [''],
      companyName:       ['', Validators.maxLength(100)],
      taxId:             ['', Validators.maxLength(50)],
      preferredCurrency: ['USD'],
      preferredLanguage: ['en'],
      marketingOptIn:    [false],
      smsOptIn:          [false],
      acquisitionSource: [''],
      groupId:           [''],
      internalNotes:     [''],
      linkedUserId:      [''],
    });
  }

  loadGroups(): void {
    this.customerSvc.getActiveGroups().subscribe(g => this.groups.set(g));
  }

  loadCustomer(id: string): void {
    this.isLoading.set(true);
    this.customerSvc.getCustomerById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: c => {
        this.form.patchValue({
          firstName: c.firstName, lastName: c.lastName,
          email: c.email, phone: c.phone, secondaryPhone: c.secondaryPhone,
          dateOfBirth: c.dateOfBirth ? c.dateOfBirth.split('T')[0] : '',
          gender: c.gender, avatarUrl: c.avatarUrl,
          companyName: c.companyName, taxId: c.taxId,
          preferredCurrency: c.preferredCurrency, preferredLanguage: c.preferredLanguage,
          marketingOptIn: c.marketingOptIn, smsOptIn: c.smsOptIn,
          acquisitionSource: c.acquisitionSource, groupId: c.groupId ?? '',
          internalNotes: c.internalNotes, linkedUserId: c.linkedUserId
        });
        this.isLoading.set(false);
      },
      error: () => {
        this.alertSvc.error('Customer not found');
        this.router.navigate(['/customers/all']);
      }
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alertSvc.warning('Validation error', 'Please fix the errors before saving.');
      return;
    }

    this.isSaving.set(true);
    const value = this.form.value;
    const req = {
      ...value,
      groupId: value.groupId || null,
      linkedUserId: value.linkedUserId || null,
      dateOfBirth: value.dateOfBirth || null
    };

    const op = this.editId()
      ? this.customerSvc.updateCustomer(this.editId()!, req)
      : this.customerSvc.createCustomer(req);

    op.pipe(takeUntil(this.destroy$)).subscribe({
      next: customer => {
        this.alertSvc.success(
          this.editId() ? 'Customer updated' : 'Customer created',
          `"${customer.fullName}" has been saved.`
        );
        this.router.navigate(['/customers/detail', customer.id]);
      },
      error: err => {
        this.isSaving.set(false);
        this.alertSvc.error('Save failed', err?.error?.message ?? 'Please try again.');
      }
    });
  }

  cancel(): void {
    if (this.editId()) this.router.navigate(['/customers/detail', this.editId()]);
    else this.router.navigate(['/customers/all']);
  }

  hasError(field: string, error: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.touched && c.hasError(error));
  }
}
