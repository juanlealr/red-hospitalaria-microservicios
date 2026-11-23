package co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador;

import co.edu.uptc.laboratorio_service.aplicacion.OrdenLaboratorioUseCase;
import co.edu.uptc.laboratorio_service.compartido.GlobalExceptionHandler;
import co.edu.uptc.laboratorio_service.compartido.RecursoNoEncontradoException;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorioFactory;
import co.edu.uptc.laboratorio_service.dominio.ResultadoExamen;
import co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia.OrdenLaboratorioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controlador REST + GlobalExceptionHandler sin levantar Spring completo (MockMvc standalone,
 * mismo estilo que el test del controlador de farmacia-service). La seguridad se prueba aparte
 * en SeguridadRolesTest.
 */
class OrdenLaboratorioControllerTest {

    private static final String BASE = "/api/laboratorio/ordenes";

    private final OrdenLaboratorioFactory factory = new OrdenLaboratorioFactory();
    private OrdenLaboratorioUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(OrdenLaboratorioUseCase.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrdenLaboratorioController(useCase))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------- HU-04: solicitar examen ----------

    @Test
    void solicitarExamenDevuelve201ConLaOrdenSolicitada() throws Exception {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");
        when(useCase.solicitarExamen("PAC-1001", "Hemograma")).thenReturn(orden);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"PAC-1001\",\"tipoExamen\":\"Hemograma\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orden.getId().toString()))
                .andExpect(jsonPath("$.pacienteId").value("PAC-1001"))
                .andExpect(jsonPath("$.tipoExamen").value("Hemograma"))
                .andExpect(jsonPath("$.estado").value("SOLICITADA"))
                .andExpect(jsonPath("$.pendiente").value(true))
                .andExpect(jsonPath("$.resultado").doesNotExist());
    }

    @Test
    void solicitarExamenSinPacienteDevuelve400ConElMensajeDeValidacion() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\" \",\"tipoExamen\":\"Hemograma\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("El id del paciente es obligatorio"));

