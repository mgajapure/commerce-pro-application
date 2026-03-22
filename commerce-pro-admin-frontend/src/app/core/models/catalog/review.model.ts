// src/app/core/models/review.model.ts
// Review models for catalog module

export type ReviewStatus = 'pending' | 'approved' | 'rejected';
export type ReviewRating = 1 | 2 | 3 | 4 | 5;

export interface Review {
  id: string;
  productId: string;
  productName: string;
  productImage?: string;
  customerId: string;
  customerName: string;
  customerAvatar?: string;
  rating: ReviewRating;
  title?: string;
  content: string;
  status: ReviewStatus;
  isVerifiedPurchase: boolean;
  helpfulCount: number;
  unhelpfulCount: number;
  images?: string[];
  reply?: ReviewReply;
  createdAt: Date;
  updatedAt: Date;
}

export interface ReviewReply {
  id: string;
  authorId: string;
  authorName: string;
  authorRole: 'admin' | 'staff';
  content: string;
  createdAt: Date;
}

export interface ReviewStats {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
  averageRating: number;
  fiveStar: number;
  fourStar: number;
  threeStar: number;
  twoStar: number;
  oneStar: number;
}
