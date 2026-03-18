# E-Commerce Admin Dashboard - UI/UX Guidelines & Standards

## Application Overview

**Project Type:** Angular 21+ Standalone Components E-Commerce Admin Dashboard  
**Architecture:** Signal-based state management, JSON data loading (API-ready), Lazy loading  
**UI Framework:** Tailwind CSS + Bootstrap Icons  
**Charting:** Chart.js for data visualization  

---

## 1. Core Requirements Understanding

### 1.1 Business Context
- **Multi-module e-commerce admin panel** with Catalog, Inventory, Orders, Customers, Dashboard
- **API-ready architecture** - All services load from JSON files but structured for easy Spring Boot migration
- **Real-time feel** with signals, loading states, and smooth transitions
- **Professional, modern SaaS aesthetic** similar to Shopify, Stripe, or Vercel dashboards

### 1.2 Key User Needs
- Quick overview of business metrics (KPIs, charts, alerts)
- Efficient product/catalog management with bulk operations
- Inventory tracking across multiple warehouses
- Order lifecycle management (pending → processing → shipped → delivered)
- Customer insights and segmentation

---

## 2. Component Architecture Standards

### 2.1 Component Structure
```typescript
@Component({
  selector: 'app-component-name',  // Prefix with 'app-'
  standalone: true,                // Always standalone
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, Dropdown],
  templateUrl: './component-name.html',
  styleUrl: './component-name.scss'   // Note: singular 'styleUrl' (not 'Urls')
})
export class ComponentName implements OnInit {
  // Services - always inject at top
  private service = inject(ServiceName);
  private fb = inject(FormBuilder);
  
  // Signals for state
  isLoading = signal(false);
  showModal = signal(false);
  selectedItems = signal<string[]>([]);
  
  // Computed values
  filteredData = computed(() => { ... });
  displayStats = computed(() => { ... });
  
  // Data from service
  items = this.service.allItems;
  
  // Dropdown/menu items
  exportItems: DropdownItem[] = [
    { id: 'csv', label: 'Export as CSV', icon: 'filetype-csv' },
    { id: 'excel', label: 'Export as Excel', icon: 'filetype-xlsx' }
  ];
}
```

### 2.2 Service Pattern (API-Ready)
```typescript
@Injectable({ providedIn: 'root' })
export class ServiceName {
  // Base URL - easily switchable
  private readonly BASE_URL = 'assets/data/catalog';  // For JSON
  // private readonly BASE_URL = '/api/v1/catalog';   // For Spring Boot
  
  // Private signals
  private items = signal<Item[]>([]);
  private loading = signal(false);
  
  // Public computed signals
  readonly allItems = computed(() => this.items());
  readonly isLoading = computed(() => this.loading());
  
  constructor(private http: HttpClient) {
    this.loadData();
  }
  
  // CRUD operations with Spring Boot comments
  createItem(item: Partial<Item>): Observable<Item> { ... }
  updateItem(id: string, updates: Partial<Item>): Observable<Item> { ... }
  deleteItem(id: string): Observable<void> { ... }
}
```

---

## 3. HTML Template Standards

### 3.1 Container Structure
```html
<!-- Main container -->
<div class="min-h-screen bg-gray-50/50 p-4 sm:p-6 lg:p-8">
    <div class="max-w-[1600px] mx-auto space-y-6">
        <!-- Content here -->
    </div>
</div>

<!-- Card container -->
<div class="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
    <!-- Content -->
</div>

<!-- For equal height cards in grid -->
<div class="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm h-full flex flex-col">
    <!-- Content with flex-1 for stretchable areas -->
</div>
```

### 3.2 Header Pattern (Consistent across all pages)
```html
<!-- Header Section -->
<div class="flex flex-col lg:flex-row lg:items-center justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
    <div>
        <div class="flex items-center gap-3">
            <h1 class="text-2xl font-bold text-gray-900">Page Title</h1>
            <span class="px-3 py-1 bg-gray-100 text-gray-600 text-sm font-medium rounded-full">
                {{ filteredCount() }} items
            </span>
        </div>
        <p class="text-gray-500 mt-1 text-sm">Page description</p>
    </div>
    <div class="flex flex-wrap items-center gap-3">
        <!-- Filter/Search buttons -->
        <button (click)="toggleFilters()" class="...">Filters</button>
        
        <!-- Export dropdown -->
        <app-dropdown label="Export" [items]="exportItems" [showChevron]="true"></app-dropdown>
        
        <!-- Primary action -->
        <button (click)="openModal()" class="px-4 py-2.5 bg-indigo-600 text-white rounded-xl ...">
            <i class="bi bi-plus-lg"></i>
            <span>Add Item</span>
        </button>
    </div>
</div>
```

