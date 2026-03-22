// src/app/core/models/seo.model.ts
// SEO models for catalog module

export interface SeoMetadata {
  title?: string;
  description?: string;
  keywords?: string;
  canonicalUrl?: string;
  robotsMeta?: string;
  ogTitle?: string;
  ogDescription?: string;
  ogImage?: string;
  twitterCard?: 'summary' | 'summary_large_image';
  twitterTitle?: string;
  twitterDescription?: string;
  twitterImage?: string;
}

export interface SeoTemplate {
  id: string;
  name: string;
  entityType: 'product' | 'category' | 'collection' | 'page';
  titlePattern: string;
  descriptionPattern: string;
  isDefault: boolean;
}

export interface SeoAuditResult {
  entityId: string;
  entityType: string;
  entityName: string;
  score: number;
  issues: SeoIssue[];
  suggestions: string[];
}

export interface SeoIssue {
  type: 'error' | 'warning' | 'info';
  field: string;
  message: string;
  suggestion: string;
}

export interface SeoStats {
  totalEntities: number;
  audited: number;
  averageScore: number;
  errors: number;
  warnings: number;
}
