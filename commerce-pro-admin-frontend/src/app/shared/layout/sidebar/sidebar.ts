import { Component, Output, EventEmitter, signal, computed, inject, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

// Role and Permission types
type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'MANAGER' | 'STAFF' | 'VIEWER';

type Permission =
  // System & Security
  | 'manage_users'
  | 'manage_roles'
  | 'manage_permissions'
  | 'manage_integrations'
  | 'manage_flags'
  | 'view_audit_logs'
  | 'manage_security_settings'
  | 'manage_backup_restore'
  // Catalog & Products
  | 'manage_products'
  | 'manage_categories'
  | 'manage_attributes'
  | 'manage_brands'
  | 'manage_reviews'
  | 'manage_inventory'
  | 'manage_pricing'
  | 'manage_bulk_operations'
  // Orders & Fulfillment
  | 'manage_orders'
  | 'manage_order_fulfillment'
  | 'manage_returns'
  | 'manage_refunds'
  | 'manage_shipping'
  | 'manage_invoicing'
  // Customers & CRM
  | 'manage_customers'
  | 'manage_customer_segments'
  | 'manage_loyalty'
  | 'manage_support_tickets'
  | 'view_customer_analytics'
  // Marketing & Content
  | 'manage_marketing'
  | 'manage_promotions'
  | 'manage_coupons'
  | 'manage_email_campaigns'
  | 'manage_cms_content'
  | 'manage_seo'
  | 'manage_affiliates'
  // Analytics & Reporting
  | 'view_analytics'
  | 'view_sales_reports'
  | 'view_traffic_analytics'
  | 'view_conversion_analytics'
  | 'view_financial_reports'
  | 'view_inventory_reports'
  | 'create_custom_reports'
  // Multi-channel & Marketplace
  | 'manage_channels'
  | 'manage_marketplaces'
  | 'manage_pos'
  | 'manage_social_commerce'
  // Financial & Accounting
  | 'manage_payments'
  | 'manage_taxes'
  | 'manage_currency'
  | 'view_revenue'
  | 'manage_commissions'
  // Suppliers & Procurement
  | 'manage_suppliers'
  | 'manage_purchase_orders'
  | 'manage_procurement'
  // Global & Localization
  | 'manage_multi_language'
  | 'manage_multi_currency'
  | 'manage_regions'
  | 'manage_tax_rules';

interface MenuChild {
  id: string;
  label: string;
  route: string;
  permissions?: Permission[];
  badge?: number;
  icon?: string;
  description?: string;
}

interface MenuItem {
  id: string;
  label: string;
  icon: string;
  route?: string;
  badge?: number;
  roles?: UserRole[];
  permissions?: Permission[];
  children?: MenuChild[];
  divider?: boolean;
  section?: string;
  description?: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar implements OnInit {
  @Input() isOpen = true;
  @Output() close = new EventEmitter<void>();
  @Output() collapseChange = new EventEmitter<boolean>();

  private router = inject(Router);

  private _expandedMenu = signal<string | null>(null);
  private _isCollapsed = signal(false);

  expandedMenu = computed(() => this._expandedMenu());
  isCollapsed = computed(() => this._isCollapsed());
  isMobile = computed(() => window.innerWidth < 768);

  // Current user context (inject from auth service in real app)
  private currentUserRoles: UserRole[] = ['SUPER_ADMIN'];

  private currentUserPermissions: Permission[] = [
    'manage_users',
    'manage_roles',
    'manage_permissions',
    'manage_integrations',
    'manage_flags',
    'view_audit_logs',
    'manage_security_settings',
    'manage_backup_restore',
    'manage_products',
    'manage_categories',
    'manage_attributes',
    'manage_brands',
    'manage_reviews',
    'manage_inventory',
    'manage_pricing',
    'manage_bulk_operations',
    'manage_orders',
    'manage_order_fulfillment',
    'manage_returns',
    'manage_refunds',
    'manage_shipping',
    'manage_invoicing',
    'manage_customers',
    'manage_customer_segments',
    'manage_loyalty',
    'manage_support_tickets',
    'view_customer_analytics',
    'manage_marketing',
    'manage_promotions',
    'manage_coupons',
    'manage_email_campaigns',
    'manage_cms_content',
    'manage_seo',
    'manage_affiliates',
    'view_analytics',
    'view_sales_reports',
    'view_traffic_analytics',
    'view_conversion_analytics',
    'view_financial_reports',
    'view_inventory_reports',
    'create_custom_reports',
    'manage_channels',
    'manage_marketplaces',
    'manage_pos',
    'manage_social_commerce',
    'manage_payments',
    'manage_taxes',
    'manage_currency',
    'view_revenue',
    'manage_commissions',
    'manage_suppliers',
    'manage_purchase_orders',
    'manage_procurement',
    'manage_multi_language',
    'manage_multi_currency',
    'manage_regions',
    'manage_tax_rules'
  ];

  // Translation helper (replace with i18n service in real app)
  private t(key: string): string {
    const translations: Record<string, string> = {
      // Dashboard & Analytics
      'menu.dashboard': 'Dashboard',
      'menu.dashboard.executive': 'Executive Overview',
      'menu.dashboard.realtime': 'Real-time Metrics',
      'menu.analytics': 'Analytics & Reports',
      'menu.analytics.sales': 'Sales Reports',
      'menu.analytics.traffic': 'Traffic Analysis',
      'menu.analytics.conversion': 'Conversion Funnels',
      'menu.analytics.customer': 'Customer Analytics',
      'menu.analytics.product': 'Product Performance',
      'menu.analytics.financial': 'Financial Reports',
      'menu.analytics.overview':   'Analytics Overview',
      'menu.analytics.inventory':  'Inventory Reports',
      'menu.analytics.orders':     'Order Reports',
      'menu.analytics.shipping':   'Shipping Reports',
      'menu.analytics.returns':    'Returns Reports',
      'menu.analytics.saved':      'Saved Reports',
      'menu.analytics.scheduled':  'Scheduled Reports',
      'menu.analytics.history':    'Execution History',
      'menu.analytics.custom':     'Custom Reports',

      // Catalog Management
      'menu.catalog': 'Catalog',
      'menu.catalog.products': 'All Products',
      'menu.catalog.add_product': 'Add Product',
      'menu.catalog.bulk_edit': 'Bulk Operations',
      'menu.catalog.categories': 'Categories',
      'menu.catalog.attributes': 'Attributes & Specs',
      'menu.catalog.brands': 'Brands & Manufacturers',
      'menu.catalog.reviews': 'Product Reviews',
      'menu.catalog.collections': 'Collections',
      'menu.catalog.seo': 'SEO Management',

      // Inventory Management
      'menu.inventory': 'Inventory',
      'menu.inventory.overview': 'Stock Overview',
      'menu.inventory.warehouses': 'Warehouses',
      'menu.inventory.transfers': 'Stock Transfers',
      'menu.inventory.adjustments': 'Stock Adjustments',
      'menu.inventory.low_stock': 'Low Stock Alerts',
      'menu.inventory.forecasting': 'Demand Forecasting',
      'menu.inventory.valuation': 'Inventory Valuation',

      // Order Management
      'menu.orders': 'Orders',
      'menu.orders.all': 'All Orders',
      'menu.orders.pending': 'Pending Approval',
      'menu.orders.processing': 'Processing',
      'menu.orders.shipped': 'Shipped',
      'menu.orders.delivered': 'Delivered',
      'menu.orders.cancelled': 'Cancelled',
      'menu.orders.returns': 'Returns & Exchanges',
      'menu.orders.refunds': 'Refunds',
      'menu.orders.drafts': 'Draft Orders',
      'menu.orders.bulk_actions': 'Bulk Actions',

      // Fulfillment & Shipping
      'menu.fulfillment': 'Fulfillment',
      'menu.fulfillment.queue': 'Fulfillment Queue',
      'menu.fulfillment.shipments': 'Shipments',
      'menu.fulfillment.pick_pack': 'Pick & Pack',
      'menu.fulfillment.shipping_rules': 'Shipping Rules',
      'menu.fulfillment.labels': 'Shipping Labels',
      'menu.fulfillment.tracking': 'Order Tracking',

      // Customer Management
      'menu.customers': 'Customers',
      'menu.customers.all': 'All Customers',
      'menu.customers.groups': 'Customer Groups',
      'menu.customers.segments': 'Segments & Personas',
      'menu.customers.vip': 'VIP Customers',
      'menu.customers.companies': 'B2B Accounts',
      'menu.customers.reviews': 'Reviews & Ratings',
      'menu.customers.support': 'Support Tickets',
      'menu.customers.feedback': 'Feedback & NPS',
      'menu.customers.import': 'Import/Export',

      // CRM & Loyalty
      'menu.crm': 'CRM & Loyalty',
      'menu.crm.loyalty': 'Loyalty Program',
      'menu.crm.points': 'Points Management',
      'menu.crm.rewards': 'Rewards Catalog',
      'menu.crm.memberships': 'Membership Tiers',
      'menu.crm.gift_cards': 'Gift Cards',
      'menu.crm.referrals': 'Referral Program',

      // Marketing & Promotions
      'menu.marketing': 'Marketing',
      'menu.marketing.campaigns': 'Campaigns',
      'menu.marketing.promotions': 'Promotions',
      'menu.marketing.coupons': 'Coupons & Vouchers',
      'menu.marketing.email': 'Email Marketing',
      'menu.marketing.automation': 'Automation',
      'menu.marketing.affiliates': 'Affiliate Program',
      'menu.marketing.influencers': 'Influencer Management',
      'menu.marketing.retargeting': 'Retargeting',

      // Content Management
      'menu.content': 'Content',
      'menu.content.pages': 'CMS Pages',
      'menu.content.blog': 'Blog & Articles',
      'menu.content.media': 'Media Library',
      'menu.content.menus': 'Navigation Menus',
      'menu.content.themes': 'Themes & Design',
      'menu.content.widgets': 'Widgets',
      'menu.content.landing': 'Landing Pages',

      // Multi-Channel & Marketplaces
      'menu.channels': 'Channels',
      'menu.channels.overview': 'Channel Overview',
      'menu.channels.mobile_app': 'Mobile App',
      'menu.channels.social': 'Social Commerce',
      'menu.channels.marketplaces': 'Marketplaces',
      'menu.channels.pos': 'Point of Sale',
      'menu.channels.b2b_portal': 'B2B Portal',
      'menu.channels.api': 'API Channels',

      // Marketplace Integrations
      'menu.marketplaces': 'Marketplaces',
      'menu.marketplaces.amazon': 'Amazon',
      'menu.marketplaces.ebay': 'eBay',
      'menu.marketplaces.walmart': 'Walmart',
      'menu.marketplaces.etsy': 'Etsy',
      'menu.marketplaces.alibaba': 'Alibaba',
      'menu.marketplaces.sync': 'Sync Settings',

      // Financial & Accounting
      'menu.finance': 'Finance',
      'menu.finance.transactions': 'Transactions',
      'menu.finance.payouts': 'Payouts',
      'menu.finance.invoices': 'Invoices',
      'menu.finance.refunds': 'Refund Management',
      'menu.finance.reconciliation': 'Reconciliation',
      'menu.finance.taxes': 'Tax Management',
      'menu.finance.currency': 'Multi-Currency',
      'menu.finance.commissions': 'Commissions',

      // Payments
      'menu.payments': 'Payments',
      'menu.payments.methods': 'Payment Methods',
      'menu.payments.gateways': 'Payment Gateways',
      'menu.payments.fraud': 'Fraud Protection',
      'menu.payments.subscriptions': 'Subscriptions',
      'menu.payments.installments': 'Installments',

      // Suppliers & Procurement
      'menu.suppliers': 'Suppliers',
      'menu.suppliers.directory': 'Supplier Directory',
      'menu.suppliers.products': 'Supplier Products',
      'menu.suppliers.purchase_orders': 'Purchase Orders',
      'menu.suppliers.procurement': 'Procurement',
      'menu.suppliers.evaluation': 'Supplier Evaluation',
      'menu.suppliers.contracts': 'Contracts',

      // System Administration
      'menu.system': 'System',
      'menu.system.users': 'User Management',
      'menu.system.roles': 'Roles',
      'menu.system.permissions': 'Permissions',
      'menu.system.audit': 'Audit Logs',
      'menu.system.integrations': 'Integrations',
      'menu.system.api': 'API Management',
      'menu.system.webhooks': 'Webhooks',
      'menu.system.flags': 'Feature Flags',
      'menu.system.backup': 'Backup & Restore',

      // Security & Compliance
      'menu.security': 'Security',
      'menu.security.settings': 'Security Settings',
      'menu.security.2fa': 'Two-Factor Auth',
      'menu.security.sso': 'Single Sign-On',
      'menu.security.gdpr': 'GDPR & Privacy',
      'menu.security.pci': 'PCI Compliance',
      'menu.security.encryption': 'Encryption Keys',

      // Global & Localization
      'menu.global': 'Global Settings',
      'menu.global.languages': 'Languages',
      'menu.global.currencies': 'Currencies',
      'menu.global.regions': 'Regions & Zones',
      'menu.global.tax': 'Tax Rules',
      'menu.global.shipping_zones': 'Shipping Zones',
      'menu.global.localization': 'Localization',

      // Settings & Configuration
      'menu.settings': 'Settings',
      'menu.settings.general': 'General Settings',
      'menu.settings.store': 'Store Details',
      'menu.settings.checkout': 'Checkout Settings',
      'menu.settings.notifications': 'Notifications',
      'menu.settings.billing': 'Billing & Plan',

      // Help & Support
      'menu.help': 'Help & Support',
      'menu.help.documentation': 'Documentation',
      'menu.help.api_docs': 'API Documentation',
      'menu.help.support': 'Contact Support',
      'menu.help.community': 'Community',
      'menu.help.status': 'System Status'
    };
    return translations[key] || key;
  }

  // Raw menu schema with RBAC - Enterprise Grade E-commerce
  private rawMenuSchema: MenuItem[] = [
    // DASHBOARD & ANALYTICS SECTION
    {
      id: 'dashboard',
      label: 'menu.dashboard',
      icon: 'speedometer2',
      route: '/dashboard',
      roles: ['SUPER_ADMIN', 'ADMIN', 'MANAGER', 'STAFF', 'VIEWER'],
      description: 'Executive overview and real-time metrics'
    },
    {
      id: 'analytics',
      label: 'menu.analytics',
      icon: 'bar-chart-line',
      permissions: ['view_analytics'],
      description: 'Comprehensive business intelligence and reporting',
      children: [
        { id: 'analytics-overview',    label: 'menu.analytics.overview',    route: '/analytics/overview',          permissions: ['view_analytics'] },
        { id: 'analytics-sales',       label: 'menu.analytics.sales',       route: '/analytics/sales',             permissions: ['view_sales_reports'] },
        { id: 'analytics-inventory',   label: 'menu.analytics.inventory',   route: '/analytics/inventory-report',  permissions: ['view_inventory_reports'] },
        { id: 'analytics-orders',      label: 'menu.analytics.orders',      route: '/analytics/orders-report',     permissions: ['view_analytics'] },
        { id: 'analytics-customers',   label: 'menu.analytics.customer',    route: '/analytics/customers-report',  permissions: ['view_customer_analytics'] },
        { id: 'analytics-financial',   label: 'menu.analytics.financial',   route: '/analytics/financial',         permissions: ['view_financial_reports'] },
        { id: 'analytics-shipping',    label: 'menu.analytics.shipping',    route: '/analytics/shipping',          permissions: ['view_analytics'] },
        { id: 'analytics-returns',     label: 'menu.analytics.returns',     route: '/analytics/returns',           permissions: ['view_analytics'] },
        { id: 'analytics-saved',       label: 'menu.analytics.saved',       route: '/analytics/saved-reports',     permissions: ['create_custom_reports'] },
        { id: 'analytics-scheduled',   label: 'menu.analytics.scheduled',   route: '/analytics/scheduled-reports', permissions: ['create_custom_reports'] },
        { id: 'analytics-history',     label: 'menu.analytics.history',     route: '/analytics/execution-history', permissions: ['create_custom_reports'] },
        { id: 'analytics-custom',      label: 'menu.analytics.custom',      route: '/analytics/custom-reports',    permissions: ['create_custom_reports'] }
      ]
    },

    // CATALOG MANAGEMENT SECTION
    {
      id: 'catalog',
      label: 'menu.catalog',
      icon: 'boxes',
      permissions: ['manage_products'],
      description: 'Product catalog and category management',
      children: [
        { id: 'catalog-products', label: 'menu.catalog.products', route: '/catalog/products', permissions: ['manage_products'] },
        { id: 'catalog-add', label: 'menu.catalog.add_product', route: '/catalog/products/add', permissions: ['manage_products'] },
        { id: 'catalog-bulk', label: 'menu.catalog.bulk_edit', route: '/catalog/bulk-operations', permissions: ['manage_bulk_operations'] },
        { id: 'catalog-categories', label: 'menu.catalog.categories', route: '/catalog/categories', permissions: ['manage_categories'] },
        { id: 'catalog-attributes', label: 'menu.catalog.attributes', route: '/catalog/attributes', permissions: ['manage_attributes'] },
        { id: 'catalog-brands', label: 'menu.catalog.brands', route: '/catalog/brands', permissions: ['manage_brands'] },
        { id: 'catalog-reviews', label: 'menu.catalog.reviews', route: '/catalog/reviews', permissions: ['manage_reviews'] },
        { id: 'catalog-collections', label: 'menu.catalog.collections', route: '/catalog/collections', permissions: ['manage_products'] },
        { id: 'catalog-seo', label: 'menu.catalog.seo', route: '/catalog/seo', permissions: ['manage_seo'] }
      ]
    },

    // INVENTORY MANAGEMENT SECTION
    {
      id: 'inventory',
      label: 'menu.inventory',
      icon: 'box-seam',
      permissions: ['manage_inventory'],
      description: 'Stock control and warehouse management',
      children: [
        { id: 'inventory-overview', label: 'menu.inventory.overview', route: '/inventory/overview', permissions: ['manage_inventory'] },
        { id: 'inventory-warehouses', label: 'menu.inventory.warehouses', route: '/inventory/warehouses', permissions: ['manage_inventory'] },
        { id: 'inventory-transfers', label: 'menu.inventory.transfers', route: '/inventory/transfers', permissions: ['manage_inventory'] },
        { id: 'inventory-adjustments', label: 'menu.inventory.adjustments', route: '/inventory/adjustments', permissions: ['manage_inventory'] },
        { id: 'inventory-low-stock', label: 'menu.inventory.low_stock', route: '/inventory/low-stock', permissions: ['manage_inventory'] },
        { id: 'inventory-forecasting', label: 'menu.inventory.forecasting', route: '/inventory/forecasting', permissions: ['manage_inventory'] },
        { id: 'inventory-valuation', label: 'menu.inventory.valuation', route: '/inventory/valuation', permissions: ['view_inventory_reports'] }
      ]
    },

    // ORDER MANAGEMENT SECTION
    {
      id: 'orders',
      label: 'menu.orders',
      icon: 'cart-check',
      badge: 12,
      permissions: ['manage_orders'],
      description: 'Order processing and lifecycle management',
      children: [
        { id: 'orders-all', label: 'menu.orders.all', route: '/orders/all', permissions: ['manage_orders'] },
        { id: 'orders-pending', label: 'menu.orders.pending', route: '/orders/pending', badge: 8, permissions: ['manage_orders'] },
        { id: 'orders-processing', label: 'menu.orders.processing', route: '/orders/processing', badge: 3, permissions: ['manage_orders'] },
        { id: 'orders-shipped', label: 'menu.orders.shipped', route: '/orders/shipped', permissions: ['manage_orders'] },
        { id: 'orders-delivered', label: 'menu.orders.delivered', route: '/orders/delivered', permissions: ['manage_orders'] },
        { id: 'orders-cancelled', label: 'menu.orders.cancelled', route: '/orders/cancelled', permissions: ['manage_orders'] },
        { id: 'orders-returns', label: 'menu.orders.returns', route: '/orders/returns', badge: 1, permissions: ['manage_returns'] },
        { id: 'orders-refunds', label: 'menu.orders.refunds', route: '/orders/refunds', permissions: ['manage_refunds'] },
        { id: 'orders-drafts', label: 'menu.orders.drafts', route: '/orders/drafts', permissions: ['manage_orders'] },
        { id: 'orders-bulk', label: 'menu.orders.bulk_actions', route: '/orders/bulk', permissions: ['manage_orders'] }
      ]
    },

    // FULFILLMENT SECTION
    {
      id: 'fulfillment',
      label: 'menu.fulfillment',
      icon: 'truck',
      permissions: ['manage_order_fulfillment'],
      description: 'Shipping and logistics management',
      children: [
        { id: 'fulfillment-queue',    label: 'menu.fulfillment.queue',          route: '/fulfillment/queue',          permissions: ['manage_order_fulfillment'] },
        { id: 'fulfillment-shipments', label: 'menu.fulfillment.shipments',      route: '/fulfillment/shipments',      permissions: ['manage_order_fulfillment'] },
        { id: 'fulfillment-pick-pack', label: 'menu.fulfillment.pick_pack',      route: '/fulfillment/pick-pack',      permissions: ['manage_order_fulfillment'] },
        { id: 'fulfillment-rules',     label: 'menu.fulfillment.shipping_rules', route: '/fulfillment/shipping-rules', permissions: ['manage_shipping'] },
        { id: 'fulfillment-labels',    label: 'menu.fulfillment.labels',         route: '/fulfillment/labels',         permissions: ['manage_shipping'] },
        { id: 'fulfillment-tracking',  label: 'menu.fulfillment.tracking',       route: '/fulfillment/tracking',       permissions: ['manage_order_fulfillment'] }
      ]
    },

    // CUSTOMER MANAGEMENT SECTION
    {
      id: 'customers',
      label: 'menu.customers',
      icon: 'people',
      permissions: ['manage_customers'],
      description: 'Customer database and relationship management',
      children: [
        { id: 'customers-all', label: 'menu.customers.all', route: '/customers/all', permissions: ['manage_customers'] },
        { id: 'customers-groups', label: 'menu.customers.groups', route: '/customers/groups', permissions: ['manage_customers'] },
        //{ id: 'customers-segments', label: 'menu.customers.segments', route: '/customers/segments', permissions: ['manage_customer_segments'] },
        //{ id: 'customers-vip', label: 'menu.customers.vip', route: '/customers/vip', permissions: ['manage_customers'] },
        //{ id: 'customers-companies', label: 'menu.customers.companies', route: '/customers/companies', permissions: ['manage_customers'] },
        //{ id: 'customers-reviews', label: 'menu.customers.reviews', route: '/customers/reviews', permissions: ['manage_reviews'] },
        //{ id: 'customers-support', label: 'menu.customers.support', route: '/customers/support', permissions: ['manage_support_tickets'] },
        //{ id: 'customers-feedback', label: 'menu.customers.feedback', route: '/customers/feedback', permissions: ['manage_support_tickets'] },
        //{ id: 'customers-import', label: 'menu.customers.import', route: '/customers/import', permissions: ['manage_customers'] }
      ]
    },

    // CRM & LOYALTY SECTION
    {
      id: 'crm',
      label: 'menu.crm',
      icon: 'heart',
      permissions: ['manage_loyalty'],
      description: 'Loyalty programs and customer retention',
      children: [
        { id: 'crm-loyalty', label: 'menu.crm.loyalty', route: '/crm/loyalty', permissions: ['manage_loyalty'] },
        { id: 'crm-points', label: 'menu.crm.points', route: '/crm/points', permissions: ['manage_loyalty'] },
        { id: 'crm-rewards', label: 'menu.crm.rewards', route: '/crm/rewards', permissions: ['manage_loyalty'] },
        { id: 'crm-memberships', label: 'menu.crm.memberships', route: '/crm/memberships', permissions: ['manage_loyalty'] },
        { id: 'crm-gift-cards', label: 'menu.crm.gift_cards', route: '/crm/gift-cards', permissions: ['manage_loyalty'] },
        { id: 'crm-referrals', label: 'menu.crm.referrals', route: '/crm/referrals', permissions: ['manage_loyalty'] }
      ]
    },

    // MARKETING SECTION
    {
      id: 'marketing',
      label: 'menu.marketing',
      icon: 'megaphone',
      permissions: ['manage_marketing'],
      description: 'Campaigns and promotional activities',
      children: [
        { id: 'marketing-campaigns', label: 'menu.marketing.campaigns', route: '/marketing/campaigns', permissions: ['manage_marketing'] },
        { id: 'marketing-promotions', label: 'menu.marketing.promotions', route: '/marketing/promotions', permissions: ['manage_promotions'] },
        { id: 'marketing-coupons', label: 'menu.marketing.coupons', route: '/marketing/coupons', permissions: ['manage_coupons'] },
        { id: 'marketing-email', label: 'menu.marketing.email', route: '/marketing/email', permissions: ['manage_email_campaigns'] },
        { id: 'marketing-automation', label: 'menu.marketing.automation', route: '/marketing/automation', permissions: ['manage_marketing'] },
        { id: 'marketing-affiliates', label: 'menu.marketing.affiliates', route: '/marketing/affiliates', permissions: ['manage_affiliates'] },
        { id: 'marketing-influencers', label: 'menu.marketing.influencers', route: '/marketing/influencers', permissions: ['manage_marketing'] },
        { id: 'marketing-retargeting', label: 'menu.marketing.retargeting', route: '/marketing/retargeting', permissions: ['manage_marketing'] }
      ]
    },

    // CONTENT MANAGEMENT SECTION
    {
      id: 'content',
      label: 'menu.content',
      icon: 'file-earmark-text',
      permissions: ['manage_cms_content'],
      description: 'Website content and design management',
      children: [
        { id: 'content-pages', label: 'menu.content.pages', route: '/content/pages', permissions: ['manage_cms_content'] },
        { id: 'content-blog', label: 'menu.content.blog', route: '/content/blog', permissions: ['manage_cms_content'] },
        { id: 'content-media', label: 'menu.content.media', route: '/content/media', permissions: ['manage_cms_content'] },
        { id: 'content-menus', label: 'menu.content.menus', route: '/content/menus', permissions: ['manage_cms_content'] },
        { id: 'content-themes', label: 'menu.content.themes', route: '/content/themes', permissions: ['manage_cms_content'] },
        { id: 'content-widgets', label: 'menu.content.widgets', route: '/content/widgets', permissions: ['manage_cms_content'] },
        { id: 'content-landing', label: 'menu.content.landing', route: '/content/landing-pages', permissions: ['manage_cms_content'] }
      ]
    },

    // MULTI-CHANNEL SECTION
    {
      id: 'channels',
      label: 'menu.channels',
      icon: 'diagram-3',
      permissions: ['manage_channels'],
      description: 'Sales channels and omnichannel management',
      children: [
        { id: 'channels-overview', label: 'menu.channels.overview', route: '/channels/overview', permissions: ['manage_channels'] },
        { id: 'channels-mobile', label: 'menu.channels.mobile_app', route: '/channels/mobile-app', permissions: ['manage_channels'] },
        { id: 'channels-social', label: 'menu.channels.social', route: '/channels/social', permissions: ['manage_social_commerce'] },
        { id: 'channels-pos', label: 'menu.channels.pos', route: '/channels/pos', permissions: ['manage_pos'] },
        { id: 'channels-b2b', label: 'menu.channels.b2b_portal', route: '/channels/b2b', permissions: ['manage_channels'] },
        { id: 'channels-api', label: 'menu.channels.api', route: '/channels/api', permissions: ['manage_integrations'] }
      ]
    },

    // MARKETPLACES SECTION
    {
      id: 'marketplaces',
      label: 'menu.marketplaces',
      icon: 'shop',
      permissions: ['manage_marketplaces'],
      description: 'Third-party marketplace integrations',
      children: [
        { id: 'marketplaces-overview', label: 'menu.channels.marketplaces', route: '/marketplaces', permissions: ['manage_marketplaces'] },
        { id: 'marketplaces-amazon', label: 'menu.marketplaces.amazon', route: '/marketplaces/amazon', permissions: ['manage_marketplaces'] },
        { id: 'marketplaces-ebay', label: 'menu.marketplaces.ebay', route: '/marketplaces/ebay', permissions: ['manage_marketplaces'] },
        { id: 'marketplaces-walmart', label: 'menu.marketplaces.walmart', route: '/marketplaces/walmart', permissions: ['manage_marketplaces'] },
        { id: 'marketplaces-etsy', label: 'menu.marketplaces.etsy', route: '/marketplaces/etsy', permissions: ['manage_marketplaces'] },
        { id: 'marketplaces-alibaba', label: 'menu.marketplaces.alibaba', route: '/marketplaces/alibaba', permissions: ['manage_marketplaces'] },
        { id: 'marketplaces-sync', label: 'menu.marketplaces.sync', route: '/marketplaces/sync', permissions: ['manage_marketplaces'] }
      ]
    },

    // FINANCE SECTION
    {
      id: 'finance',
      label: 'menu.finance',
      icon: 'cash-stack',
      permissions: ['view_revenue'],
      description: 'Financial operations and accounting',
      children: [
        { id: 'finance-transactions', label: 'menu.finance.transactions', route: '/finance/transactions', permissions: ['view_revenue'] },
        { id: 'finance-payouts', label: 'menu.finance.payouts', route: '/finance/payouts', permissions: ['view_revenue'] },
        { id: 'finance-invoices', label: 'menu.finance.invoices', route: '/finance/invoices', permissions: ['manage_invoicing'] },
        { id: 'finance-refunds', label: 'menu.finance.refunds', route: '/finance/refunds', permissions: ['manage_refunds'] },
        { id: 'finance-reconciliation', label: 'menu.finance.reconciliation', route: '/finance/reconciliation', permissions: ['view_revenue'] },
        { id: 'finance-taxes', label: 'menu.finance.taxes', route: '/finance/taxes', permissions: ['manage_taxes'] },
        { id: 'finance-currency', label: 'menu.finance.currency', route: '/finance/currency', permissions: ['manage_currency'] },
        { id: 'finance-commissions', label: 'menu.finance.commissions', route: '/finance/commissions', permissions: ['manage_commissions'] }
      ]
    },

    // PAYMENTS SECTION
    {
      id: 'payments',
      label: 'menu.payments',
      icon: 'credit-card',
      permissions: ['manage_payments'],
      description: 'Payment processing configuration',
      children: [
        { id: 'payments-methods', label: 'menu.payments.methods', route: '/payments/methods', permissions: ['manage_payments'] },
        { id: 'payments-gateways', label: 'menu.payments.gateways', route: '/payments/gateways', permissions: ['manage_payments'] },
        { id: 'payments-fraud', label: 'menu.payments.fraud', route: '/payments/fraud', permissions: ['manage_payments'] },
        { id: 'payments-subscriptions', label: 'menu.payments.subscriptions', route: '/payments/subscriptions', permissions: ['manage_payments'] },
        { id: 'payments-installments', label: 'menu.payments.installments', route: '/payments/installments', permissions: ['manage_payments'] }
      ]
    },

    // SUPPLIERS SECTION
    {
      id: 'suppliers',
      label: 'menu.suppliers',
      icon: 'building',
      permissions: ['manage_suppliers'],
      description: 'Supplier relationship management',
      children: [
        { id: 'suppliers-directory', label: 'menu.suppliers.directory', route: '/suppliers/directory', permissions: ['manage_suppliers'] },
        { id: 'suppliers-products', label: 'menu.suppliers.products', route: '/suppliers/products', permissions: ['manage_suppliers'] },
        { id: 'suppliers-po', label: 'menu.suppliers.purchase_orders', route: '/suppliers/purchase-orders', permissions: ['manage_purchase_orders'] },
        { id: 'suppliers-procurement', label: 'menu.suppliers.procurement', route: '/suppliers/procurement', permissions: ['manage_procurement'] },
        { id: 'suppliers-evaluation', label: 'menu.suppliers.evaluation', route: '/suppliers/evaluation', permissions: ['manage_suppliers'] },
        { id: 'suppliers-contracts', label: 'menu.suppliers.contracts', route: '/suppliers/contracts', permissions: ['manage_suppliers'] }
      ]
    },

    // DIVIDER
    { id: 'divider-1', label: '', icon: '', divider: true, section: 'Administration' },

    // GLOBAL SETTINGS SECTION
    {
      id: 'global',
      label: 'menu.global',
      icon: 'globe',
      roles: ['SUPER_ADMIN', 'ADMIN'],
      description: 'Internationalization and regional settings',
      children: [
        { id: 'global-languages', label: 'menu.global.languages', route: '/global/languages', permissions: ['manage_multi_language'] },
        { id: 'global-currencies', label: 'menu.global.currencies', route: '/global/currencies', permissions: ['manage_multi_currency'] },
        { id: 'global-regions', label: 'menu.global.regions', route: '/global/regions', permissions: ['manage_regions'] },
        { id: 'global-tax', label: 'menu.global.tax', route: '/global/tax-rules', permissions: ['manage_tax_rules'] },
        { id: 'global-shipping', label: 'menu.global.shipping_zones', route: '/global/shipping-zones', permissions: ['manage_shipping'] },
        { id: 'global-localization', label: 'menu.global.localization', route: '/global/localization', permissions: ['manage_multi_language'] }
      ]
    },

    // SYSTEM ADMINISTRATION SECTION
    {
      id: 'system',
      label: 'menu.system',
      icon: 'gear',
      roles: ['SUPER_ADMIN', 'ADMIN'],
      description: 'System configuration and maintenance',
      children: [
        { id: 'system-users', label: 'menu.system.users', route: '/identity/users', permissions: ['manage_users'] },
        { id: 'system-roles', label: 'menu.system.roles', route: '/identity/roles', permissions: ['manage_roles'] },
        { id: 'system-permissions', label: 'menu.system.permissions', route: '/identity/permissions', permissions: ['manage_permissions'] },
        { id: 'system-audit', label: 'menu.system.audit', route: '/identity/audit', permissions: ['view_audit_logs'] },
        { id: 'system-config', label: 'menu.settings.general', route: '/identity/configuration', permissions: ['manage_security_settings'] },
        { id: 'system-integrations', label: 'menu.system.integrations', route: '/system/integrations', permissions: ['manage_integrations'] },
        { id: 'system-api', label: 'menu.system.api', route: '/system/api', permissions: ['manage_integrations'] },
        { id: 'system-webhooks', label: 'menu.system.webhooks', route: '/system/webhooks', permissions: ['manage_integrations'] },
        { id: 'system-flags', label: 'menu.system.flags', route: '/system/flags', permissions: ['manage_flags'] },
        { id: 'system-backup', label: 'menu.system.backup', route: '/system/backup', permissions: ['manage_backup_restore'] }
      ]
    },

    // SECURITY SECTION
    {
      id: 'security',
      label: 'menu.security',
      icon: 'shield-lock',
      roles: ['SUPER_ADMIN'],
      description: 'Security and compliance management',
      children: [
        { id: 'security-settings', label: 'menu.security.settings', route: '/security/settings', permissions: ['manage_security_settings'] },
        { id: 'security-2fa', label: 'menu.security.2fa', route: '/security/2fa', permissions: ['manage_security_settings'] },
        { id: 'security-sso', label: 'menu.security.sso', route: '/security/sso', permissions: ['manage_security_settings'] },
        { id: 'security-gdpr', label: 'menu.security.gdpr', route: '/security/gdpr', permissions: ['manage_security_settings'] },
        { id: 'security-pci', label: 'menu.security.pci', route: '/security/pci', permissions: ['manage_security_settings'] },
        { id: 'security-encryption', label: 'menu.security.encryption', route: '/security/encryption', permissions: ['manage_security_settings'] }
      ]
    },

    // SETTINGS SECTION
    {
      id: 'settings',
      label: 'menu.settings',
      icon: 'sliders',
      roles: ['SUPER_ADMIN', 'ADMIN'],
      description: 'Store and application settings',
      children: [
        { id: 'settings-general', label: 'menu.settings.general', route: '/settings/general' },
        { id: 'settings-store', label: 'menu.settings.store', route: '/settings/store' },
        { id: 'settings-checkout', label: 'menu.settings.checkout', route: '/settings/checkout' },
        { id: 'settings-notifications', label: 'menu.settings.notifications', route: '/settings/notifications' },
        { id: 'settings-billing', label: 'menu.settings.billing', route: '/settings/billing' }
      ]
    },

    // HELP & SUPPORT SECTION
    {
      id: 'help',
      label: 'menu.help',
      icon: 'question-circle',
      route: '/help',
      roles: ['SUPER_ADMIN', 'ADMIN', 'MANAGER', 'STAFF', 'VIEWER'],
      description: 'Documentation and support resources',
      children: [
        { id: 'help-docs', label: 'menu.help.documentation', route: '/help/documentation' },
        { id: 'help-api', label: 'menu.help.api_docs', route: '/help/api-docs' },
        { id: 'help-support', label: 'menu.help.support', route: '/help/support' },
        { id: 'help-community', label: 'menu.help.community', route: '/help/community' },
        { id: 'help-status', label: 'menu.help.status', route: '/help/status' }
      ]
    }
  ];

  // Filtered menu based on user permissions
  filteredMenuItems = computed(() => {
    return this.rawMenuSchema
      .filter(item => this.hasAccess(item))
      .map(item => ({
        ...item,
        label: this.t(item.label),
        children: item.children
          ?.filter(child => this.hasChildAccess(child))
          .map(child => ({
            ...child,
            label: this.t(child.label)
          }))
      }));
  });

  // Separate system items for visual grouping
  mainMenuItems = computed(() =>
    this.filteredMenuItems().filter(item =>
      !['system', 'security', 'global', 'settings', 'help'].includes(item.id) && !item.divider
    )
  );

  systemMenuItems = computed(() =>
    this.filteredMenuItems().filter(item =>
      ['system', 'security', 'global', 'settings'].includes(item.id)
    )
  );

  helpMenuItems = computed(() =>
    this.filteredMenuItems().filter(item => item.id === 'help')
  );

  dividerItems = computed(() =>
    this.filteredMenuItems().filter(item => item.divider)
  );

  isMenuExpanded = (id: string) => this.expandedMenu() === id;

  ngOnInit(): void {
    // Auto-expand menu based on current route
    this.expandForRoute(this.router.url);

    // Listen to route changes to update active menu and auto-expand
    this.router.events.subscribe(() => {
      this.expandForRoute(this.router.url);
    });
    // // Close drawer on route change (for mobile)
    this.router.events.subscribe(() => {
      if (this.isOpen) {
        this.closeDrawer();
      }
    });
  }

  toggleCollapse() {
    this._isCollapsed.update(v => !v);
    this.collapseChange.emit(this._isCollapsed());
  }

  // Check if route is currently active
  isActive(route: string | undefined): boolean {
    if (!route) return false;
    return this.router.url === route || this.router.url.startsWith(route + '/');
  }

  private hasAccess(item: MenuItem): boolean {
    // Check roles if specified
    if (item.roles && !item.roles.some(role => this.currentUserRoles.includes(role))) {
      return false;
    }
    // Check permissions if specified
    if (item.permissions && !item.permissions.some(perm => this.currentUserPermissions.includes(perm))) {
      return false;
    }
    return true;
  }

  private hasChildAccess(child: MenuChild): boolean {
    if (child.permissions && !child.permissions.some(perm => this.currentUserPermissions.includes(perm))) {
      return false;
    }
    return true;
  }

  toggleSubmenu(id: string) {
    this._expandedMenu.update(current => current === id ? null : id);
  }

  open() {
    this.isOpen = true;
    document.body.style.overflow = 'hidden';
  }

  closeDrawer() {
    this.isOpen = false;
    document.body.style.overflow = '';
    this.close.emit();
  }

  // Auto-expand based on current route
  expandForRoute(route: string) {
    const parent = this.rawMenuSchema.find(item =>
      item.children?.some(child => child.route === route)
    );
    if (parent) {
      this._expandedMenu.set(parent.id);
    }
  }

  // Check if any child is active (for highlighting parent)
  hasActiveChild(item: MenuItem): boolean {
    if (!item.children) return false;
    return item.children.some(child => this.isActive(child.route));
  }

  // Get badge count for menu item (example implementation)
  getBadgeCount(itemId: string): number | undefined {
    // This would typically come from a service
    const badgeMap: Record<string, number> = {
      'orders': 12,
      'orders-pending': 8,
      'orders-processing': 3,
      'orders-shipped': 1,
      'orders-returns': 1
    };
    return badgeMap[itemId];
  }
}