### 3.3 Stats Cards Pattern
```html
<!-- Standard 4-column stats -->
<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
    @for (stat of displayStats(); track stat.label) {
    <div class="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-all">
        <div class="flex items-start justify-between">
            <div>
                <p class="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">{{ stat.label }}</p>
                <p class="text-2xl font-bold text-gray-900">{{ stat.value }}</p>
            </div>
            <div class="w-10 h-10 rounded-xl flex items-center justify-center {{ stat.bgColor }}">
                <i class="bi bi-{{stat.icon}} {{stat.iconColor}} text-lg"></i>
            </div>
        </div>
        <div class="mt-3 flex items-center gap-2">
            <span class="text-xs font-medium px-2 py-0.5 rounded-full"
                [class]="stat.trend >= 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'">
                {{ stat.trend >= 0 ? '+' : '' }}{{ stat.trend }}%
            </span>
            <span class="text-xs text-gray-400">vs last month</span>
        </div>
    </div>
    }
</div>
```

### 3.4 KPI Cards (Dashboard) - Gradient Style
```html
<div class="relative overflow-hidden rounded-2xl p-6 shadow-lg hover:shadow-2xl transition-all duration-300 group cursor-pointer transform hover:-translate-y-1"
     [class]="'bg-gradient-to-br ' + kpi.gradient">
    <!-- Background Pattern -->
    <div class="absolute inset-0">
        <div class="absolute -right-6 -top-6 w-32 h-32 rounded-full bg-white/20 blur-2xl"></div>
        <div class="absolute -left-6 -bottom-6 w-24 h-24 rounded-full bg-white/10 blur-xl"></div>
    </div>
    
    <!-- Content -->
    <div class="relative z-10">
        <div class="flex items-start justify-between mb-4">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center bg-white/25 backdrop-blur-md border border-white/30 shadow-lg">
                <i class="bi bi-{{kpi.icon}} text-2xl text-white drop-shadow-md"></i>
            </div>
            <span class="flex items-center gap-1 text-xs font-bold px-3 py-1.5 rounded-full bg-white/25 backdrop-blur-sm border border-white/30 text-white shadow-lg">
                <i class="bi bi-{{kpi.growth >= 0 ? 'arrow-up-short' : 'arrow-down-short'}} text-sm"></i>
                {{ kpi.growth > 0 ? '+' : '' }}{{ kpi.growth }}%
            </span>
        </div>
        
        <div class="mb-3">
            <p class="text-white/90 text-sm font-semibold mb-1 uppercase tracking-wide">{{ kpi.title }}</p>
            <h3 class="text-3xl font-extrabold text-white tracking-tight drop-shadow-lg">{{ kpi.value }}</h3>
        </div>
    </div>
</div>
```

---

## 4. UI Design Guidelines (Section by Section)

### 4.1 Color Palette
| Purpose | Color | Tailwind Class |
|---------|-------|----------------|
| Primary | Indigo | `bg-indigo-600`, `text-indigo-600` |
| Success | Green | `bg-green-100`, `text-green-600` |
| Warning | Yellow/Amber | `bg-yellow-100`, `text-yellow-600` |
| Danger | Red | `bg-red-100`, `text-red-600` |
| Info | Blue | `bg-blue-100`, `text-blue-600` |
| Background | Gray 50 | `bg-gray-50/50` |
| Cards | White | `bg-white` |
| Borders | Gray 100 | `border-gray-100` |
| Text Primary | Gray 900 | `text-gray-900` |
| Text Secondary | Gray 500 | `text-gray-500` |

