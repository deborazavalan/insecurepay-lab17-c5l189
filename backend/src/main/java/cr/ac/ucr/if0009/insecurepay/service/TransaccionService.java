package cr.ac.ucr.if0009.insecurepay.service;

import cr.ac.ucr.if0009.insecurepay.dto.TransaccionDto;
import cr.ac.ucr.if0009.insecurepay.dto.TransferenciaDto;
import cr.ac.ucr.if0009.insecurepay.exception.AccesoDenegadoException;
import cr.ac.ucr.if0009.insecurepay.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.insecurepay.exception.SolicitudInvalidaException;
import cr.ac.ucr.if0009.insecurepay.model.CuentaBancaria;
import cr.ac.ucr.if0009.insecurepay.model.Transaccion;
import cr.ac.ucr.if0009.insecurepay.model.Usuario;
import cr.ac.ucr.if0009.insecurepay.repository.CuentaRepository;
import cr.ac.ucr.if0009.insecurepay.repository.TransaccionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransaccionService {

    private static final int LONGITUD_MAXIMA_TERMINO = 100;

    private final TransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;
    private final IdentidadService identidadService;

    public TransaccionService(TransaccionRepository transaccionRepository,
                              CuentaRepository cuentaRepository,
                              IdentidadService identidadService) {
        this.transaccionRepository = transaccionRepository;
        this.cuentaRepository = cuentaRepository;
        this.identidadService = identidadService;
    }

    @Transactional(readOnly = true)
    public List<TransaccionDto> listarAutorizadas(String authHeader) {
        Usuario usuario = identidadService.usuarioAutenticado(authHeader);
        List<Transaccion> transacciones = identidadService.esPrivilegiado(usuario)
                ? transaccionRepository.findAllByOrderByFechaDesc()
                : transaccionRepository.buscarPorUsuario(usuario.getId());
        return transacciones.stream()
                .map(this::convertirADto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransaccionDto obtenerPorId(Long id, String authHeader) {
        Usuario usuario = identidadService.usuarioAutenticado(authHeader);
        Transaccion transaccion = transaccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Transaccion no existe"));

        boolean participa = identidadService.esPropietario(transaccion.getCuentaOrigen(), usuario)
                || identidadService.esPropietario(transaccion.getCuentaDestino(), usuario);

        if (!participa && !identidadService.esPrivilegiado(usuario)) {
            throw new AccesoDenegadoException("Acceso no autorizado a transaccion ajena.");
        }
        return convertirADto(transaccion);
    }

    @Transactional(readOnly = true)
    public List<TransaccionDto> obtenerPorCuenta(Long cuentaId, String authHeader) {
        Usuario usuario = identidadService.usuarioAutenticado(authHeader);
        CuentaBancaria cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria no existe"));

        if (!identidadService.esPropietario(cuenta, usuario) && !identidadService.esPrivilegiado(usuario)) {
            throw new AccesoDenegadoException("Acceso no autorizado a cuenta ajena.");
        }

        return transaccionRepository.findByCuentaOrigenIdOrCuentaDestinoIdOrderByFechaDesc(cuentaId, cuentaId).stream()
                .map(this::convertirADto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransaccionDto> buscarPorTermino(String termino, String authHeader) {
        Usuario usuario = identidadService.usuarioAutenticado(authHeader);

        if (termino == null || termino.trim().isEmpty()) {
            return List.of();
        }
        String limpio = termino.trim();
        if (limpio.length() > LONGITUD_MAXIMA_TERMINO) {
            throw new SolicitudInvalidaException("El termino de busqueda excede la longitud permitida.");
        }

        List<Transaccion> resultado = identidadService.esPrivilegiado(usuario)
                ? transaccionRepository.buscarPorTerminoSeguro(limpio)
                : transaccionRepository.buscarPorTerminoSeguroDelUsuario(limpio, usuario.getId());

        return resultado.stream()
                .map(this::convertirADto)
                .toList();
    }

    @Transactional
    public TransaccionDto transferir(TransferenciaDto dto, String authHeader) {
        Usuario usuarioAutenticado = identidadService.usuarioAutenticado(authHeader);

        if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException("El monto a transferir debe ser mayor a cero.");
        }

        CuentaBancaria cuentaOrigen = cuentaRepository.findById(dto.getCuentaOrigenId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta de origen no existe"));

        if (!cuentaOrigen.getUsuario().getUsername()
                .equalsIgnoreCase(usuarioAutenticado.getUsername())) {
            throw new AccesoDenegadoException(
                "Fraude detectado: no puede debitar " +
                "fondos de cuentas ajenas.");
        }

        CuentaBancaria cuentaDestino = cuentaRepository.findByNumeroCuenta(dto.getNumeroCuentaDestino().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta de destino no existe"));

        if (cuentaOrigen.getId().equals(cuentaDestino.getId())) {
            throw new SolicitudInvalidaException("La cuenta de origen y destino no pueden ser la misma.");
        }
        if (!Boolean.TRUE.equals(cuentaOrigen.getActiva()) || !Boolean.TRUE.equals(cuentaDestino.getActiva())) {
            throw new SolicitudInvalidaException("Una de las cuentas no se encuentra activa.");
        }
        if (!cuentaOrigen.getMoneda().equals(cuentaDestino.getMoneda())) {
            throw new SolicitudInvalidaException("Las cuentas deben manejar la misma moneda.");
        }
        if (cuentaOrigen.getSaldo().compareTo(dto.getMonto()) < 0) {
            throw new SolicitudInvalidaException("Fondos insuficientes en la cuenta de origen seleccionada.");
        }

        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(dto.getMonto()));
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(dto.getMonto()));
        cuentaRepository.save(cuentaOrigen);
        cuentaRepository.save(cuentaDestino);

        Transaccion transaccion = new Transaccion();
        transaccion.setCuentaOrigen(cuentaOrigen);
        transaccion.setCuentaDestino(cuentaDestino);
        transaccion.setMonto(dto.getMonto());
        transaccion.setConceptoHtml(sanitizarTexto(dto.getConceptoHtml()));
        transaccion.setEstado("COMPLETADA");

        Transaccion guardada = transaccionRepository.save(transaccion);
        return convertirADto(guardada);
    }

    private String sanitizarTexto(String entrada) {
        if (entrada == null) return "";
        return entrada.trim()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    private TransaccionDto convertirADto(Transaccion t) {
        TransaccionDto dto = new TransaccionDto();
        dto.setId(t.getId());
        dto.setCuentaOrigenId(t.getCuentaOrigen().getId());
        dto.setNumeroCuentaOrigen(t.getCuentaOrigen().getNumeroCuenta());
        dto.setTitularOrigen(t.getCuentaOrigen().getUsuario().getNombreCompleto());
        dto.setCuentaDestinoId(t.getCuentaDestino().getId());
        dto.setNumeroCuentaDestino(t.getCuentaDestino().getNumeroCuenta());
        dto.setTitularDestino(t.getCuentaDestino().getUsuario().getNombreCompleto());
        dto.setMonto(t.getMonto());
        dto.setConceptoHtml(t.getConceptoHtml());
        dto.setFecha(t.getFecha());
        dto.setEstado(t.getEstado());
        return dto;
    }
}