        verifyNoInteractions(useCase);
    }

    @Test
    void solicitarExamenSinTipoDeExamenDevuelve400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"PAC-1001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El tipo de examen es obligatorio"));

        verifyNoInteractions(useCase);
    }

    @Test
    void solicitarExamenConTipoMuyLargoDevuelve400() throws Exception {
        String tipoLargo = "x".repeat(151);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"PAC-1001\",\"tipoExamen\":\"" + tipoLargo + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase);
    }

    @Test
    void solicitarExamenConPacienteMuyLargoDevuelve400EnVezDeUnErrorDeBaseDeDatos() throws Exception {
        String pacienteLargo = "P".repeat(256);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":\"" + pacienteLargo + "\",\"tipoExamen\":\"Hemograma\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase);
    }

    @Test
    void unJsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(useCase);
    }

    // ---------- HU-05: registrar resultado ----------

    @Test
    void registrarResultadoDevuelve200ConElResultado() throws Exception {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");
        orden.registrarResultado(new ResultadoExamen("120", "g/L"));
        when(useCase.registrarResultado(orden.getId(), "120", "g/L")).thenReturn(orden);

        mockMvc.perform(post(BASE + "/" + orden.getId() + "/resultado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"120\",\"unidad\":\"g/L\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CON_RESULTADO"))
                .andExpect(jsonPath("$.pendiente").value(true))
                .andExpect(jsonPath("$.resultado.valor").value("120"))
                .andExpect(jsonPath("$.resultado.unidad").value("g/L"));
    }

    @Test
    void registrarResultadoSinUnidadDevuelve400() throws Exception {
        mockMvc.perform(post(BASE + "/" + UUID.randomUUID() + "/resultado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"120\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La unidad del resultado es obligatoria"));

        verifyNoInteractions(useCase);
    }

    @Test
    void registrarResultadoEnUnaOrdenCerradaDevuelve409() throws Exception {
        UUID id = UUID.randomUUID();
        when(useCase.registrarResultado(id, "120", "g/L"))
                .thenThrow(new IllegalStateException("No se puede registrar un resultado en una orden cerrada."));

        mockMvc.perform(post(BASE + "/" + id + "/resultado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"120\",\"unidad\":\"g/L\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ---------- cierre ----------

    @Test
    void cerrarDevuelve204YLlamaAlCasoDeUso() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(useCase).cerrar(id);

        mockMvc.perform(put(BASE + "/" + id + "/cierre"))
                .andExpect(status().isNoContent());

        verify(useCase).cerrar(id);
    }

    @Test
    void cerrarUnaOrdenSinResultadoDevuelve409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalStateException("No se puede cerrar una orden sin resultado registrado."))
                .when(useCase).cerrar(id);

        mockMvc.perform(put(BASE + "/" + id + "/cierre"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede cerrar una orden sin resultado registrado."));
    }

    @Test
    void unConflictoDeConcurrenciaOptimistaDevuelve409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ObjectOptimisticLockingFailureException(OrdenLaboratorioEntity.class, id))
                .when(useCase).cerrar(id);

        mockMvc.perform(put(BASE + "/" + id + "/cierre"))
                .andExpect(status().isConflict());
    }

    // ---------- consultas ----------

    @Test
    void buscarPorIdDevuelveLaOrden() throws Exception {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Glucosa");
        when(useCase.buscarPorId(orden.getId())).thenReturn(orden);

        mockMvc.perform(get(BASE + "/" + orden.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orden.getId().toString()))
                .andExpect(jsonPath("$.tipoExamen").value("Glucosa"));
    }

    @Test
    void unaOrdenInexistenteDevuelve404ConElFormatoDeErrorDelProyecto() throws Exception {
        UUID id = UUID.randomUUID();
        when(useCase.buscarPorId(id)).thenThrow(
                new RecursoNoEncontradoException("Orden de laboratorio no encontrada con ID: " + id));

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Orden de laboratorio no encontrada con ID: " + id));
    }

    @Test
    void unIdQueNoEsUuidDevuelve400() throws Exception {
        mockMvc.perform(get(BASE + "/no-es-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(useCase);
    }

    @Test
    void listarSinFiltroPideTodasLasOrdenes() throws Exception {
        OrdenLaboratorio a = factory.crear("PAC-1001", "Hemograma");
        OrdenLaboratorio b = factory.crear("PAC-2002", "Glucosa");
        when(useCase.listarOrdenes(null)).thenReturn(List.of(a, b));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteId").value("PAC-1001"))
                .andExpect(jsonPath("$[1].pacienteId").value("PAC-2002"));
    }

    @Test
    void listarConPacienteFiltraPorEsePaciente() throws Exception {
        OrdenLaboratorio a = factory.crear("PAC-1001", "Hemograma");
        when(useCase.listarOrdenes("PAC-1001")).thenReturn(List.of(a));

        mockMvc.perform(get(BASE).param("pacienteId", "PAC-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoExamen").value("Hemograma"));

        verify(useCase).listarOrdenes("PAC-1001");
    }

    @Test
    void pendientesDevuelveElMismoDatoQueConsultaAdmisionesPorGrpc() throws Exception {
        when(useCase.tieneOrdenesPendientes("PAC-1001")).thenReturn(true);
        when(useCase.tieneOrdenesPendientes("PAC-2002")).thenReturn(false);

        mockMvc.perform(get(BASE + "/pendientes/PAC-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteId").value("PAC-1001"))
                .andExpect(jsonPath("$.tienePendientes").value(true));

        mockMvc.perform(get(BASE + "/pendientes/PAC-2002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tienePendientes").value(false));
    }

    // ---------- errores inesperados ----------

    @Test
    void unErrorInesperadoDevuelve500SinFiltrarElDetalleInterno() throws Exception {
        UUID id = UUID.randomUUID();
        when(useCase.buscarPorId(id)).thenThrow(new RuntimeException("detalle interno: password=secreto"));

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor."));
    }
}
