import { Component, OnInit } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { AdminDashboard } from '../../services/types';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [NgIf, NgFor],
  templateUrl: './admin-dashboard.component.html'
})
export class AdminDashboardComponent implements OnInit {
  data: AdminDashboard | null = null;
  error = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.adminDashboard().subscribe({
      next: (data) => (this.data = data),
      error: (err) => (this.error = err?.error?.message ?? 'No se pudo cargar el dashboard')
    });
  }

  assignedPercent(): number {
    if (!this.data?.totalCards) return 0;
    return this.percent(this.data.assignedCards, this.data.totalCards);
  }

  availablePercent(): number {
    if (!this.data?.totalCards) return 0;
    return this.percent(this.data.availableCards, this.data.totalCards);
  }

  orgAssignedPercent(assigned: number, total: number): number {
    if (!total) return 0;
    return this.percent(assigned, total);
  }

  orgAvailablePercent(available: number, total: number): number {
    if (!total) return 0;
    return this.percent(available, total);
  }

  assignedVisualPercent(): number {
    if (!this.data?.assignedCards) return 0;
    return Math.max(this.assignedPercent(), 2);
  }

  availableVisualPercent(): number {
    if (!this.data?.availableCards) return 0;
    const available = this.availablePercent();
    return this.data.assignedCards > 0 ? Math.max(0, 100 - this.assignedVisualPercent()) : available;
  }

  orgAssignedVisualPercent(assigned: number, total: number): number {
    if (!assigned) return 0;
    return Math.max(this.orgAssignedPercent(assigned, total), 3);
  }

  orgAvailableVisualPercent(assigned: number, available: number, total: number): number {
    if (!available) return 0;
    return assigned > 0 ? Math.max(0, 100 - this.orgAssignedVisualPercent(assigned, total)) : this.orgAvailablePercent(available, total);
  }

  private percent(value: number, total: number): number {
    if (!total) return 0;
    return Math.round((value / total) * 1000) / 10;
  }
}
