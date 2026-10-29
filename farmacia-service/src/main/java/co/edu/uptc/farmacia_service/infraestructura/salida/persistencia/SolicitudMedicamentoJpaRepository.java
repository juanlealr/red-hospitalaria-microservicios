package co.edu.uptc.farmacia_service.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Detalle técnico de persistencia: Spring Data JPA genera la
 * implementación por reflexión. Nadie fuera de
 * SolicitudMedicamentoJpaAdapter debería conocer esta interfaz.
 */
public interface SolicitudMedicamentoJpaRepository extends JpaRepository<SolicitudMedicamentoJpaEntity, String> {
}
