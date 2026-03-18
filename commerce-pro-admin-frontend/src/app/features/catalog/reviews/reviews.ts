// src/app/features/catalog/reviews/reviews.component.ts
// Product reviews management component with standard UI patterns

import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { Review, ReviewStatus, ReviewReply } from '../../../core/models/review.model';
import { ReviewService } from '../../../core/services/review.service';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-reviews',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './reviews.html',
  styleUrl: './reviews.scss'
})
export class Reviews implements OnInit {
  private fb            = inject(FormBuilder);
  private reviewService = inject(ReviewService);
  private alertSvc      = inject(AlertService);

  // View State
  showReplyModal = signal(false);
  isSaving = signal(false);
  selectedReview = signal<Review | null>(null);
  
  // Filters
  searchQuery = signal('');
  filterStatus = signal<ReviewStatus | ''>('');
  filterRating = signal<number | null>(null);
  showFilters = signal(false);

  // Data from service
  reviews = this.reviewService.allReviews;
  isLoading = this.reviewService.isLoading;
  error = this.reviewService.currentError;

  replyForm: FormGroup;

  // Status options
  statusOptions = [
    { value: 'pending', label: 'Pending', icon: 'hourglass-split', color: 'bg-yellow-100 text-yellow-800' },
    { value: 'approved', label: 'Approved', icon: 'check-circle', color: 'bg-green-100 text-green-800' },
    { value: 'rejected', label: 'Rejected', icon: 'x-circle', color: 'bg-red-100 text-red-800' }
  ];

  // Rating options
  ratingOptions = [5, 4, 3, 2, 1];

  exportItems: DropdownItem[] = [
    { id: 'csv', label: 'Export as CSV', icon: 'filetype-csv' },
    { id: 'excel', label: 'Export as Excel', icon: 'filetype-xlsx' }
  ];

  // Stats from service
  displayStats = computed(() => {
    const stats = this.reviewService.reviewStats();
    return [
      {
        label: 'Total Reviews',
        value: stats.total.toString(),
        trend: 15.3,
        icon: 'chat-square-text',
        bgColor: 'bg-blue-100',
        iconColor: 'text-blue-600',
        filter: '' as const
      },
      {
        label: 'Pending',
        value: stats.pending.toString(),
        trend: 8.2,
        icon: 'hourglass-split',
        bgColor: 'bg-yellow-100',
        iconColor: 'text-yellow-600',
        filter: 'pending' as ReviewStatus
      },
      {
        label: 'Approved',
        value: stats.approved.toString(),
        trend: 22.1,
        icon: 'check-circle',
        bgColor: 'bg-green-100',
        iconColor: 'text-green-600',
        filter: 'approved' as ReviewStatus
      },
      {
        label: 'Rejected',
        value: stats.rejected.toString(),
        trend: -5.4,
        icon: 'x-circle',
        bgColor: 'bg-red-100',
        iconColor: 'text-red-600',
        filter: 'rejected' as ReviewStatus
      },
      {
        label: 'Avg Rating',
        value: stats.averageRating.toFixed(1),
        trend: 3.2,
        icon: 'star-fill',
        bgColor: 'bg-amber-100',
        iconColor: 'text-amber-600',
        filter: '' as const
      }
    ];
  });

  // Rating distribution
  ratingDistribution = computed(() => {
    const stats = this.reviewService.reviewStats();
    const total = stats.total || 1;
    return [
      { stars: 5, count: stats.fiveStar, percentage: Math.round((stats.fiveStar / total) * 100) },
      { stars: 4, count: stats.fourStar, percentage: Math.round((stats.fourStar / total) * 100) },
      { stars: 3, count: stats.threeStar, percentage: Math.round((stats.threeStar / total) * 100) },
      { stars: 2, count: stats.twoStar, percentage: Math.round((stats.twoStar / total) * 100) },
      { stars: 1, count: stats.oneStar, percentage: Math.round((stats.oneStar / total) * 100) }
    ];
  });

