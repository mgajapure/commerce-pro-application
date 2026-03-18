// src/app/shared/services/alert.service.ts
import { Injectable, signal } from '@angular/core';

export type AlertType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  type: AlertType;
  title: string;
  message?: string;
  duration: number; // ms, 0 = sticky
}

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

export interface ConfirmState extends ConfirmOptions {
  resolve: (value: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class AlertService {

  // ── Toasts ─────────────────────────────────────────────────────────────────
  readonly toasts = signal<Toast[]>([]);

  private add(type: AlertType, title: string, message?: string, duration = 5000) {
    const id = Math.random().toString(36).slice(2, 9);
    this.toasts.update(list => [...list, { id, type, title, message, duration }]);
    if (duration > 0) setTimeout(() => this.dismiss(id), duration);
  }

  success(title: string, message?: string) { this.add('success', title, message, 5000); }
  error  (title: string, message?: string) { this.add('error',   title, message, 6000); }
  warning(title: string, message?: string) { this.add('warning', title, message, 5000); }
  info   (title: string, message?: string) { this.add('info',    title, message, 5000); }

  dismiss(id: string) {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  // ── Confirm dialog ────────────────────────────────────────────────────────
  readonly confirmState = signal<ConfirmState | null>(null);

  confirm(options: ConfirmOptions): Promise<boolean> {
    return new Promise(resolve => {
      this.confirmState.set({ ...options, resolve });
    });
  }

  resolveConfirm(value: boolean) {
    const state = this.confirmState();
    if (state) {
      state.resolve(value);
      this.confirmState.set(null);
    }
  }
}
