package co.edu.uptc.farmacia_service.infraestructura.entrada.rest;

import co.edu.uptc.farmacia_service.aplicacion.SolicitudMedicamentoUseCase;
import co.edu.uptc.farmacia_service.compartido.ServicioExternoNoDisponibleException;
import co.edu.uptc.farmacia_service.dominio.Cobertura;
import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitudMedicamentoControllerTest {

    private SolicitudMedicamentoUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(SolicitudMedicamentoUseCase.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SolicitudMedicamentoController(
                        useCase, new SolicitudMedicamentoRestMapper()))
                .setValidator(validator)
                .setControllerAdvice(new FarmaciaExceptionHandler())
                .build();
    }

    @Test
    void recibeUnaSolicitudYDevuelve201() throws Exception {
        var solicitud = solicitudRecibida();
        when(useCase.recibir("pac-1", "med-1", "Ibuprofeno 400 mg")).thenReturn(solicitud);

        mockMvc.perform(post("/api/farmacia/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pacienteId":"pac-1","medicoId":"med-1","medicamento":"Ibuprofeno 400 mg"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("sol-1"))
                .andExpect(jsonPath("$.estado").value("RECIBIDA"));
    }

    @Test
    void rechazaUnaSolicitudSinMedicamentoCon400() throws Exception {
        mockMvc.perform(post("/api/farmacia/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pacienteId":"pac-1","medicoId":"med-1","medicamento":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificaLaCoberturaYDevuelveLaSolicitudActualizada() throws Exception {
        var solicitud = solicitudRecibida();
        solicitud.registrarCobertura(new Cobertura(80));
        when(useCase.verificarCobertura("sol-1")).thenReturn(solicitud);

        mockMvc.perform(post("/api/farmacia/solicitudes/sol-1/verificar-cobertura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COBERTURA_VERIFICADA"))
                .andExpect(jsonPath("$.cobertura").value(80));
    }

    @Test
    void devuelve404CuandoLaSolicitudNoExiste() throws Exception {
        when(useCase.buscarPorId("no-existe"))
                .thenThrow(new NoSuchElementException("No existe la solicitud"));

        mockMvc.perform(get("/api/farmacia/solicitudes/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void despacharSinCoberturaDevuelve409() throws Exception {
        when(useCase.despachar("sol-1"))
                .thenThrow(new IllegalStateException("No se puede despachar sin cobertura verificada"));

        mockMvc.perform(post("/api/farmacia/solicitudes/sol-1/despachar"))
                .andExpect(status().isConflict());
    }

    @Test
    void aseguradoraCaidaDevuelve503() throws Exception {
        when(useCase.verificarCobertura("sol-1"))
                .thenThrow(new ServicioExternoNoDisponibleException("aseguradora no disponible", null));

        mockMvc.perform(post("/api/farmacia/solicitudes/sol-1/verificar-cobertura"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void listaSolicitudes() throws Exception {
        when(useCase.listarTodas()).thenReturn(List.of(solicitudRecibida()));

        mockMvc.perform(get("/api/farmacia/solicitudes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("sol-1"));
    }

    private SolicitudMedicamento solicitudRecibida() {
        return new SolicitudMedicamento(
                "sol-1", "pac-1", "med-1", "Ibuprofeno 400 mg",
                LocalDateTime.of(2026, 10, 20, 8, 0));
    }
}
