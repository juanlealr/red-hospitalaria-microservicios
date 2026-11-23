package co.edu.uptc.farmacia_service.infraestructura.seguridad;

import co.edu.uptc.farmacia_service.aplicacion.SolicitudMedicamentoFactory;
import co.edu.uptc.farmacia_service.aplicacion.SolicitudMedicamentoUseCase;
import co.edu.uptc.farmacia_service.dominio.Cobertura;
import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import co.edu.uptc.farmacia_service.infraestructura.entrada.rest.FarmaciaExceptionHandler;
import co.edu.uptc.farmacia_service.infraestructura.entrada.rest.SolicitudMedicamentoController;
import co.edu.uptc.farmacia_service.infraestructura.entrada.rest.SolicitudMedicamentoRestMapper;
import co.edu.uptc.farmacia_service.infraestructura.entrada.rest.StatusController;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * es un mock. Debe mantenerse alineado con el gateway: /api/farmacia/** -> FARMACEUTICO,
 * ADMISIONES, MEDICO; el despacho es exclusivo de FARMACEUTICO.
 */
@SpringJUnitWebConfig(SeguridadRolesTest.Config.class)
@TestPropertySource(properties = "seguridad.jwt.secret=" + SeguridadRolesTest.SECRETO)
class SeguridadRolesTest {

    static final String SECRETO = "secreto-de-prueba-con-mas-de-treinta-y-dos-bytes-0123456789";
    private static final String OTRO_SECRETO = "otro-secreto-distinto-con-mas-de-treinta-y-dos-bytes-9876543210";

    private static final List<String> ROLES =
            List.of("ADMISIONES", "MEDICO", "LABORATORIO", "FARMACEUTICO", "ADMINISTRADOR");

    private static final String BASE = "/api/farmacia/solicitudes";
    private static final String ID = "sol-1001";
    private static final String CUERPO_SOLICITUD =
            "{\"pacienteId\":\"PAC-1001\",\"medicoId\":\"MED-7\",\"medicamento\":\"Ibuprofeno 400 mg\"}";

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, ManejadorErroresSeguridad.class})
    static class Config {

        @Bean
        SolicitudMedicamentoUseCase solicitudMedicamentoUseCase() {
            return mock(SolicitudMedicamentoUseCase.class);
        }

        @Bean
        SolicitudMedicamentoRestMapper solicitudMedicamentoRestMapper() {
            return new SolicitudMedicamentoRestMapper();
        }

        @Bean
        SolicitudMedicamentoController solicitudMedicamentoController(
                SolicitudMedicamentoUseCase useCase, SolicitudMedicamentoRestMapper mapper) {
            return new SolicitudMedicamentoController(useCase, mapper);
        }

        @Bean
        StatusController statusController() {
            return new StatusController();
        }

        @Bean
        FarmaciaExceptionHandler farmaciaExceptionHandler() {
            return new FarmaciaExceptionHandler();
        }
    }

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private SolicitudMedicamentoUseCase useCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(useCase);
        SolicitudMedicamento recibida =
                new SolicitudMedicamentoFactory().recibirSolicitud("PAC-1001", "MED-7", "Ibuprofeno 400 mg");
        SolicitudMedicamento verificada =
                new SolicitudMedicamentoFactory().recibirSolicitud("PAC-1001", "MED-7", "Ibuprofeno 400 mg");
        verificada.registrarCobertura(new Cobertura(80));
        SolicitudMedicamento despachada =
                new SolicitudMedicamentoFactory().recibirSolicitud("PAC-1001", "MED-7", "Ibuprofeno 400 mg");
        despachada.registrarCobertura(new Cobertura(80));
        despachada.despachar(java.time.LocalDateTime.now());

        when(useCase.recibir(anyString(), anyString(), anyString())).thenReturn(recibida);
        when(useCase.verificarCobertura(anyString())).thenReturn(verificada);
        when(useCase.despachar(anyString())).thenReturn(despachada);
        when(useCase.buscarPorId(anyString())).thenReturn(recibida);
        when(useCase.listarTodas()).thenReturn(List.of(recibida));

        mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(contexto.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    // ---------- matriz de roles ----------

    @Test
    void medicoYFarmaceuticoPuedenCrearSolicitudes() throws Exception {
        verificarRoles(HttpMethod.POST, BASE, CUERPO_SOLICITUD, 201, "MEDICO", "FARMACEUTICO");
    }

    @Test
    void soloElFarmaceuticoPuedeDespachar() throws Exception {
        verificarRoles(HttpMethod.POST, BASE + "/" + ID + "/despachar", null, 200, "FARMACEUTICO");
    }

    @Test
    void medicoYFarmaceuticoPuedenVerificarCobertura() throws Exception {
        verificarRoles(HttpMethod.POST, BASE + "/" + ID + "/verificar-cobertura", null, 200,
                "MEDICO", "FARMACEUTICO");
    }

    @Test
    void losTresRolesDelGatewayPuedenListar() throws Exception {
        verificarRoles(HttpMethod.GET, BASE, null, 200, "FARMACEUTICO", "ADMISIONES", "MEDICO");
    }

    @Test
    void losTresRolesDelGatewayPuedenConsultarUnaSolicitud() throws Exception {
        verificarRoles(HttpMethod.GET, BASE + "/" + ID, null, 200, "FARMACEUTICO", "ADMISIONES", "MEDICO");
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
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void sinTokenNoSePuedeEscribirUnaSolicitud() throws Exception {
        assertEquals(401, estado(HttpMethod.POST, BASE, CUERPO_SOLICITUD, null));
    }

    @Test
    void conUnRolSinPermisoSeResponde403ConElFormatoDeErrorDelProyecto() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BASE)
                        .header("Authorization", "Bearer " + token("LABORATORIO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void unTokenBasuraSeRechazaConUn401() throws Exception {
        assertEquals(401, estado(HttpMethod.GET, BASE, null, "esto.no.es-un-jwt"));
    }

    @Test
    void unTokenFirmadoConOtroSecretoSeRechaza() throws Exception {
        String falso = jwt(OTRO_SECRETO, "FARMACEUTICO", Instant.now().plusSeconds(3600));

        assertEquals(401, estado(HttpMethod.GET, BASE, null, falso));
    }

    @Test
    void unTokenExpiradoSeRechaza() throws Exception {
        String expirado = jwt(SECRETO, "FARMACEUTICO", Instant.now().minusSeconds(3600));

        assertEquals(401, estado(HttpMethod.GET, BASE, null, expirado));
    }

    @Test
    void unTokenSinFirmaConAlgNoneSeRechaza() throws Exception {
        String cabecera = base64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String cuerpo = base64("{\"sub\":\"x@hospital.test\",\"rol\":\"FARMACEUTICO\",\"exp\":"
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
