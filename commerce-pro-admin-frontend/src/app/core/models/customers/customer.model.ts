export interface Customer {
  id: string;
  name: string;
  email: string;
  avatar: string;
  orders: number;
  totalSpent: number;
  tier: 'gold' | 'silver' | 'bronze' | 'platinum';
}

export interface CustomerStats {
  total: number;
  newThisWeek: number;
  growth: number;
}