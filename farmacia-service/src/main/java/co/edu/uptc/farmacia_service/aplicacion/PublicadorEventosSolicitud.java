package co.edu.uptc.farmacia_service.aplicacion;

import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;

/**
 * Puerto secundario de publicación de eventos de dominio.
 *
 * El núcleo nombra el hecho que ocurrió; la infraestructura decide cómo
 * entregarlo (Transactional Outbox + Kafka). Así el caso de uso no conoce
 * el bróker ni el formato de serialización.
 */
public interface PublicadorEventosSolicitud {

    void publicar(String tipoEvento, SolicitudMedicamento solicitud);
}
