import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard, vendorGuard } from './guards/auth.guard';
import { LoginComponent } from './pages/login/login.component';
import { AdminDashboardComponent } from './pages/admin-dashboard/admin-dashboard.component';
import { AdminCardsComponent } from './pages/admin-cards/admin-cards.component';
import { VendorDashboardComponent } from './pages/vendor-dashboard/vendor-dashboard.component';
import { GenerateCardComponent } from './pages/generate-card/generate-card.component';
import { VendorHistoryComponent } from './pages/vendor-history/vendor-history.component';
import { ChangePasswordComponent } from './pages/change-password/change-password.component';
import { VerifyComponent } from './pages/verify/verify.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'admin/dashboard', component: AdminDashboardComponent, canActivate: [authGuard, adminGuard] },
  { path: 'admin/cartones', component: AdminCardsComponent, canActivate: [authGuard, adminGuard] },
  { path: 'representante/dashboard', component: VendorDashboardComponent, canActivate: [authGuard, vendorGuard] },
  { path: 'representante/generar', component: GenerateCardComponent, canActivate: [authGuard, vendorGuard] },
  { path: 'representante/historial', component: VendorHistoryComponent, canActivate: [authGuard, vendorGuard] },
  { path: 'vendedor/dashboard', redirectTo: 'representante/dashboard' },
  { path: 'vendedor/generar', redirectTo: 'representante/generar' },
  { path: 'vendedor/historial', redirectTo: 'representante/historial' },
  { path: 'account/change-password', component: ChangePasswordComponent, canActivate: [authGuard] },
  { path: 'verificar/:serial', component: VerifyComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];
