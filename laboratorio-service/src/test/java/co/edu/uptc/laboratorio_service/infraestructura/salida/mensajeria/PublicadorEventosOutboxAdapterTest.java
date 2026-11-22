package co.edu.uptc.laboratorio_service.infraestructura.salida.mensajeria;

import co.edu.uptc.laboratorio_service.aplicacion.EventosLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorioFactory;
import co.edu.uptc.laboratorio_service.dominio.ResultadoExamen;
import co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia.OutboxEventEntity;
import co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El payload que se guarda en el outbox ES el contrato con los consumidores (p. ej.
 * consultas_admisiones_service), por eso se prueba campo por campo. Sin Spring ni base de datos:
 * el repositorio es un proxy que solo captura lo que se guarda.
 */
class PublicadorEventosOutboxAdapterTest {

    private final List<OutboxEventEntity> guardados = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final PublicadorEventosOutboxAdapter adaptador =
            new PublicadorEventosOutboxAdapter(repositorioQueCapturaLoGuardado(), mapper);
    private final OrdenLaboratorioFactory factory = new OrdenLaboratorioFactory();

    @Test
    void examenSolicitadoSePublicaSinResultado() throws Exception {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");

        adaptador.publicar(EventosLaboratorio.EXAMEN_SOLICITADO, orden);

        OutboxEventEntity fila = unicaFila();
        JsonNode json = mapper.readTree(fila.getPayload());
        assertEquals("ExamenSolicitado", fila.getTipo());
        assertEquals("ExamenSolicitado", json.get("tipo").asText());
        assertEquals(orden.getId().toString(), json.get("ordenId").asText());
        assertEquals("PAC-1001", json.get("pacienteId").asText());
        assertEquals("Hemograma", json.get("tipoExamen").asText());
        assertEquals("SOLICITADA", json.get("estado").asText());
        assertTrue(json.get("resultadoValor").isNull());
        assertTrue(json.get("resultadoUnidad").isNull());
    }

    @Test
    void resultadoRegistradoIncluyeValorYUnidad() throws Exception {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");
        orden.registrarResultado(new ResultadoExamen("120", "g/L"));

        adaptador.publicar(EventosLaboratorio.RESULTADO_REGISTRADO, orden);

        JsonNode json = mapper.readTree(unicaFila().getPayload());
        assertEquals("ResultadoRegistrado", json.get("tipo").asText());
        assertEquals("CON_RESULTADO", json.get("estado").asText());
        assertEquals("120", json.get("resultadoValor").asText());
        assertEquals("g/L", json.get("resultadoUnidad").asText());
    }

    @Test
    void ordenCerradaConservaElResultadoYMarcaElEstadoCerrada() throws Exception {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");
        orden.registrarResultado(new ResultadoExamen("120", "g/L"));
        orden.cerrar();

        adaptador.publicar(EventosLaboratorio.ORDEN_CERRADA, orden);

        JsonNode json = mapper.readTree(unicaFila().getPayload());
        assertEquals("OrdenCerrada", json.get("tipo").asText());
        assertEquals("CERRADA", json.get("estado").asText());
        assertEquals("120", json.get("resultadoValor").asText());
    }

    @Test
    void laClaveDeParticionEsElIdDeLaOrdenParaPreservarElOrdenPorOrden() {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");

        adaptador.publicar(EventosLaboratorio.EXAMEN_SOLICITADO, orden);

        assertEquals(orden.getId().toString(), unicaFila().getClave());
    }

    @Test
    void cadaEventoTieneUnEventoIdUuidUnicoQueCoincideConElPayload() throws Exception {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");

        adaptador.publicar(EventosLaboratorio.EXAMEN_SOLICITADO, orden);
        adaptador.publicar(EventosLaboratorio.EXAMEN_SOLICITADO, orden);

        assertEquals(2, guardados.size());
        OutboxEventEntity primera = guardados.get(0);
        OutboxEventEntity segunda = guardados.get(1);
        assertNotEquals(primera.getEventoId(), segunda.getEventoId());
        assertEquals(primera.getEventoId(), UUID.fromString(primera.getEventoId()).toString());
        assertEquals(primera.getEventoId(), mapper.readTree(primera.getPayload()).get("eventoId").asText());
    }

    @Test
    void lasFechasViajanComoTextoIsoQueSePuedeLeerDeVuelta() throws Exception {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");

        adaptador.publicar(EventosLaboratorio.EXAMEN_SOLICITADO, orden);

        JsonNode json = mapper.readTree(unicaFila().getPayload());
        assertTrue(json.get("fechaSolicitud").isTextual());
        assertEquals(orden.getFechaSolicitud(), LocalDateTime.parse(json.get("fechaSolicitud").asText()));
        assertTrue(json.get("ocurridoEn").isTextual());
        LocalDateTime.parse(json.get("ocurridoEn").asText()); // no debe lanzar
    }

    @Test
    void elEventoSeGuardaPendienteDePublicar() {
        adaptador.publicar(EventosLaboratorio.EXAMEN_SOLICITADO, factory.crear("PAC-1001", "Hemograma"));

        assertFalse(unicaFila().isPublicado());
    }

    @Test
    void elPayloadExponeExactamenteLosCamposDelContrato() throws Exception {
        adaptador.publicar(EventosLaboratorio.EXAMEN_SOLICITADO, factory.crear("PAC-1001", "Hemograma"));

        Set<String> campos = new TreeSet<>();
        mapper.readTree(unicaFila().getPayload()).fieldNames().forEachRemaining(campos::add);

        // Si cambia esta lista hay que avisar a los consumidores del topic laboratorio.ordenes.
        assertEquals(new TreeSet<>(Set.of(
                "eventoId", "tipo", "ocurridoEn", "ordenId", "pacienteId", "tipoExamen",
                "estado", "resultadoValor", "resultadoUnidad", "fechaSolicitud")), campos);
    }

    private OutboxEventEntity unicaFila() {
        assertEquals(1, guardados.size());
        return guardados.get(0);
    }

    /** Repositorio de mentira sin Mockito: solo `save` hace algo; cualquier otra llamada es un error. */
    private OutboxEventRepository repositorioQueCapturaLoGuardado() {
        return (OutboxEventRepository) Proxy.newProxyInstance(
                OutboxEventRepository.class.getClassLoader(),
                new Class<?>[]{OutboxEventRepository.class},
                (proxy, metodo, args) -> {
                    if (metodo.getDeclaringClass() == Object.class) {
                        return switch (metodo.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> "repositorioOutboxFalso";
                        };
                    }
                    if (metodo.getName().equals("save")) {
                        guardados.add((OutboxEventEntity) args[0]);
                        return args[0];
                    }
                    throw new UnsupportedOperationException(metodo.getName());
                });
    }
}
