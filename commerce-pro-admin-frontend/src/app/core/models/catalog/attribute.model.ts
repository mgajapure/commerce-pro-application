// src/app/core/models/attribute.model.ts
// Attribute models for catalog module

export type AttributeType = 
  | 'select' 
  | 'multiselect' 
  | 'text' 
  | 'textarea' 
  | 'color' 
  | 'image' 
  | 'boolean' 
  | 'number' 
  | 'date';

export interface AttributeOption {
  id: string;
  label: string;
  value: string;
  sortOrder: number;
}

export interface Attribute {
  id: string;
  name: string;
  code: string;
  description?: string;
  type: AttributeType;
  options: AttributeOption[];
  isFilterable: boolean;
  isRequired: boolean;
  isVariant: boolean;
  isVisible: boolean;
  isComparable: boolean;
  isActive: boolean;
  color?: string;
  productCount: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface AttributeFormData {
  name: string;
  code: string;
  description: string;
  type: AttributeType;
  isFilterable: boolean;
  isRequired: boolean;
  isVariant: boolean;
  isVisible: boolean;
  isComparable: boolean;
  isActive: boolean;
  color: string;
  options: AttributeOption[];
}

export interface AttributeStats {
  total: number;
  active: number;
  filterable: number;
  variant: number;
  required: number;
  withOptions: number;
}
