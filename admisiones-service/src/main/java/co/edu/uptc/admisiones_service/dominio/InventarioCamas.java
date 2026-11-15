package co.edu.uptc.admisiones_service.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Raíz del Agregado: InventarioCamas.
 *
 * Es la fuente de verdad de cuántas camas hay y cuántas están ocupadas por
 * sede y servicio (HU-02, HU-10). Antes de este agregado, ese dato lo
 * inventaba quien llamara al endpoint de ingreso — ahora vive aquí y se
 * actualiza atómicamente con cada ingreso, traslado y alta.
 */
@Entity
@Table(name = "inventario_camas")
public class InventarioCamas {

    @Id
    private String id;

    private String sedeId;
    private String servicio;
    private int camasTotales;
    private int camasOcupadas;
    @Version
    private Long version;

    protected InventarioCamas() {
        // exigido por Hibernate
    }

    public InventarioCamas(String id, String sedeId, String servicio, int camasTotales) {
        if (camasTotales < 0) {
            throw new IllegalArgumentException("El total de camas no puede ser negativo.");
        }
        this.id = id;
        this.sedeId = sedeId;
        this.servicio = servicio;
        this.camasTotales = camasTotales;
        this.camasOcupadas = 0;
    }

    public void ocupar() {
        if (camasOcupadas >= camasTotales) {
            throw new IllegalStateException(
                    "No hay cama disponible en sede %s / servicio %s.".formatted(sedeId, servicio));
        }
        camasOcupadas++;
    }

    public void liberar() {
        if (camasOcupadas <= 0) {
            throw new IllegalStateException(
                    "No hay camas ocupadas que liberar en sede %s / servicio %s.".formatted(sedeId, servicio));
        }
        camasOcupadas--;
    }

    public void actualizarCapacidad(int nuevoTotal) {
        if (nuevoTotal < camasOcupadas) {
            throw new IllegalArgumentException(
                    "No se puede fijar un total (%d) menor que las camas ya ocupadas (%d)."
                            .formatted(nuevoTotal, camasOcupadas));
        }
        this.camasTotales = nuevoTotal;
    }

    public DisponibilidadCama aDisponibilidad() {
        return new DisponibilidadCama(sedeId, servicio, camasTotales, camasOcupadas);
    }

    public String id() {
        return id;
    }

    public String sedeId() {
        return sedeId;
    }

    public String servicio() {
        return servicio;
    }
}
