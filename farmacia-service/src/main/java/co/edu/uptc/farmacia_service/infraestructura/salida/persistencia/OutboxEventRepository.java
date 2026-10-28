package co.edu.uptc.farmacia_service.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Toma eventos pendientes con bloqueo pesimista y SKIP LOCKED: si hay
     * varias instancias del servicio, cada una procesa un lote distinto y
     * ningún evento se publica dos veces por esta vía.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE publicado = false
            ORDER BY id ASC
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventEntity> buscarPendientesParaPublicar();
}
