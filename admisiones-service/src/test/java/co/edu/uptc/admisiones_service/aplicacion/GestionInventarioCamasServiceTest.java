// admisiones-service/src/test/java/co/edu/uptc/admisiones_service/aplicacion/GestionInventarioCamasServiceTest.java
package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import co.edu.uptc.admisiones_service.dominio.InventarioCamas;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GestionInventarioCamasServiceTest {

    private final RepositorioInventarioCamasFalso repositorio = new RepositorioInventarioCamasFalso();
    private final GestionInventarioCamasService service = new GestionInventarioCamasService(repositorio);

    @Test
    void registraConsultaOcupaYLiberaUnaCamaSinSpringNiBaseDeDatos() {
        service.registrarCapacidad("sede-tunja", "urgencias", 2);

        DisponibilidadCama inicial = service.consultarDisponibilidad("sede-tunja", "urgencias");
        assertEquals(2, inicial.camasDisponibles());

        service.ocuparCama("sede-tunja", "urgencias");
        service.ocuparCama("sede-tunja", "urgencias");

        assertThrows(IllegalStateException.class, () -> service.ocuparCama("sede-tunja", "urgencias"));

        service.liberarCama("sede-tunja", "urgencias");
        assertEquals(1, service.consultarDisponibilidad("sede-tunja", "urgencias").camasDisponibles());
    }

    @Test
    void fallaSiConsultanUnaSedeServicioNuncaRegistrada() {
        assertThrows(RuntimeException.class, () -> service.consultarDisponibilidad("sede-x", "oncologia"));
    }

    private static class RepositorioInventarioCamasFalso implements RepositorioInventarioCamas {
        private final Map<String, InventarioCamas> almacen = new LinkedHashMap<>();

        @Override
        public Optional<InventarioCamas> buscarPorSedeYServicio(String sedeId, String servicio) {
            return almacen.values().stream()
                    .filter(i -> i.sedeId().equals(sedeId) && i.servicio().equals(servicio))
                    .findFirst();
        }

        @Override
        public List<InventarioCamas> listarPorSede(String sedeId) {
            return almacen.values().stream().filter(i -> i.sedeId().equals(sedeId)).toList();
        }

        @Override
        public InventarioCamas guardar(InventarioCamas inventario) {
            almacen.put(inventario.id(), inventario);
            return inventario;
        }
    }
}