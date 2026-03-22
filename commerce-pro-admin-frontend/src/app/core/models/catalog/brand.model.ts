// src/app/core/models/brand.model.ts
// Brand models for catalog module

export interface Brand {
  id: string;
  name: string;
  slug: string;
  description?: string;
  logo?: string;
  website?: string;
  isActive: boolean;
  isFeatured: boolean;
  productCount: number;
  sortOrder: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface BrandStats {
  total: number;
  active: number;
  featured: number;
}
