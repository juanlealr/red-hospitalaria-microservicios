package co.edu.uptc.consultas_admisiones_service.dominio.proyeccion;

import co.edu.uptc.consultas_admisiones_service.dominio.modelo.FichaAdmision;
import co.edu.uptc.consultas_admisiones_service.dominio.repositorio.FichaAdmisionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba la proyección CQRS sin Mongo ni Kafka: el repositorio es un proxy en memoria. */
class ProyectorFichasAdmisionTest {

    private static final String INGRESO = """
            {"admisionId":"A-1","pacienteId":"PAC-1001","sedeInicialId":"TUNJA","servicioInicial":"URGENCIAS",
             "medicoTratante":"Dr. Prueba","motivoIngreso":"Control","fechaIngreso":"2026-11-21T10:00:00"}""";

    private final Map<String, FichaAdmision> almacen = new HashMap<>();
    private ProyectorFichasAdmision proyector;

    @BeforeEach
    void preparar() {
        FichaAdmisionRepository repositorio = (FichaAdmisionRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { FichaAdmisionRepository.class },
                (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "findById" -> Optional.ofNullable(almacen.get((String) args[0]));
                    case "save" -> {
                        FichaAdmision ficha = (FichaAdmision) args[0];
                        almacen.put(ficha.getAdmisionId(), ficha);
                        yield ficha;
                    }
                    case "toString" -> "RepositorioEnMemoria";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
        proyector = new ProyectorFichasAdmision(repositorio, new ObjectMapper());
    }

    @Test
    void elEventoDeIngresoCreaLaFichaActiva() {
        proyector.alIngresarPaciente(INGRESO);

        FichaAdmision ficha = almacen.get("A-1");
        assertNotNull(ficha);
        assertEquals("PAC-1001", ficha.getPacienteId());
        assertEquals("ACTIVA", ficha.getEstado());
        assertEquals("TUNJA", ficha.getSedeActualId());
        assertEquals("Dr. Prueba", ficha.getMedicoTratante());
    }

    @Test
    void elTrasladoActualizaLaUbicacionYNoSeDuplicaAlReentregarse() {
        proyector.alIngresarPaciente(INGRESO);
        String traslado = """
                {"admisionId":"A-1","pacienteId":"PAC-1001","sedeOrigenId":"TUNJA","servicioOrigen":"URGENCIAS",
                 "sedeDestinoId":"DUITAMA","servicioDestino":"UCI","fechaTraslado":"2026-11-21T11:00:00"}""";

        proyector.alTrasladarPaciente(traslado);
        proyector.alTrasladarPaciente(traslado);

        FichaAdmision ficha = almacen.get("A-1");
        assertEquals("DUITAMA", ficha.getSedeActualId());
        assertEquals(1, ficha.getTotalTraslados());
    }

    @Test
    void laCoberturaYElAltaSeProyectan() {
        proyector.alIngresarPaciente(INGRESO);

        proyector.alVerificarCobertura(
                "{\"admisionId\":\"A-1\",\"pacienteId\":\"PAC-1001\",\"coberturaPorcentaje\":80}");
        proyector.alDarDeAltaPaciente(
                "{\"admisionId\":\"A-1\",\"pacienteId\":\"PAC-1001\",\"fechaAlta\":\"2026-11-22T09:00:00\"}");

        FichaAdmision ficha = almacen.get("A-1");
        assertEquals(80, ficha.getCoberturaPorcentaje());
        assertEquals("DADA_DE_ALTA", ficha.getEstado());
        assertTrue(ficha.getFechaAlta().startsWith("2026-11-22"));
    }

    @Test
    void unMensajeMalformadoNoDetieneElConsumidor() {
        assertDoesNotThrow(() -> proyector.alIngresarPaciente("esto no es json"));
        assertTrue(almacen.isEmpty());
    }
}
