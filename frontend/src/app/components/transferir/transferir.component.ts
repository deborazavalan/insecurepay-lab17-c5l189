import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { BancoService } from '../../services/banco.service';
import { AuthService } from '../../services/auth.service';
import { Cuenta, TransferenciaRequest } from '../../models/banco.model';

@Component({
  selector: 'app-transferir',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './transferir.component.html'
})
export class TransferirComponent implements OnInit {
  misCuentas: Cuenta[] = [];
  transferencia: TransferenciaRequest = {
    cuentaOrigenId: null,
    numeroCuentaDestino: '',
    monto: null,
    conceptoHtml: ''
  };

  loading = false;
  errorMsg = '';
  successMsg = '';

  constructor(
    private bancoService: BancoService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.bancoService.listarMisCuentas().subscribe({
      next: (cuentas) => {
        this.misCuentas = cuentas;
        if (cuentas.length > 0) {
          this.transferencia.cuentaOrigenId = cuentas[0].id;
        }
      },
      error: (err) => {
        this.errorMsg = 'No fue posible cargar sus cuentas: ' + (err.error?.detail || 'intente nuevamente.');
      }
    });
  }

  onTransferir(): void {
    if (!this.transferencia.cuentaOrigenId || !this.transferencia.numeroCuentaDestino || !this.transferencia.monto) {
      this.errorMsg = 'Por favor complete todos los datos de la transferencia.';
      return;
    }

    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.bancoService.transferir(this.transferencia).subscribe({
      next: (res) => {
        this.loading = false;
        this.successMsg = `¡Transferencia exitosa! Débito de ₡${res.monto} ejecutado en la cuenta ${res.numeroCuentaOrigen}.`;
        setTimeout(() => {
          this.router.navigate(['/transacciones']);
        }, 1500);
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = 'Error en la transacción: ' + (err.error?.detail || 'intente nuevamente.');
      }
    });
  }
}
