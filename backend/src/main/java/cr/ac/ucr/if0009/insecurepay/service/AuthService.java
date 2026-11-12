package cr.ac.ucr.if0009.insecurepay.service;

import cr.ac.ucr.if0009.insecurepay.dto.LoginRequest;
import cr.ac.ucr.if0009.insecurepay.dto.LoginResponse;
import cr.ac.ucr.if0009.insecurepay.exception.CredencialesInvalidasException;
import cr.ac.ucr.if0009.insecurepay.model.Usuario;
import cr.ac.ucr.if0009.insecurepay.repository.UsuarioRepository;
import cr.ac.ucr.if0009.insecurepay.util.JwtUtil;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String MENSAJE_GENERICO = "Credenciales invalidas.";

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final String hashSenuelo;

    public AuthService(UsuarioRepository usuarioRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.hashSenuelo = passwordEncoder.encode("insecurepay-senuelo-temporizacion");
    }

    public LoginResponse autenticar(LoginRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findByUsername(request.getUsername());
        String hashAlmacenado = encontrado.map(Usuario::getPassword).orElse(hashSenuelo);

        boolean coincide = passwordEncoder.matches(request.getPassword(), hashAlmacenado);

        if (encontrado.isEmpty() || !coincide) {
            throw new CredencialesInvalidasException(MENSAJE_GENERICO);
        }

        Usuario usuario = encontrado.get();
        String token = jwtUtil.generateToken(usuario.getUsername(), usuario.getRol().name(), usuario.getId());

        return new LoginResponse(
                token,
                usuario.getUsername(),
                usuario.getNombreCompleto(),
                usuario.getRol().name(),
                usuario.getId()
        );
    }
}
