package cr.ac.ucr.if0009.insecurepay.service;

import cr.ac.ucr.if0009.insecurepay.exception.NoAutenticadoException;
import cr.ac.ucr.if0009.insecurepay.model.CuentaBancaria;
import cr.ac.ucr.if0009.insecurepay.model.Rol;
import cr.ac.ucr.if0009.insecurepay.model.Usuario;
import cr.ac.ucr.if0009.insecurepay.repository.UsuarioRepository;
import cr.ac.ucr.if0009.insecurepay.util.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class IdentidadService {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public IdentidadService(JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario usuarioAutenticado(String authHeader) {
        String usernameAutenticado = jwtUtil.extraerUsername(extraerToken(authHeader));
        return usuarioRepository.findByUsername(usernameAutenticado)
                .orElseThrow(() -> new NoAutenticadoException("Sesion invalida."));
    }

    public boolean esPrivilegiado(Usuario usuario) {
        return usuario.getRol() == Rol.AUDITOR || usuario.getRol() == Rol.ADMIN;
    }

    public boolean esPropietario(CuentaBancaria cuenta, Usuario usuario) {
        return cuenta.getUsuario().getUsername().equalsIgnoreCase(usuario.getUsername());
    }

    private String extraerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            return authHeader.substring(7);
        }
        throw new NoAutenticadoException("Cabecera de autorizacion invalida.");
    }
}
