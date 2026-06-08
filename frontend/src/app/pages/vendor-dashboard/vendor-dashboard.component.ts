import { Component, OnInit } from '@angular/core';
import { NgIf } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { VendorDashboard } from '../../services/types';

@Component({
  selector: 'app-vendor-dashboard',
  standalone: true,
  imports: [NgIf],
  templateUrl: './vendor-dashboard.component.html'
})
export class VendorDashboardComponent implements OnInit {
  data: VendorDashboard | null = null;
  error = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.vendorDashboard().subscribe({
      next: (data) => (this.data = data),
      error: (err) => (this.error = err?.error?.message ?? 'No se pudo cargar el dashboard')
    });
  }
}
