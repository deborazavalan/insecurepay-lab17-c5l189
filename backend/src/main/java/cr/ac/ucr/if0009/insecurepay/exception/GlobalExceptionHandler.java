package cr.ac.ucr.if0009.insecurepay.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String BASE_TIPO = "https://insecurepay.ucr.ac.cr/problemas/";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return problema(HttpStatus.NOT_FOUND, "recurso-no-encontrado", "Recurso no encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ProblemDetail accesoDenegado(AccesoDenegadoException ex, HttpServletRequest req) {
        log.warn("Intento de acceso no autorizado en {} {}", req.getMethod(), req.getRequestURI());
        return problema(HttpStatus.FORBIDDEN, "acceso-denegado", "Acceso denegado", ex.getMessage(), req);
    }

    @ExceptionHandler({NoAutenticadoException.class, CredencialesInvalidasException.class})
    public ProblemDetail noAutenticado(RuntimeException ex, HttpServletRequest req) {
        return problema(HttpStatus.UNAUTHORIZED, "no-autenticado", "No autenticado", ex.getMessage(), req);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail solicitudInvalida(SolicitudInvalidaException ex, HttpServletRequest req) {
        return problema(HttpStatus.BAD_REQUEST, "solicitud-invalida", "Solicitud invalida", ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInterno(Exception ex, HttpServletRequest req) {
        String incidente = UUID.randomUUID().toString();
        log.error("Incidente {} en {} {}", incidente, req.getMethod(), req.getRequestURI(), ex);
        ProblemDetail pd = problema(HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "Error interno",
                "Ocurrio un error inesperado. Reporte el codigo de incidente a soporte.", req);
        pd.setProperty("incidente", incidente);
        return pd;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField())
                .distinct()
                .toList();
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "validacion", "Solicitud invalida",
                "Uno o mas campos no cumplen las validaciones.", request);
        pd.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "parametro-invalido", "Solicitud invalida",
                "Uno o mas parametros tienen un formato invalido.", request);
        return ResponseEntity.badRequest().body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "cuerpo-invalido", "Solicitud invalida",
                "El cuerpo de la solicitud no es valido.", request);
        return ResponseEntity.badRequest().body(pd);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail pd) {
            completar(pd, rutaDe(request));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ProblemDetail problema(HttpStatus estado, String tipo, String titulo, String detalle, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estado, detalle);
        pd.setType(URI.create(BASE_TIPO + tipo));
        pd.setTitle(titulo);
        completar(pd, req.getRequestURI());
        return pd;
    }

    private ProblemDetail problema(HttpStatus estado, String tipo, String titulo, String detalle, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estado, detalle);
        pd.setType(URI.create(BASE_TIPO + tipo));
        pd.setTitle(titulo);
        completar(pd, rutaDe(request));
        return pd;
    }

    private void completar(ProblemDetail pd, String ruta) {
        if (ruta != null) {
            try {
                pd.setInstance(URI.create(ruta));
            } catch (IllegalArgumentException ignorada) {
                pd.setInstance(null);
            }
        }
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
    }

    private String rutaDe(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return null;
    }
}
