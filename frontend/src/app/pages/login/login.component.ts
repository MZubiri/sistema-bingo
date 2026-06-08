import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  error = '';

  constructor(private auth: AuthService, private router: Router) {}

  submit(): void {
    this.error = '';
    this.loading = true;
    this.auth.login(this.username, this.password).subscribe({
      next: (session) => {
        const target = session.role === 'ADMIN' ? '/admin/dashboard' : '/representante/dashboard';
        this.router.navigateByUrl(target);
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'No se pudo iniciar sesion';
        this.loading = false;
      }
    });
  }
}
