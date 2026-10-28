package co.edu.uptc.farmacia_service.aplicacion;

import co.edu.uptc.farmacia_service.dominio.Cobertura;

/**
 * Puerto secundario hacia el sistema externo de la aseguradora (HU-07).
 *
 * La lógica clínica no depende de la tecnología de la integración: hoy hay
 * un adaptador REST con Circuit Breaker y, para desarrollo local, un
 * adaptador stub. Cambiar de uno a otro no toca el núcleo.
 */
public interface ConsultaCoberturaExterna {

    Cobertura consultar(String pacienteId);
}
