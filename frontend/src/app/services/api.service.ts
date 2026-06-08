import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AvailableCard,
  AdminCard,
  AdminCardFilters,
  AdminDashboard,
  CardResponse,
  CardHistoryFilters,
  PageResponse,
  VendorDashboard,
  VerifyResponse
} from './types';
import { apiBaseUrl, appUrl } from './api-url';

const API_URL = apiBaseUrl();

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  vendorDashboard(): Observable<VendorDashboard> {
    return this.http.get<VendorDashboard>(`${API_URL}/representative/dashboard`);
  }

  availableCards(search: string, page: number, size: number): Observable<PageResponse<AvailableCard>> {
    return this.http.get<PageResponse<AvailableCard>>(`${API_URL}/representative/cards/available`, {
      params: { search, page, size }
    });
  }

  generateCard(buyerName: string, serial: string, idempotencyKey: string): Observable<CardResponse> {
    return this.http.post<CardResponse>(
      `${API_URL}/representative/cards/generate`,
      { buyerName, serial, idempotencyKey },
      { headers: { 'Idempotency-Key': idempotencyKey } }
    );
  }

  vendorCards(filters: CardHistoryFilters = {}): Observable<CardResponse[]> {
    return this.http.get<CardResponse[]>(`${API_URL}/representative/cards`, {
      params: this.cleanParams(filters)
    });
  }

  adminDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(`${API_URL}/admin/dashboard`);
  }

  adminCards(filters: AdminCardFilters = {}): Observable<AdminCard[]> {
    return this.http.get<AdminCard[]>(`${API_URL}/admin/cards`, {
      params: this.cleanParams(filters)
    });
  }

  cancelCard(id: number): Observable<CardResponse> {
    return this.http.patch<CardResponse>(`${API_URL}/admin/cards/${id}/cancel`, {});
  }

  changePassword(currentPassword: string, newPassword: string, confirmPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${API_URL}/account/change-password`, {
      currentPassword,
      newPassword,
      confirmPassword
    });
  }

  verify(serial: string): Observable<VerifyResponse> {
    return this.http.get<VerifyResponse>(`${API_URL}/public/cards/${serial}/verify`);
  }

  pdfUrl(serial: string): string {
    return `${API_URL}/cards/${serial}/pdf`;
  }

  publicPdfUrl(serial: string): string {
    return `${API_URL}/public/cards/${serial}/pdf`;
  }

  appUrl(path: string): string {
    return appUrl(path);
  }

  pdfBlob(serial: string): Observable<Blob> {
    return this.http.get(`${API_URL}/cards/${serial}/pdf`, { responseType: 'blob' });
  }

  publicPdfBlob(serial: string): Observable<Blob> {
    return this.http.get(this.publicPdfUrl(serial), { responseType: 'blob' });
  }

  viewPdf(serial: string): void {
    this.pdfBlob(serial).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    });
  }

  downloadPdf(serial: string, buyerName?: string | null): void {
    this.pdfBlob(serial).subscribe((blob) => {
      const safeBuyer = (buyerName || 'comprador').normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '');
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `carton_${serial}_${safeBuyer}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  downloadPublicPdf(serial: string, buyerName?: string | null): Observable<Blob> {
    return this.publicPdfBlob(serial);
  }

  private cleanParams<T extends object>(values: T): Record<string, string> {
    return Object.fromEntries(
      Object.entries(values)
        .filter(([, value]) => value !== undefined && value !== '')
        .map(([key, value]) => [key, String(value)])
    );
  }
}
