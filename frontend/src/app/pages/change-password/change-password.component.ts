import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './change-password.component.html'
})
export class ChangePasswordComponent {
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  loading = false;
  error = '';
  message = '';

  constructor(private api: ApiService) {}

  submit(): void {
    this.error = '';
    this.message = '';
    this.loading = true;
    this.api.changePassword(this.currentPassword, this.newPassword, this.confirmPassword).subscribe({
      next: (response) => {
        this.message = response.message;
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'No se pudo cambiar la contraseña';
        this.loading = false;
      }
    });
  }
}
