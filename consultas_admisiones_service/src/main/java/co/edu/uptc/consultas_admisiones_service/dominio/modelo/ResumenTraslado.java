package co.edu.uptc.consultas_admisiones_service.dominio.modelo;

public record ResumenTraslado(
                String sedeOrigenId,
                String servicioOrigen,
                String sedeDestinoId,
                String servicioDestino,
                String fecha) {
}
