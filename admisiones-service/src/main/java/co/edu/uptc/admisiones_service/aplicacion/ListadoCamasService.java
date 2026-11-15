package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import co.edu.uptc.admisiones_service.dominio.InventarioCamas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ListadoCamasService implements ListadoCamasUseCase {

    private final CatalogoCamasPuerto catalogo;

    public ListadoCamasService(CatalogoCamasPuerto catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibilidadCama> listarTodas() {
        return catalogo.listarTodas().stream()
                .map(InventarioCamas::aDisponibilidad)
                .sorted(Comparator.comparing(DisponibilidadCama::sedeId)
                        .thenComparing(DisponibilidadCama::servicio))
                .toList();
    }
}
