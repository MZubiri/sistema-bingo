import { Component, OnInit } from '@angular/core';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { VerifyResponse } from '../../services/types';

@Component({
  selector: 'app-verify',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe],
  templateUrl: './verify.component.html',
  styleUrl: './verify.component.css'
})
export class VerifyComponent implements OnInit {
  data: VerifyResponse | null = null;
  error = '';

  constructor(private route: ActivatedRoute, public api: ApiService) {}

  ngOnInit(): void {
    const serial = this.route.snapshot.paramMap.get('serial') ?? '';
    this.api.verify(serial).subscribe({
      next: (data) => (this.data = data),
      error: (err) => (this.error = err?.error?.message ?? 'No se pudo verificar el carton')
    });
  }

  numbers(): string[][] {
    if (!this.data?.numbersJson) return [];
    try {
      return JSON.parse(this.data.numbersJson);
    } catch {
      return [];
    }
  }

  canDownloadPdf(): boolean {
    return this.data?.status === 'ASSIGNED';
  }
}