  // Filtered reviews
  filteredReviews = computed(() => {
    let result = this.reviews();

    // Search
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(r =>
        r.productName.toLowerCase().includes(query) ||
        r.customerName.toLowerCase().includes(query) ||
        r.content.toLowerCase().includes(query) ||
        r.title?.toLowerCase().includes(query)
      );
    }

    // Status filter
    if (this.filterStatus()) {
      result = result.filter(r => r.status === this.filterStatus());
    }

    // Rating filter
    if (this.filterRating()) {
      result = result.filter(r => r.rating === this.filterRating());
    }

    // Sort by newest first
    result = [...result].sort((a, b) => 
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );

    return result;
  });

  // Pending reviews count
  pendingCount = computed(() => 
    this.reviews().filter(r => r.status === 'pending').length
  );

  constructor() {
    this.replyForm = this.fb.group({
      content: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  ngOnInit() {
    // Service loads data automatically
  }

  // Helpers
  getStatusIcon(status: string): string {
    const icons: Record<string, string> = {
      pending: 'hourglass-split',
      approved: 'check-circle',
      rejected: 'x-circle'
    };
    return icons[status] || 'circle';
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      pending: 'Pending',
      approved: 'Approved',
      rejected: 'Rejected'
    };
    return labels[status] || status;
  }

  getStatusColor(status: string): string {
    const colors: Record<string, string> = {
      pending: 'bg-yellow-100 text-yellow-800',
      approved: 'bg-green-100 text-green-800',
      rejected: 'bg-red-100 text-red-800'
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  }

  // Actions
  toggleFilters() {
    this.showFilters.update(v => !v);
  }

  clearFilters() {
    this.searchQuery.set('');
    this.filterStatus.set('');
    this.filterRating.set(null);
  }

  applyQuickFilter(filter: ReviewStatus | '') {
    this.clearFilters();
    this.filterStatus.set(filter);
  }

  applyRatingFilter(rating: number | null) {
    this.filterRating.set(this.filterRating() === rating ? null : rating);
  }

  approveReview(review: Review) {
    this.reviewService.approveReview(review.id).subscribe();
  }

  rejectReview(review: Review) {
    this.reviewService.rejectReview(review.id).subscribe();
  }

  deleteReview(review: Review) {
    this.alertSvc.confirm({
      title: 'Delete Review',
      message: 'Are you sure you want to delete this review? This action cannot be undone.',
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.reviewService.deleteReview(review.id).subscribe({
        next: () => this.alertSvc.success('Review deleted'),
        error: () => this.alertSvc.error('Failed to delete review')
      });
    });
  }

  // Reply functionality
  openReplyModal(review: Review) {
    this.selectedReview.set(review);
    if (review.reply) {
      this.replyForm.patchValue({ content: review.reply.content });
    } else {
      this.replyForm.reset();
    }
    this.showReplyModal.set(true);
  }

  closeReplyModal() {
    this.showReplyModal.set(false);
    this.selectedReview.set(null);
    this.replyForm.reset();
  }

  saveReply() {
    if (this.replyForm.invalid) {
      this.replyForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const content = this.replyForm.value.content;
    
    // Simulate reply saving - in real app, call service
    setTimeout(() => {
      this.isSaving.set(false);
      this.closeReplyModal();
    }, 500);
  }

  deleteReply(review: Review) {
    this.alertSvc.confirm({
      title: 'Delete Reply',
      message: 'Are you sure you want to delete your reply to this review?',
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.alertSvc.success('Reply deleted');
    });
  }

  // Helpful votes
  markHelpful(review: Review) {
    // Simulate marking helpful
  }

  // Dropdown menu
  getReviewMenuItems(review: Review): DropdownItem[] {
    const items: DropdownItem[] = [
      { id: 'view', label: 'View Product', icon: 'box' },
      { id: 'reply', label: review.reply ? 'Edit Reply' : 'Reply', icon: 'reply' }
    ];

    if (review.status === 'pending') {
      items.push({ id: 'approve', label: 'Approve', icon: 'check-circle' });
      items.push({ id: 'reject', label: 'Reject', icon: 'x-circle' });
    }

    items.push({ id: 'divider', label: '', divider: true });
    items.push({ id: 'delete', label: 'Delete Review', icon: 'trash', danger: true });

    return items;
  }

  onReviewAction(item: DropdownItem, review: Review) {
    switch (item.id) {
      case 'view':
        // Navigate to product
        break;
      case 'reply':
        this.openReplyModal(review);
        break;
      case 'approve':
        this.approveReview(review);
        break;
      case 'reject':
        this.rejectReview(review);
        break;
      case 'delete':
        this.deleteReview(review);
        break;
    }
  }

  // Export
  onExport(item: DropdownItem) {
    console.log('Exporting reviews as', item.id);
  }

  // Format date
  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  // Format relative time
  getRelativeTime(date: Date): string {
    const now = new Date();
    const reviewDate = new Date(date);
    const diffInDays = Math.floor((now.getTime() - reviewDate.getTime()) / (1000 * 60 * 60 * 24));
    
    if (diffInDays === 0) return 'Today';
    if (diffInDays === 1) return 'Yesterday';
    if (diffInDays < 7) return `${diffInDays} days ago`;
    if (diffInDays < 30) return `${Math.floor(diffInDays / 7)} weeks ago`;
    return this.formatDate(date);
  }
}
