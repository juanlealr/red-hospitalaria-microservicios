package co.edu.uptc.laboratorio_service.infraestructura.seguridad;

import co.edu.uptc.laboratorio_service.aplicacion.OrdenLaboratorioUseCase;
import co.edu.uptc.laboratorio_service.compartido.GlobalExceptionHandler;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorioFactory;
import co.edu.uptc.laboratorio_service.dominio.ResultadoExamen;
import co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador.OrdenLaboratorioController;
import co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador.StatusController;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz rol x endpoint contra la SecurityConfig REAL, con tokens JWT HS256 firmados igual que
 * auth_service (claim "rol", mismo secreto). No necesita base de datos ni Kafka: el caso de uso
 * es un mock. Debe mantenerse alineado con el gateway: /api/laboratorio/** -> MEDICO, LABORATORIO.
 */
@SpringJUnitWebConfig(SeguridadRolesTest.Config.class)
@TestPropertySource(properties = "seguridad.jwt.secret=" + SeguridadRolesTest.SECRETO)
class SeguridadRolesTest {

    static final String SECRETO = "secreto-de-prueba-con-mas-de-treinta-y-dos-bytes-0123456789";
    private static final String OTRO_SECRETO = "otro-secreto-distinto-con-mas-de-treinta-y-dos-bytes-9876543210";

    private static final List<String> ROLES =
            List.of("ADMISIONES", "MEDICO", "LABORATORIO", "FARMACEUTICO", "ADMINISTRADOR");

