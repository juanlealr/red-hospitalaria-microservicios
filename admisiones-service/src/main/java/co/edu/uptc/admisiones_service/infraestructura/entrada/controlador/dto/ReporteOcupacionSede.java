package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador.dto;

import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;

import java.util.List;

public record ReporteOcupacionSede(
        String sedeId,
        int camasTotales,
        int camasOcupadas,
        int camasLibres,
        double porcentajeOcupacion,
        List<DetalleServicio> servicios) {

    public record DetalleServicio(
            String servicio, int camasTotales, int camasOcupadas, int camasLibres, double porcentajeOcupacion) {
    }

    public static ReporteOcupacionSede desde(String sedeId, List<DisponibilidadCama> inventario) {
        List<DetalleServicio> detalle = inventario.stream()
                .map(d -> new DetalleServicio(d.servicio(), d.camasTotales(), d.camasOcupadas(),
                        d.camasTotales() - d.camasOcupadas(), porcentaje(d.camasOcupadas(), d.camasTotales())))
                .toList();

        int totales = detalle.stream().mapToInt(DetalleServicio::camasTotales).sum();
        int ocupadas = detalle.stream().mapToInt(DetalleServicio::camasOcupadas).sum();

        return new ReporteOcupacionSede(sedeId, totales, ocupadas, totales - ocupadas,
                porcentaje(ocupadas, totales), detalle);
    }

    private static double porcentaje(int ocupadas, int totales) {
        return totales == 0 ? 0.0 : Math.round(ocupadas * 1000.0 / totales) / 10.0;
    }
}
