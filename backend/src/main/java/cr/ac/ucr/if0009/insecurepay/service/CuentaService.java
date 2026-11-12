package cr.ac.ucr.if0009.insecurepay.service;

import cr.ac.ucr.if0009.insecurepay.dto.CuentaDto;
import cr.ac.ucr.if0009.insecurepay.exception.AccesoDenegadoException;
import cr.ac.ucr.if0009.insecurepay.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.insecurepay.model.CuentaBancaria;
import cr.ac.ucr.if0009.insecurepay.model.Usuario;
import cr.ac.ucr.if0009.insecurepay.repository.CuentaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final IdentidadService identidadService;

    public CuentaService(CuentaRepository cuentaRepository, IdentidadService identidadService) {
        this.cuentaRepository = cuentaRepository;
        this.identidadService = identidadService;
    }

    @Transactional(readOnly = true)
    public List<CuentaDto> listarAutorizadas(String authHeader) {
        Usuario autenticado = identidadService.usuarioAutenticado(authHeader);
        List<CuentaBancaria> cuentas = identidadService.esPrivilegiado(autenticado)
                ? cuentaRepository.findAll()
                : cuentaRepository.findByUsuarioId(autenticado.getId());
        return cuentas.stream()
                .map(this::convertirADto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CuentaDto obtenerCuentaSegura(Long cuentaId, String authHeader) {
        Usuario autenticado = identidadService.usuarioAutenticado(authHeader);

        CuentaBancaria cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria no existe"));

        boolean esPropietario = identidadService.esPropietario(cuenta, autenticado);
        boolean esAuditor = identidadService.esPrivilegiado(autenticado);

        if (!esPropietario && !esAuditor) {
            throw new AccesoDenegadoException("Acceso no autorizado a cuenta ajena.");
        }

        return convertirADto(cuenta);
    }

    @Transactional(readOnly = true)
    public List<CuentaDto> obtenerPorUsuario(Long usuarioId, String authHeader) {
        Usuario autenticado = identidadService.usuarioAutenticado(authHeader);

        if (!autenticado.getId().equals(usuarioId) && !identidadService.esPrivilegiado(autenticado)) {
            throw new AccesoDenegadoException("Acceso no autorizado a cuentas de otro cliente.");
        }

        return cuentaRepository.findByUsuarioId(usuarioId).stream()
                .map(this::convertirADto)
                .toList();
    }

    private CuentaDto convertirADto(CuentaBancaria cuenta) {
        CuentaDto dto = new CuentaDto();
        dto.setId(cuenta.getId());
        dto.setUsuarioId(cuenta.getUsuario().getId());
        dto.setTitular(cuenta.getUsuario().getNombreCompleto());
        dto.setNumeroCuenta(cuenta.getNumeroCuenta());
        dto.setTipoCuenta(cuenta.getTipoCuenta().name());
        dto.setSaldo(cuenta.getSaldo());
        dto.setMoneda(cuenta.getMoneda());
        dto.setActiva(cuenta.getActiva());
        return dto;
    }
}