    private static final String BASE = "/api/laboratorio/ordenes";
    private static final UUID ID = UUID.randomUUID();
    private static final String CUERPO_SOLICITUD = "{\"pacienteId\":\"PAC-1001\",\"tipoExamen\":\"Hemograma\"}";
    private static final String CUERPO_RESULTADO = "{\"valor\":\"120\",\"unidad\":\"g/L\"}";

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, ManejadorErroresSeguridad.class})
    static class Config {

        @Bean
        OrdenLaboratorioUseCase ordenLaboratorioUseCase() {
            return mock(OrdenLaboratorioUseCase.class);
        }

        @Bean
        OrdenLaboratorioController ordenLaboratorioController(OrdenLaboratorioUseCase useCase) {
            return new OrdenLaboratorioController(useCase);
        }

        @Bean
        StatusController statusController() {
            return new StatusController();
        }

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private OrdenLaboratorioUseCase useCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(useCase);
        OrdenLaboratorio solicitada = new OrdenLaboratorioFactory().crear("PAC-1001", "Hemograma");
        OrdenLaboratorio conResultado = new OrdenLaboratorioFactory().crear("PAC-1001", "Hemograma");
        conResultado.registrarResultado(new ResultadoExamen("120", "g/L"));

        when(useCase.solicitarExamen(anyString(), anyString())).thenReturn(solicitada);
        when(useCase.registrarResultado(any(UUID.class), anyString(), anyString())).thenReturn(conResultado);
        when(useCase.buscarPorId(any(UUID.class))).thenReturn(solicitada);
        when(useCase.listarOrdenes(any())).thenReturn(List.of(solicitada));
        when(useCase.tieneOrdenesPendientes(anyString())).thenReturn(true);

        mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(contexto.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    // ---------- matriz de roles ----------

    @Test
    void soloElMedicoPuedeSolicitarExamenes() throws Exception {
        verificarRoles(HttpMethod.POST, BASE, CUERPO_SOLICITUD, 201, "MEDICO");
    }

    @Test
    void soloElLaboratorioPuedeRegistrarResultados() throws Exception {
        verificarRoles(HttpMethod.POST, BASE + "/" + ID + "/resultado", CUERPO_RESULTADO, 200, "LABORATORIO");
    }

    @Test
    void soloElLaboratorioPuedeCerrarOrdenes() throws Exception {
        verificarRoles(HttpMethod.PUT, BASE + "/" + ID + "/cierre", null, 204, "LABORATORIO");
    }

    @Test
    void medicoYLaboratorioPuedenListarOrdenes() throws Exception {
        verificarRoles(HttpMethod.GET, BASE, null, 200, "MEDICO", "LABORATORIO");
    }

    @Test
    void medicoYLaboratorioPuedenConsultarUnaOrden() throws Exception {
        verificarRoles(HttpMethod.GET, BASE + "/" + ID, null, 200, "MEDICO", "LABORATORIO");
    }

    @Test
    void medicoYLaboratorioPuedenConsultarPendientes() throws Exception {
        verificarRoles(HttpMethod.GET, BASE + "/pendientes/PAC-1001", null, 200, "MEDICO", "LABORATORIO");
    }

    // ---------- autenticación ----------

    @Test
    void statusEsPublico() throws Exception {
        assertEquals(200, estado(HttpMethod.GET, "/api/status", null, null));
    }

    @Test
    void sinTokenSeResponde401ConElFormatoDeErrorDelProyecto() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void conUnRolSinPermisoSeResponde403ConElFormatoDeErrorDelProyecto() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE)
                        .header("Authorization", "Bearer " + token("ADMINISTRADOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void unTokenBasuraSeRechazaConUn401() throws Exception {
        assertEquals(401, estado(HttpMethod.GET, BASE, null, "esto.no.es-un-jwt"));
    }

    @Test
    void unTokenFirmadoConOtroSecretoSeRechaza() throws Exception {
        String falso = jwt(OTRO_SECRETO, "MEDICO", Instant.now().plusSeconds(3600));

        assertEquals(401, estado(HttpMethod.GET, BASE, null, falso));
    }

    @Test
    void unTokenExpiradoSeRechaza() throws Exception {
        String expirado = jwt(SECRETO, "MEDICO", Instant.now().minusSeconds(3600));

        assertEquals(401, estado(HttpMethod.GET, BASE, null, expirado));
    }

    @Test
    void unTokenSinFirmaConAlgNoneSeRechaza() throws Exception {
        String cabecera = base64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String cuerpo = base64("{\"sub\":\"x@hospital.test\",\"rol\":\"MEDICO\",\"exp\":"
                + Instant.now().plusSeconds(3600).getEpochSecond() + "}");

        assertEquals(401, estado(HttpMethod.GET, BASE, null, cabecera + "." + cuerpo + "."));
    }

    @Test
    void unTokenValidoSinClaimDeRolNoTieneAcceso() throws Exception {
        String sinRol = jwt(SECRETO, null, Instant.now().plusSeconds(3600));

        assertEquals(403, estado(HttpMethod.GET, BASE, null, sinRol));
    }

    // ---------- utilidades ----------

    /** Cada rol del sistema debe recibir exito solo si esta en la lista; los demas 403; sin token 401. */
    private void verificarRoles(HttpMethod metodo, String ruta, String cuerpo, int exitoEsperado,
                                String... rolesPermitidos) throws Exception {
        List<String> permitidos = List.of(rolesPermitidos);
        for (String rol : ROLES) {
            int esperado = permitidos.contains(rol) ? exitoEsperado : 403;
            assertEquals(esperado, estado(metodo, ruta, cuerpo, token(rol)),
                    metodo + " " + ruta + " con rol " + rol);
        }
        assertEquals(401, estado(metodo, ruta, cuerpo, null), metodo + " " + ruta + " sin token");
    }

    private int estado(HttpMethod metodo, String ruta, String cuerpo, String token) throws Exception {
        var peticion = MockMvcRequestBuilders.request(metodo, ruta);
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mockMvc.perform(peticion).andReturn().getResponse().getStatus();
    }

    private static String token(String rol) {
        return jwt(SECRETO, rol, Instant.now().plusSeconds(3600));
    }

    /** JWT HS256 con la misma forma que genera auth_service: sub = email, claim "rol", iat, exp. */
    private static String jwt(String secreto, String rol, Instant expira) {
        try {
            String cabecera = base64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
            String claimRol = rol == null ? "" : ",\"rol\":\"" + rol + "\"";
            String cuerpo = base64("{\"sub\":\"usuario@hospital.test\"" + claimRol
                    + ",\"iat\":" + Instant.now().getEpochSecond()
                    + ",\"exp\":" + expira.getEpochSecond() + "}");

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String firma = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((cabecera + "." + cuerpo).getBytes(StandardCharsets.UTF_8)));
            return cabecera + "." + cuerpo + "." + firma;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token de prueba", e);
        }
    }

    private static String base64(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }
}
