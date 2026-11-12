package cr.ac.ucr.if0009.insecurepay.controller;

import cr.ac.ucr.if0009.insecurepay.dto.TransaccionDto;
import cr.ac.ucr.if0009.insecurepay.dto.TransferenciaDto;
import cr.ac.ucr.if0009.insecurepay.service.TransaccionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @GetMapping
    public ResponseEntity<List<TransaccionDto>> listarAutorizadas(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        return ResponseEntity.ok(transaccionService.listarAutorizadas(authHeader));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransaccionDto> obtenerPorId(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        return ResponseEntity.ok(transaccionService.obtenerPorId(id, authHeader));
    }

    @GetMapping("/cuenta/{cuentaId}")
    public ResponseEntity<List<TransaccionDto>> obtenerPorCuenta(
            @PathVariable Long cuentaId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        return ResponseEntity.ok(transaccionService.obtenerPorCuenta(cuentaId, authHeader));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<TransaccionDto>> buscarPorConcepto(
            @RequestParam("termino") String termino,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        return ResponseEntity.ok(transaccionService.buscarPorTermino(termino, authHeader));
    }

    @PostMapping("/transferir")
    public ResponseEntity<TransaccionDto> transferir(
            @Valid @RequestBody TransferenciaDto dto,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        TransaccionDto resultado = transaccionService.transferir(dto, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
