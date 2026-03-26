import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { SupplierEvaluation, Supplier } from '../../../core/models/suppliers/suppliers.model';
import { SupplierApiService } from '../../../core/services/suppliers/suppliers.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-evaluation',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './evaluation.html'
})
export class Evaluation implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private supplierSvc = inject(SupplierApiService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  showModal = signal(false);
  isSaving = signal(false);
  isLoading = signal(true);
  searchQuery = signal('');
  filterStatus = signal('');

  evaluations = signal<SupplierEvaluation[]>([]);
  suppliers = signal<Supplier[]>([]);
  editingEval = signal<SupplierEvaluation | null>(null);
  evalForm!: FormGroup;

  statusOptions = ['DRAFT', 'SUBMITTED', 'REVIEWED'];

  displayStats = computed(() => {
    const all = this.evaluations();
    const avgScore = all.length > 0 ? all.reduce((sum, e) => sum + (e.overallScore || 0), 0) / all.length : 0;
    return [
      { label: 'Total Evaluations', value: all.length, icon: 'clipboard-data', bgColor: 'bg-blue-100', iconColor: 'text-blue-600' },
      { label: 'Draft', value: all.filter(e => e.status === 'DRAFT').length, icon: 'pencil-square', bgColor: 'bg-gray-100', iconColor: 'text-gray-600' },
      { label: 'Submitted', value: all.filter(e => e.status === 'SUBMITTED').length, icon: 'send', bgColor: 'bg-green-100', iconColor: 'text-green-600' },
      { label: 'Avg Score', value: avgScore.toFixed(1), icon: 'graph-up', bgColor: 'bg-purple-100', iconColor: 'text-purple-600' }
    ];
  });

  filteredEvaluations = computed(() => {
    let result = this.evaluations();
    const query = this.searchQuery().toLowerCase();
    if (query) { result = result.filter(e => e.supplierName?.toLowerCase().includes(query) || e.period?.toLowerCase().includes(query)); }
    if (this.filterStatus()) { result = result.filter(e => e.status === this.filterStatus()); }
    return result;
  });

  constructor() { this.initForm(); }
  ngOnInit() { this.loadData(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  private loadData() {
    this.isLoading.set(true);
    this.supplierSvc.getEvaluations().pipe(takeUntil(this.destroy$)).subscribe(res => { this.evaluations.set(res?.content || []); this.isLoading.set(false); });
    this.supplierSvc.getActiveSuppliers().pipe(takeUntil(this.destroy$)).subscribe(data => this.suppliers.set(data || []));
  }

  private initForm() {
    this.evalForm = this.fb.group({
      supplierId: ['', Validators.required],
      supplierName: [''],
      period: ['', Validators.required],
      qualityScore: [null, [Validators.required, Validators.min(0), Validators.max(5)]],
      deliveryScore: [null, [Validators.required, Validators.min(0), Validators.max(5)]],
      pricingScore: [null, [Validators.required, Validators.min(0), Validators.max(5)]],
      communicationScore: [null, [Validators.required, Validators.min(0), Validators.max(5)]],
      complianceScore: [null, [Validators.required, Validators.min(0), Validators.max(5)]],
      comments: [''],
      recommendations: ['']
    });
  }

  onSupplierChange() {
    const sid = this.evalForm.get('supplierId')?.value;
    const supplier = this.suppliers().find(s => s.id === sid);
    if (supplier) this.evalForm.patchValue({ supplierName: supplier.name });
  }

  getOverallScore(): number {
    const scores = ['qualityScore', 'deliveryScore', 'pricingScore', 'communicationScore', 'complianceScore'];
    const values = scores.map(s => this.evalForm.get(s)?.value || 0);
    return values.reduce((a, b) => a + b, 0) / 5;
  }

  openModal(evaluation?: SupplierEvaluation) {
    this.editingEval.set(evaluation || null);
    if (evaluation) { this.evalForm.patchValue(evaluation); }
    else { this.evalForm.reset(); }
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); this.editingEval.set(null); this.evalForm.reset(); }

  saveEvaluation() {
    if (this.evalForm.invalid) { this.evalForm.markAllAsTouched(); return; }
    this.isSaving.set(true);
    const val = { ...this.evalForm.value, overallScore: this.getOverallScore() };
    const action = this.editingEval()
      ? this.supplierSvc.updateEvaluation(this.editingEval()!.id, val)
      : this.supplierSvc.createEvaluation(val);
    action.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.isSaving.set(false); this.closeModal(); this.loadData(); this.alertSvc.success(this.editingEval() ? 'Evaluation updated' : 'Evaluation created'); },
      error: () => { this.isSaving.set(false); this.alertSvc.error('Failed to save'); }
    });
  }

  deleteEvaluation(evaluation: SupplierEvaluation) {
    this.alertSvc.confirm({ title: 'Delete Evaluation', message: `Delete this evaluation for "${evaluation.supplierName}"?`, confirmLabel: 'Delete', danger: true }).then(ok => {
      if (!ok) return;
      this.supplierSvc.deleteEvaluation(evaluation.id).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.loadData(); this.closeModal(); this.alertSvc.success('Evaluation deleted'); }, error: () => this.alertSvc.error('Failed to delete') });
    });
  }

  submitEvaluation(evaluation: SupplierEvaluation) {
    this.supplierSvc.submitEvaluation(evaluation.id).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.loadData(); this.alertSvc.success('Evaluation submitted'); }, error: () => this.alertSvc.error('Failed to submit') });
  }

  getStatusBadge(status: string): string {
    const map: Record<string, string> = { 'DRAFT': 'bg-gray-100 text-gray-800', 'SUBMITTED': 'bg-green-100 text-green-800', 'REVIEWED': 'bg-blue-100 text-blue-800' };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  getScoreColor(score: number): string {
    if (score >= 4) return 'text-green-600';
    if (score >= 3) return 'text-amber-600';
    return 'text-red-600';
  }

  getMenuItems(evaluation: SupplierEvaluation): DropdownItem[] {
    const items: DropdownItem[] = [{ id: 'edit', label: 'Edit', icon: 'pencil' }];
    if (evaluation.status === 'DRAFT') items.push({ id: 'submit', label: 'Submit', icon: 'send' });
    items.push({ id: 'divider', label: '', divider: true }, { id: 'delete', label: 'Delete', icon: 'trash', danger: true });
    return items;
  }

  onAction(item: DropdownItem, evaluation: SupplierEvaluation) {
    switch (item.id) {
      case 'edit': this.openModal(evaluation); break;
      case 'submit': this.submitEvaluation(evaluation); break;
      case 'delete': this.deleteEvaluation(evaluation); break;
    }
  }

  aiPerformanceAnalysis() {
    this.alertSvc.info('AI Performance Analysis', 'AI analyzes supplier evaluation trends, identifies performance patterns, and generates predictive risk assessments for supplier relationships.');
  }

  clearFilters() { this.searchQuery.set(''); this.filterStatus.set(''); }
}
