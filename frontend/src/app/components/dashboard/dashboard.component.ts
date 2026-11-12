import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { BancoService } from '../../services/banco.service';
import { Cuenta } from '../../models/banco.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  misCuentas: Cuenta[] = [];
  loading = false;
  errorMsg = '';

  constructor(
    public authService: AuthService,
    private bancoService: BancoService
  ) {}

  ngOnInit(): void {
    this.cargarMisCuentas();
  }

  cargarMisCuentas(): void {
    const user = this.authService.currentUser();
    if (!user) return;

    this.loading = true;
    this.bancoService.obtenerCuentasUsuario(user.usuarioId).subscribe({
      next: (cuentas) => {
        this.misCuentas = cuentas;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = 'Error al cargar cuentas: ' + (err.error?.detail || 'intente nuevamente.');
      }
    });
  }
}
