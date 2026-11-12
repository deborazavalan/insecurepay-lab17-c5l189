package cr.ac.ucr.if0009.insecurepay.util;

import cr.ac.ucr.if0009.insecurepay.exception.NoAutenticadoException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    public static final int LONGITUD_MINIMA_BYTES = 32;
    private static final String EMISOR = "insecurepay";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Debe definir la variable de entorno INSECUREPAY_JWT_SECRET.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LONGITUD_MINIMA_BYTES) {
            throw new IllegalStateException(
                    "INSECUREPAY_JWT_SECRET debe tener al menos 256 bits (32 bytes).");
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, String rol, Long usuarioId) {
        Date ahora = new Date();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(username)
                .claim("rol", rol)
                .claim("usuarioId", usuarioId)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(EMISOR)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new NoAutenticadoException("Token invalido o expirado.");
        }
    }

    public String extraerUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (NoAutenticadoException e) {
            return false;
        }
    }
}
