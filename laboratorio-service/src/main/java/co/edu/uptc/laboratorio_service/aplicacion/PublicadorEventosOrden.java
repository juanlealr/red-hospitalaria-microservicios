package co.edu.uptc.laboratorio_service.aplicacion;

import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;

/**
 * Puerto secundario de publicación de eventos de dominio. El caso de uso nombra el
 * hecho que ocurrió; la infraestructura decide cómo entregarlo (Outbox + Kafka).
 * Debe invocarse dentro de la misma transacción que guarda la orden.
 */
public interface PublicadorEventosOrden {

    void publicar(String tipoEvento, OrdenLaboratorio orden);
}
