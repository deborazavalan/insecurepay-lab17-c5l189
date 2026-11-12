import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { TransferirComponent } from './components/transferir/transferir.component';
import { TransaccionesComponent } from './components/transacciones/transacciones.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'transferir', component: TransferirComponent, canActivate: [authGuard] },
  { path: 'transacciones', component: TransaccionesComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'dashboard' }
];
