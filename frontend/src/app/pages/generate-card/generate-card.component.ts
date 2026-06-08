import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { AvailableCard, CardResponse } from '../../services/types';

@Component({
  selector: 'app-generate-card',
  standalone: true,
  imports: [FormsModule, NgIf, NgFor, DatePipe],
  templateUrl: './generate-card.component.html',
  styleUrl: './generate-card.component.css'
})
export class GenerateCardComponent {
  buyerName = '';
  search = '';
  page = 0;
  size = 50;
  totalElements = 0;
  totalPages = 0;
  availableCards: AvailableCard[] = [];
  selectedCard: AvailableCard | null = null;
  loading = false;
  loadingAvailable = false;
  error = '';
  pdfError = '';
  card: CardResponse | null = null;

  constructor(public api: ApiService) {}

  ngOnInit(): void {
    this.loadAvailable();
  }

  loadAvailable(page = 0): void {
    this.loadingAvailable = true;
    this.page = page;
    this.api.availableCards(this.search, this.page, this.size).subscribe({
      next: (result) => {
        this.availableCards = result.content;
        this.totalElements = result.totalElements;
        this.totalPages = result.totalPages;
        this.page = result.page;
        this.loadingAvailable = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'No se pudieron cargar los cartones disponibles';
        this.loadingAvailable = false;
      }
    });
  }

  selectCard(card: AvailableCard): void {
    this.selectedCard = card;
    this.error = '';
  }

  generate(): void {
    if (!this.selectedCard) {
      this.error = 'Selecciona un carton disponible';
      return;
    }
    this.error = '';
    this.pdfError = '';
    this.card = null;
    this.loading = true;
    const key = crypto.randomUUID();
    this.api.generateCard(this.buyerName, this.selectedCard.serial, key).subscribe({
      next: (card) => {
        this.card = card;
        this.availableCards = this.availableCards.filter((item) => item.serial !== card.serial);
        this.totalElements = Math.max(0, this.totalElements - 1);
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'No se pudo generar el carton';
        this.loading = false;
        this.loadAvailable(this.page);
      }
    });
  }

  generateAnother(): void {
    this.buyerName = '';
    this.selectedCard = null;
    this.card = null;
    this.error = '';
    this.pdfError = '';
    this.loadAvailable(this.page);
  }

  numbers(): string[][] {
    if (!this.card?.numbersJson) return [];
    try {
      return JSON.parse(this.card.numbersJson);
    } catch {
      return [];
    }
  }

  viewPdf(): void {
    if (!this.card) return;
    this.api.pdfBlob(this.card.serial).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => this.pdfError = err?.error?.message ?? 'No tienes autorizacion para ver este PDF'
    });
  }

  downloadPdf(): void {
    if (!this.card) return;
    this.api.downloadPdf(this.card.serial, this.card.buyerName);
  }

  whatsappUrl(): string {
    const serial = this.card?.serial ?? '';
    const buyer = this.card?.buyerName ?? '';
    const verifyUrl = this.api.appUrl(`verificar/${serial}`);
    const text = `Hola, aquí está tu cartón de bingo:\n\nCartón N ${serial}\nComprador: ${buyer}\n\nVerificar cartón:\n${verifyUrl}\n\nDescargar PDF:\n${this.api.publicPdfUrl(serial)}`;
    return `https://wa.me/?text=${encodeURIComponent(text)}`;
  }
}
