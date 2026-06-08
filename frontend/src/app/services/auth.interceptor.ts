import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isAuthRequest = req.url.includes('/auth/login');
  const token = auth.token();
  const request = token && !isAuthRequest ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(request).pipe(
    catchError((error) => {
      if (!isAuthRequest && (error.status === 401 || error.status === 403)) {
        auth.logout();
      }
      return throwError(() => error);
    })
  );
};
