package cr.ac.ucr.if0009.insecurepay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(@Value("${app.security.bcrypt-strength:12}") int fuerza) {
        if (fuerza < 10) {
            throw new IllegalStateException("El factor de trabajo de BCrypt debe ser 10 o superior.");
        }
        return new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2A, fuerza);
    }
}
