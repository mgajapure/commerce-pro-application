import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-tooltip-label',
  standalone: true,
  imports: [CommonModule],
  template: `
    <label class="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1">
      {{ label }}
      @if (required) { <span class="text-red-500">*</span> }
      @if (tooltip) {
        <span class="relative group cursor-help">
          <i class="bi bi-info-circle text-gray-400 text-xs"></i>
          <span class="absolute z-50 bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-2 text-xs text-white bg-gray-900 rounded-lg shadow-lg whitespace-nowrap opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 pointer-events-none max-w-xs text-center">
            {{ tooltip }}
            <span class="absolute top-full left-1/2 -translate-x-1/2 -mt-px border-4 border-transparent border-t-gray-900"></span>
          </span>
        </span>
      }
    </label>
  `
})
export class TooltipLabel {
  @Input() label = '';
  @Input() tooltip = '';
  @Input() required = false;
}
