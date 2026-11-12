package cr.ac.ucr.if0009.insecurepay.controller;

import cr.ac.ucr.if0009.insecurepay.dto.CuentaDto;
import cr.ac.ucr.if0009.insecurepay.service.CuentaService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public ResponseEntity<List<CuentaDto>> listarAutorizadas(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        return ResponseEntity.ok(cuentaService.listarAutorizadas(authHeader));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaDto> obtenerPorId(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        return ResponseEntity.ok(cuentaService.obtenerCuentaSegura(id, authHeader));
    }

    @GetMapping("/{id}/saldo")
    public ResponseEntity<Map<String, Object>> obtenerSaldo(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        CuentaDto cuenta = cuentaService.obtenerCuentaSegura(id, authHeader);
        return ResponseEntity.ok(Map.of(
            "cuentaId", cuenta.getId(),
            "numeroCuenta", cuenta.getNumeroCuenta(),
            "saldo", cuenta.getSaldo(),
            "moneda", cuenta.getMoneda()
        ));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<CuentaDto>> obtenerPorUsuario(
            @PathVariable Long usuarioId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        return ResponseEntity.ok(cuentaService.obtenerPorUsuario(usuarioId, authHeader));
    }
}
