// src/app/core/models/collection.model.ts
// Collection models for catalog module

export type CollectionType = 'manual' | 'automated';
export type CollectionConditionOperator = 'and' | 'or';
export type CollectionConditionField = 
  | 'title' 
  | 'type' 
  | 'vendor' 
  | 'price' 
  | 'tag' 
  | 'variant_title' 
  | 'sku' 
  | 'weight' 
  | 'inventory_stock' 
  | 'compare_at_price';
export type CollectionConditionRelation = 
  | 'equals' 
  | 'not_equals' 
  | 'greater_than' 
  | 'less_than' 
  | 'contains' 
  | 'not_contains' 
  | 'starts_with' 
  | 'ends_with';

export interface CollectionCondition {
  id: string;
  field: CollectionConditionField;
  relation: CollectionConditionRelation;
  value: string | number;
}

export interface Collection {
  id: string;
  name: string;
  slug: string;
  description?: string;
  image?: string;
  type: CollectionType;
  isActive: boolean;
  isFeatured: boolean;
  sortOrder: number;
  productCount: number;
  productIds?: string[]; // For manual collections
  conditions?: CollectionCondition[]; // For automated collections
  conditionOperator?: CollectionConditionOperator;
  seoTitle?: string;
  seoDescription?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface CollectionStats {
  total: number;
  manual: number;
  automated: number;
  active: number;
}