### 4.2 Border Radius Standard
- **Cards**: `rounded-2xl` (16px)
- **Buttons**: `rounded-xl` (12px) for large, `rounded-lg` (8px) for small
- **Inputs**: `rounded-xl` (12px)
- **Badges**: `rounded-full`
- **Icons containers**: `rounded-xl` or `rounded-2xl`

### 4.3 Shadow Standard
- **Cards**: `shadow-sm` default, `hover:shadow-md` on hover
- **Modals**: `shadow-2xl`
- **Buttons (primary)**: `shadow-lg shadow-indigo-200`
- **Dropdowns**: `shadow-lg`

### 4.4 Spacing Standard
- **Page padding**: `p-4 sm:p-6 lg:p-8`
- **Card padding**: `p-6`
- **Section gaps**: `space-y-6` or `gap-6`
- **Inner card gaps**: `space-y-4` or `gap-4`
- **Element gaps**: `gap-2` or `gap-3`

### 4.5 Typography Scale
| Element | Size | Weight |
|---------|------|--------|
| Page Title | `text-2xl` | `font-bold` |
| Card Title | `text-lg` | `font-semibold` |
| Section Label | `text-sm` | `font-medium` |
| Body Text | `text-sm` | `font-normal` |
| Small/Helper | `text-xs` | `font-normal` |
| Stats Value | `text-2xl` | `font-bold` |

---

## 5. Component-Specific Patterns

### 5.1 Tables (Default View)
```html
<div class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
    <div class="overflow-x-auto">
        <table class="w-full">
            <thead class="bg-gray-50/50 border-b border-gray-100">
                <tr>
                    <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                        Column Name
                    </th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @for (item of items(); track item.id) {
                <tr class="group hover:bg-gray-50/50 transition-colors">
                    <td class="px-6 py-4">{{ item.name }}</td>
                </tr>
                }
            </tbody>
        </table>
    </div>
</div>
```

### 5.2 Grid View (Alternative)
```html
<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
    @for (item of items(); track item.id) {
    <div class="group bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-lg transition-all overflow-hidden">
        <!-- Card content -->
    </div>
    }
</div>
```

### 5.3 Modal Pattern
```html
@if (showModal()) {
<div class="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden animate-slide-up">
        <!-- Header -->
        <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
            <h2 class="text-lg font-semibold text-gray-900">Modal Title</h2>
            <button (click)="closeModal()" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg">
                <i class="bi bi-x-lg text-xl"></i>
            </button>
        </div>
        
        <!-- Body -->
        <div class="p-6 overflow-y-auto max-h-[60vh]">
            <!-- Form or content -->
        </div>
        
        <!-- Footer -->
        <div class="px-6 py-4 border-t border-gray-100 flex items-center justify-between bg-gray-50/50">
            <button (click)="closeModal()" class="text-gray-700">Cancel</button>
            <button (click)="save()" class="bg-indigo-600 text-white px-6 py-2.5 rounded-xl">Save</button>
        </div>
    </div>
</div>
}
```

### 5.4 Bulk Actions Bar
```html
@if (selectedItems().length > 0) {
<div class="bg-indigo-600 text-white rounded-2xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 animate-slide-up shadow-lg shadow-indigo-200">
    <div class="flex items-center gap-3">
        <div class="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center">
            <i class="bi bi-check-square text-xl"></i>
        </div>
        <div>
            <p class="font-semibold">{{ selectedItems().length }} items selected</p>
        </div>
    </div>
    <div class="flex flex-wrap gap-2">
        <button (click)="bulkAction()" class="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-lg">Action</button>
        <button (click)="bulkDelete()" class="px-4 py-2 bg-red-500 hover:bg-red-600 rounded-lg">Delete</button>
    </div>
</div>
}
```

### 5.5 Tabs Pattern (Like Bulk Operations)
```html
<div class="bg-white rounded-2xl p-2 shadow-sm border border-gray-100">
    <div class="flex flex-wrap gap-2">
        @for (tab of tabs; track tab.id) {
        <button (click)="setTab(tab.id)"
            class="flex-1 min-w-[140px] px-4 py-3 rounded-xl text-sm font-medium transition-all flex items-center justify-center gap-2"
            [class.bg-indigo-600]="activeTab() === tab.id"
            [class.text-white]="activeTab() === tab.id"
            [class.shadow-md]="activeTab() === tab.id"
            [class.bg-gray-50]="activeTab() !== tab.id"
            [class.text-gray-700]="activeTab() !== tab.id">
            <i class="bi bi-{{tab.icon}}"></i>
            {{ tab.label }}
        </button>
        }
    </div>
</div>
```

