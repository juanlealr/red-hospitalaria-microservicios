package co.edu.uptc.farmacia_service.infraestructura.salida.aseguradora;

import co.edu.uptc.farmacia_service.dominio.Cobertura;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoberturaAseguradoraStubAdapterTest {

    private final CoberturaAseguradoraStubAdapter adapter = new CoberturaAseguradoraStubAdapter();

    @Test
    void devuelveUnaCoberturaFijaParaCualquierPaciente() {
        assertEquals(new Cobertura(80), adapter.consultar("pac-1"));
        assertEquals(new Cobertura(80), adapter.consultar("pac-2"));
    }
}
