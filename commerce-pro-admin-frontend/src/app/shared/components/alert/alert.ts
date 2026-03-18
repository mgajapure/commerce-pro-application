// src/app/shared/components/alert/alert.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert.html',
  styleUrl: './alert.scss'
})
export class AlertComponent {
  readonly alertSvc = inject(AlertService);

  // Plain string[] signal — Angular can diff arrays and re-render correctly
  readonly leavingIds = signal<string[]>([]);

  dismiss(id: string): void {
    // Add to leaving list to trigger exit animation
    this.leavingIds.update(ids => [...ids, id]);
    // Remove from service after animation completes
    setTimeout(() => {
      this.alertSvc.dismiss(id);
      this.leavingIds.update(ids => ids.filter(i => i !== id));
    }, 320);
  }

  confirm(value: boolean): void {
    this.alertSvc.resolveConfirm(value);
  }
}
