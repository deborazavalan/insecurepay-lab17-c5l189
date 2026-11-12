package cr.ac.ucr.if0009.insecurepay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.ucr.if0009.insecurepay.model.CuentaBancaria;
import cr.ac.ucr.if0009.insecurepay.model.Transaccion;
import cr.ac.ucr.if0009.insecurepay.repository.CuentaRepository;
import cr.ac.ucr.if0009.insecurepay.repository.TransaccionRepository;
import cr.ac.ucr.if0009.insecurepay.repository.UsuarioRepository;
import cr.ac.ucr.if0009.insecurepay.util.JwtUtil;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InsecurePaySecurityTest {

    private static final long CUENTA_CARLOS = 2L;
    private static final long CUENTA_MARIA = 3L;
    private static final String IBAN_CARLOS = "CR05015202001026458901";
    private static final String IBAN_MARIA = "CR05015202001026458902";
    private static final String PAYLOAD_SQLI = "' OR '1'='1";
    private static final String PAYLOAD_XSS =
            "Pago de Servicios <img src=\"x\" onerror=\"alert(localStorage.getItem('insecure_pay_user'))\">";

    @DynamicPropertySource
    static void secretoJwt(DynamicPropertyRegistry registry) {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        String secreto = Base64.getEncoder().encodeToString(bytes);
        registry.add("jwt.secret", () -> secreto);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String login(String username, String password) throws Exception {
        String cuerpo = mapper.writeValueAsString(Map.of("username", username, "password", password));
        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(respuesta).get("token").asText();
    }

    private String tokenCarlos() throws Exception {
        return login("carlos_mora", "carlos123");
    }

    @Test
    void loginValidoConBcryptEmiteToken() throws Exception {
        assertThat(tokenCarlos()).startsWith("Bearer ey");
    }

    @Test
    void loginInvalidoRetornaProblemDetailGenericoSinEnumerarUsuarios() throws Exception {
        String incorrecta = mapper.writeValueAsString(Map.of("username", "carlos_mora", "password", "incorrecta"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(incorrecta))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Credenciales invalidas."))
                .andExpect(jsonPath("$.trace").doesNotExist());

        String inexistente = mapper.writeValueAsString(Map.of("username", "usuario_fantasma", "password", "x"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(inexistente))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales invalidas."));
    }

    @Test
    void contrasenasPersistidasConBcryptFactorMinimoDiez() {
        assertThat(usuarioRepository.findAll()).hasSize(5);
        usuarioRepository.findAll().forEach(u ->
                assertThat(u.getPassword()).matches("^\\$2[aby]\\$(1[0-9]|2[0-9]|3[01])\\$.{53}$"));
        String hashCarlos = usuarioRepository.findByUsername("carlos_mora").orElseThrow().getPassword();
        assertThat(passwordEncoder.matches("carlos123", hashCarlos)).isTrue();
        assertThat(hashCarlos).doesNotContain("carlos123");
    }

    @Test
    void busquedaEsInmuneAInyeccionSql() throws Exception {
        String token = tokenCarlos();
        mvc.perform(get("/api/transacciones/buscar").param("termino", PAYLOAD_SQLI).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/api/transacciones/buscar").param("termino", "' OR 1=1 --").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/api/transacciones/buscar")
                        .param("termino", "zzz' UNION SELECT id, 1, 1, 0, username || ':' || password, CURRENT_TIMESTAMP, rol FROM USUARIOS --")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void busquedaLegitimaSoloRetornaTransaccionesPropias() throws Exception {
        String token = tokenCarlos();
        mvc.perform(get("/api/transacciones/buscar").param("termino", "COMPLETADA").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].conceptoHtml", not(org.hamcrest.Matchers.hasItem(containsString("alquiler")))));

        mvc.perform(get("/api/transacciones/buscar").param("termino", "pago").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].conceptoHtml", everyItem(containsString("Pago"))));
    }

    @Test
    void endpointsSinTokenRetornan401() throws Exception {
        mvc.perform(get("/api/transacciones/buscar").param("termino", PAYLOAD_SQLI))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cuentas"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/transacciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listadoDeCuentasSoloIncluyeLasPropias() throws Exception {
        mvc.perform(get("/api/cuentas").header("Authorization", tokenCarlos()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeroCuenta", is(IBAN_CARLOS)));
    }

    @Test
    void consultaDeCuentaAjenaRetorna403() throws Exception {
        String token = tokenCarlos();
        mvc.perform(get("/api/cuentas/" + CUENTA_MARIA).header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.saldo").doesNotExist());
        mvc.perform(get("/api/cuentas/" + CUENTA_MARIA + "/saldo").header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/cuentas/usuario/4").header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/transacciones/cuenta/" + CUENTA_MARIA).header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultaDeCuentaPropiaRetorna200() throws Exception {
        mvc.perform(get("/api/cuentas/" + CUENTA_CARLOS + "/saldo").header("Authorization", tokenCarlos()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta").value(IBAN_CARLOS));
    }

    @Test
    void auditorPuedeConsultarCuentasDeTerceros() throws Exception {
        String token = login("auditor_seguridad", "auditor2026");
        mvc.perform(get("/api/cuentas/" + CUENTA_MARIA + "/saldo").header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void transferenciaDesdeCuentaAjenaRetorna403YNoDebita() throws Exception {
        BigDecimal saldoAntes = cuentaRepository.findById(CUENTA_MARIA).map(CuentaBancaria::getSaldo).orElseThrow();
        String cuerpo = mapper.writeValueAsString(Map.of(
                "cuentaOrigenId", CUENTA_MARIA,
                "numeroCuentaDestino", IBAN_CARLOS,
                "monto", 500000,
                "conceptoHtml", "Transferencia fraudulenta"));
        mvc.perform(post("/api/transacciones/transferir")
                        .header("Authorization", tokenCarlos())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
        BigDecimal saldoDespues = cuentaRepository.findById(CUENTA_MARIA).map(CuentaBancaria::getSaldo).orElseThrow();
        assertThat(saldoDespues).isEqualByComparingTo(saldoAntes);
    }

    @Test
    void transaccionAjenaRetorna403() throws Exception {
        mvc.perform(get("/api/transacciones/2").header("Authorization", tokenCarlos()))
                .andExpect(status().isForbidden());
    }

    @Test
    void conceptoConHtmlSePersisteEscapado() throws Exception {
        String cuerpo = mapper.writeValueAsString(Map.of(
                "cuentaOrigenId", CUENTA_CARLOS,
                "numeroCuentaDestino", IBAN_MARIA,
                "monto", 1000,
                "conceptoHtml", PAYLOAD_XSS));
        String respuesta = mvc.perform(post("/api/transacciones/transferir")
                        .header("Authorization", tokenCarlos())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(respuesta).get("id").asLong();
        Transaccion tx = transaccionRepository.findById(id).orElseThrow();
        assertThat(tx.getConceptoHtml()).doesNotContain("<", ">", "\"");
        assertThat(tx.getConceptoHtml()).contains("&lt;img src=&quot;x&quot;");
    }

    @Test
    void erroresNoExponenTrazasNiDetallesInternos() throws Exception {
        String token = tokenCarlos();
        mvc.perform(get("/api/cuentas/abc").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("java."))))
                .andExpect(content().string(not(containsString("NumberFormatException"))));

        mvc.perform(get("/api/cuentas/999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cuenta bancaria no existe"));

        mvc.perform(post("/api/transacciones/transferir")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ json roto"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Jackson"))));
    }

    @Test
    void secretoJwtCortoOAusenteEsRechazado() {
        assertThatThrownBy(() -> new JwtUtil("ClaveSecretaSuperDebil", 1000))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtUtil("", 1000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tokenFirmadoConLaClaveFiltradaEsRechazado() throws Exception {
        JwtUtil atacante = new JwtUtil("ClaveSecretaSuperDebilInsecurePayBanco2026", 3600000);
        String falso = "Bearer " + atacante.generateToken("maria_rojas", "CLIENTE", 4L);
        mvc.perform(get("/api/cuentas").header("Authorization", falso))
                .andExpect(status().isUnauthorized());
    }
}
