import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.token()) return true;
  return router.createUrlTree(['/login']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.token()) return true;
  return router.createUrlTree([auth.role() === 'ADMIN' ? '/admin/dashboard' : '/representante/dashboard']);
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.hasRole('ADMIN')) return true;
  return router.createUrlTree(['/login']);
};

export const vendorGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.hasRole('VENDEDOR')) return true;
  return router.createUrlTree(['/login']);
};
