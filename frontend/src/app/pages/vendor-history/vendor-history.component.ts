import { Component, OnInit } from '@angular/core';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { CardResponse, CardStatus } from '../../services/types';

@Component({
  selector: 'app-vendor-history',
  standalone: true,
  imports: [FormsModule, NgFor, NgIf, DatePipe],
  templateUrl: './vendor-history.component.html'
})
export class VendorHistoryComponent implements OnInit {
  cards: CardResponse[] = [];
  statusOptions: CardStatus[] = ['ASSIGNED', 'CANCELLED'];
  search = '';
  status: CardStatus | '' = '';
  assignedFrom = '';
  assignedTo = '';
  error = '';
  pdfError = '';

  constructor(public api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.vendorCards({
      search: this.search.trim(),
      status: this.status,
      assignedFrom: this.assignedFrom,
      assignedTo: this.assignedTo
    }).subscribe({
      next: (cards) => (this.cards = cards),
      error: (err) => (this.error = err?.error?.message ?? 'No se pudo cargar el historial')
    });
  }

  clearFilters(): void {
    this.search = '';
    this.status = '';
    this.assignedFrom = '';
    this.assignedTo = '';
    this.load();
  }

  viewPdf(card: CardResponse): void {
    this.pdfError = '';
    this.api.pdfBlob(card.serial).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => this.pdfError = err?.error?.message ?? 'No tienes autorizacion para ver este PDF'
    });
  }

  downloadPdf(card: CardResponse): void {
    this.pdfError = '';
    this.api.pdfBlob(card.serial).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        const buyer = (card.buyerName || 'comprador').replace(/[^a-zA-Z0-9]+/g, '_');
        link.href = url;
        link.download = `carton_${card.serial}_${buyer}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => this.pdfError = err?.error?.message ?? 'No tienes autorizacion para descargar este PDF'
    });
  }

  whatsappUrl(card: CardResponse): string {
    const verifyUrl = this.api.appUrl(`verificar/${card.serial}`);
    const text = `Hola, aquí está tu cartón de bingo:\n\nCartón N ${card.serial}\nComprador: ${card.buyerName}\n\nVerificar cartón:\n${verifyUrl}\n\nDescargar PDF:\n${this.api.publicPdfUrl(card.serial)}`;
    return `https://wa.me/?text=${encodeURIComponent(text)}`;
  }
}
