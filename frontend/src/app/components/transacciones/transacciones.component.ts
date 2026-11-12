import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BancoService } from '../../services/banco.service';
import { AuthService } from '../../services/auth.service';
import { Transaccion } from '../../models/banco.model';

@Component({
  selector: 'app-transacciones',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './transacciones.component.html'
})
export class TransaccionesComponent implements OnInit {
  transacciones: Transaccion[] = [];
  transaccionSeleccionada: Transaccion | null = null;
  terminoBusqueda = '';
  loading = false;
  errorInfo = '';

  constructor(
    private bancoService: BancoService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.cargarTransacciones();
  }

  cargarTransacciones(): void {
    this.loading = true;
    this.errorInfo = '';
    this.terminoBusqueda = '';
    this.bancoService.listarTransacciones().subscribe({
      next: (data) => {
        this.transacciones = data;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorInfo = 'Error al cargar historial: ' + (err.error?.detail || 'intente nuevamente.');
      }
    });
  }

  ejecutarBusqueda(): void {
    if (!this.terminoBusqueda.trim()) {
      this.cargarTransacciones();
      return;
    }

    this.loading = true;
    this.errorInfo = '';
    this.bancoService.buscarTransacciones(this.terminoBusqueda).subscribe({
      next: (data) => {
        this.transacciones = data;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorInfo = 'No fue posible realizar la busqueda: ' + (err.error?.detail || 'intente nuevamente.');
      }
    });
  }

  verComprobante(t: Transaccion): void {
    this.transaccionSeleccionada = t;
  }

  cerrarComprobante(): void {
    this.transaccionSeleccionada = null;
  }
}
