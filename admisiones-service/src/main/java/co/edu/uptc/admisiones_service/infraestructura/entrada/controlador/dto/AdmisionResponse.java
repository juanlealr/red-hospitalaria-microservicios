package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador.dto;

import co.edu.uptc.admisiones_service.dominio.Admision;

import java.util.List;

public record AdmisionResponse(
        String id,
        String pacienteId,
        String sedeActualId,
        String servicioActual,
        String estado,
        String fechaIngreso,
        String fechaAlta,
        List<TrasladoResponse> traslados) {

    public record TrasladoResponse(
            String sedeOrigenId, String servicioOrigen,
            String sedeDestinoId, String servicioDestino, String fecha) {
    }

    public static AdmisionResponse desde(Admision a) {
        List<TrasladoResponse> traslados = a.historialTraslados().stream()
                .map(t -> new TrasladoResponse(t.sedeOrigenId(), t.servicioOrigen(),
                        t.sedeDestinoId(), t.servicioDestino(), String.valueOf(t.fecha())))
                .toList();

        return new AdmisionResponse(
                a.id(),
                a.pacienteId(),
                a.sedeActualId(),
                a.servicioActual(),
                a.estado().name(),
                String.valueOf(a.fechaIngreso()),
                a.fechaAlta() == null ? null : a.fechaAlta().toString(),
                traslados);
    }
}
