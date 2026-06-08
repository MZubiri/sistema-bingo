import { Component } from '@angular/core';
import { NgIf } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from './services/auth.service';
import { ApiService } from './services/api.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, NgIf, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  passwordModalOpen = false;
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  passwordLoading = false;
  passwordError = '';
  passwordMessage = '';

  constructor(public auth: AuthService, private api: ApiService) {}

  openPasswordModal(): void {
    this.passwordModalOpen = true;
    this.passwordError = '';
    this.passwordMessage = '';
  }

  closePasswordModal(): void {
    this.passwordModalOpen = false;
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.passwordLoading = false;
  }

  changePassword(): void {
    this.passwordError = '';
    this.passwordMessage = '';
    this.passwordLoading = true;
    this.api.changePassword(this.currentPassword, this.newPassword, this.confirmPassword).subscribe({
      next: (response) => {
        this.passwordMessage = response.message;
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.passwordLoading = false;
      },
      error: (err) => {
        this.passwordError = err?.error?.message ?? 'No se pudo cambiar la contraseña';
        this.passwordLoading = false;
      }
    });
  }
}
