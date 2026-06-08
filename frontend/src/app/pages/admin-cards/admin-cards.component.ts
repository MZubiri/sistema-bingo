import { Component, OnInit } from '@angular/core';
import { DatePipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AdminCard, CardStatus, OrganizationCode } from '../../services/types';

@Component({
  selector: 'app-admin-cards',
  standalone: true,
  imports: [FormsModule, NgFor, NgIf, NgClass, DatePipe],
  templateUrl: './admin-cards.component.html'
})
export class AdminCardsComponent implements OnInit {
  cards: AdminCard[] = [];
  organizationOptions: OrganizationCode[] = ['GEOURP', 'CIVIAL', 'ACI'];
  statusOptions: CardStatus[] = ['AVAILABLE', 'ASSIGNED', 'CANCELLED'];
  search = '';
  organizationCode: OrganizationCode | '' = '';
  status: CardStatus | '' = '';
  assignedFrom = '';
  assignedTo = '';
  error = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.adminCards({
      search: this.search.trim(),
      organizationCode: this.organizationCode,
      status: this.status,
      assignedFrom: this.assignedFrom,
      assignedTo: this.assignedTo
    }).subscribe({
      next: (cards) => (this.cards = cards),
      error: (err) => (this.error = err?.error?.message ?? 'No se pudieron cargar los cartones')
    });
  }

  clearFilters(): void {
    this.search = '';
    this.organizationCode = '';
    this.status = '';
    this.assignedFrom = '';
    this.assignedTo = '';
    this.load();
  }

  cancel(card: AdminCard): void {
    if (!confirm(`Cancelar carton ${card.serial}?`)) return;
    this.api.cancelCard(card.id).subscribe({
      next: () => this.load(),
      error: (err) => (this.error = err?.error?.message ?? 'No se pudo cancelar el carton')
    });
  }

  badgeClass(status: string): string {
    return status === 'AVAILABLE' ? 'badge-available' : status === 'CANCELLED' ? 'badge-cancelled' : 'badge-assigned';
  }
}
