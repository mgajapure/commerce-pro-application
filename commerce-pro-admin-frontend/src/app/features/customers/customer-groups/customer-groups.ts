import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntil, Subject } from 'rxjs';
import { CustomerApiService } from '../../../core/services/customers/customers.service';
import { AlertService } from '../../../shared/services/alert.service';
import { CustomerGroup } from '../../../core/models/customers/customers.model';

@Component({
  selector: 'app-customer-groups',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './customer-groups.html',
  styleUrl: './customer-groups.scss'
})
export class CustomerGroups implements OnInit {
  private fb          = inject(FormBuilder);
  private customerSvc = inject(CustomerApiService);
  private alertSvc    = inject(AlertService);
  private destroy$    = new Subject<void>();

  groups      = signal<CustomerGroup[]>([]);
  isLoading   = signal(false);
  isSaving    = signal(false);
  showModal   = signal(false);
  editingGroup = signal<CustomerGroup | null>(null);
  totalElements = signal(0);
  currentPage = signal(1);
  totalPages  = signal(0);

  form!: FormGroup;

  colorOptions = [
    '#6366F1', '#8B5CF6', '#EC4899', '#EF4444', '#F59E0B',
    '#10B981', '#3B82F6', '#14B8A6', '#F97316', '#64748B'
  ];

  ngOnInit(): void {
    this.buildForm();
    this.loadGroups();
  }

  buildForm(): void {
    this.form = this.fb.group({
      name:               ['', [Validators.required, Validators.maxLength(100)]],
      description:        ['', Validators.maxLength(500)],
      colorHex:           ['#6366F1'],
      isActive:           [true],
      discountPercentage: [0, [Validators.min(0), Validators.max(100)]],
      internalNotes:      [''],
    });
  }

  loadGroups(): void {
    this.isLoading.set(true);
    this.customerSvc.getGroups({ page: this.currentPage() - 1, size: 20 })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: page => {
          this.groups.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.isLoading.set(false);
        },
        error: () => { this.isLoading.set(false); this.alertSvc.error('Failed to load groups'); }
      });
  }

  openCreate(): void {
    this.editingGroup.set(null);
    this.form.reset({ colorHex: '#6366F1', isActive: true, discountPercentage: 0 });
    this.showModal.set(true);
  }

  openEdit(g: CustomerGroup): void {
    this.editingGroup.set(g);
    this.form.patchValue({
      name: g.name, description: g.description, colorHex: g.colorHex,
      isActive: g.isActive, discountPercentage: g.discountPercentage, internalNotes: g.internalNotes
    });
    this.showModal.set(true);
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.isSaving.set(true);
    const req = this.form.value;
    const op = this.editingGroup()
      ? this.customerSvc.updateGroup(this.editingGroup()!.id, req)
      : this.customerSvc.createGroup(req);

    op.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.showModal.set(false); this.isSaving.set(false);
        this.alertSvc.success(this.editingGroup() ? 'Group updated' : 'Group created');
        this.loadGroups();
      },
      error: err => {
        this.isSaving.set(false);
        this.alertSvc.error('Save failed', err?.error?.message);
      }
    });
  }

  deleteGroup(g: CustomerGroup): void {
    this.alertSvc.confirm({
      title: 'Delete Group',
      message: `Delete "${g.name}"? This will unassign all ${g.memberCount} member(s).`,
      confirmLabel: 'Delete', danger: true
    }).then(ok => {
      if (!ok) return;
      this.customerSvc.deleteGroup(g.id).subscribe({
        next: () => { this.alertSvc.success('Group deleted'); this.loadGroups(); },
        error: err => this.alertSvc.error('Delete failed', err?.error?.message)
      });
    });
  }

  hasError(field: string, error: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.touched && c.hasError(error));
  }
}