---

## 6. State Management Patterns

### 6.1 Loading States
```html
@if (isLoading()) {
    <!-- Skeleton loading -->
    <div class="animate-pulse">...</div>
} @else {
    <!-- Actual content -->
}
```

### 6.2 Empty States
```html
@empty {
    <div class="col-span-full py-12 text-center">
        <div class="w-16 h-16 bg-gray-100 rounded-2xl flex items-center justify-center mb-4">
            <i class="bi bi-box text-gray-400 text-2xl"></i>
        </div>
        <h3 class="text-lg font-medium text-gray-900 mb-1">No items found</h3>
        <p class="text-gray-500 mb-4">Try adjusting your filters</p>
        <button (click)="clearFilters()" class="px-4 py-2 bg-indigo-600 text-white rounded-lg">
            Clear Filters
        </button>
    </div>
}
```

### 6.3 Error States
```html
@if (hasError()) {
    <div class="text-center">
        <div class="w-16 h-16 bg-red-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <i class="bi bi-exclamation-triangle text-2xl text-red-600"></i>
        </div>
        <p class="text-gray-600 font-medium mb-1">Failed to load data</p>
        <button (click)="retry()" class="px-4 py-2 bg-indigo-600 text-white rounded-lg">
            <i class="bi bi-arrow-clockwise mr-2"></i>Retry
        </button>
    </div>
}
```

---

## 7. Responsive Breakpoints

| Breakpoint | Grid Columns | Notes |
|------------|--------------|-------|
| Default (mobile) | 1 column | Stack everything |
| `sm` (640px) | 2 columns | Stats cards, simple grids |
| `md` (768px) | 2-3 columns | Tables visible, sidebars |
| `lg` (1024px) | 3-4 columns | Full sidebar, complex layouts |
| `xl` (1280px) | 4+ columns | Maximum content width 1600px |

---

## 8. Animation Standards

### 8.1 Included Animations (from styles.scss)
- `animate-fade-in` - Fade in effect
- `animate-slide-up` - Slide up with fade
- `animate-slide-down` - Slide down with fade
- `animate-pulse` - Skeleton loading pulse

### 8.2 Hover Transitions
```css
hover:shadow-lg transition-all duration-300
transform hover:-translate-y-1  /* Lift effect on cards */
```

### 8.3 Page Elements
- **Modals**: `animate-slide-up` on open
- **Dropdowns**: `animate-slide-down` on open
- **Bulk actions**: `animate-slide-up` when appearing
- **Skeletons**: `animate-pulse`

---

## 9. File Structure Conventions

```
src/app/features/feature-name/
├── component-name/
│   ├── component-name.ts      # Component logic
│   ├── component-name.html    # Template
│   ├── component-name.scss    # Styles (if needed)
│   └── component-name.spec.ts # Tests
```

### Naming Conventions
- **Files**: `kebab-case.component.ts` → `component-name.ts` (simplified)
- **Classes**: `PascalCase` → `ComponentName`
- **Selectors**: `app-kebab-case` → `app-component-name`
- **Signals**: camelCase → `isLoading`, `selectedItems`
- **Computed**: camelCase → `filteredItems`, `displayStats`

---

## 10. Checklist Before Committing

- [ ] Component uses standalone: true
- [ ] All imports added (CommonModule, FormsModule, etc.)
- [ ] Service injection uses `inject()`
- [ ] Signals used for state (not ngModel for internal state)
- [ ] Computed signals for derived data
- [ ] Loading states implemented
- [ ] Empty states implemented
- [ ] Error handling with retry option
- [ ] Proper TypeScript types
- [ ] HTML follows spacing/radius standards
- [ ] Icons use Bootstrap Icons (`bi-*`)
- [ ] Build passes without errors

---

**Document Version:** 1.0  
**Last Updated:** 2026-02-27  
**Author:** AI Assistant (Claude)
