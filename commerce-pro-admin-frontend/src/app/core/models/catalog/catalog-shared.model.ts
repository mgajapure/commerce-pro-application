// src/app/core/models/catalog-shared.model.ts
// Shared models for catalog operations

// Bulk Operations
export type BulkOperationType = 'edit' | 'import' | 'export' | 'copy' | 'delete';

export interface EditableField {
  id: string;
  label: string;
  icon: string;
}

export interface ExportField {
  id: string;
  label: string;
  selected: boolean;
}

export interface BulkEditValues {
  status?: string;
  category?: string;
  priceOperation?: 'set' | 'increase_percent' | 'decrease_percent' | 'increase_fixed' | 'decrease_fixed';
  priceValue?: number | null;
  stockOperation?: 'set' | 'add' | 'subtract';
  stockValue?: number | null;
  tags?: string[];
  tagOperation?: 'add' | 'remove' | 'replace';
  brand?: string;
  weight?: number | null;
  weightUnit?: 'kg' | 'g' | 'lb' | 'oz';
  description?: string;
  descriptionOperation?: 'replace' | 'append' | 'prepend' | 'find_replace';
  findText?: string;
  replaceText?: string;
}

export interface CopyOptions {
  namingPattern: 'suffix' | 'prefix' | 'custom';
  customPattern: string;
  skuPattern: 'suffix' | 'timestamp' | 'random' | 'custom';
  customSkuPattern: string;
  copyImages: boolean;
  copyVariants: boolean;
  setAsDraft: boolean;
  resetInventory: boolean;
}

export interface DeleteOptions {
  deleteImages: boolean;
  deleteVariants: boolean;
  archiveInstead: boolean;
}

export interface ExportOptions {
  includeImages: boolean;
  includeVariants: boolean;
  includeMetadata: boolean;
  useHeaders: boolean;
}

// Operation History
export interface OperationHistoryItem {
  id: string;
  type: BulkOperationType;
  title: string;
  description: string;
  icon: string;
  status: 'completed' | 'pending' | 'failed';
  timestamp: Date;
  affectedCount: number;
  duration?: number;
  canUndo: boolean;
}

// Import/Export
export type ImportMode = 'create' | 'update' | 'upsert' | 'replace';
export type ExportFormat = 'csv' | 'excel' | 'json' | 'xml';

export interface ImportPreviewRow {
  name: string;
  sku: string;
  status: 'valid' | 'error' | 'warning';
  message?: string;
}

// Product Selector
export interface ProductSelectorState {
  query: string;
  results: ProductSummary[];
  selected: ProductSummary[];
}

// Shared with product.model.ts
import { ProductSummary } from './product.model';
