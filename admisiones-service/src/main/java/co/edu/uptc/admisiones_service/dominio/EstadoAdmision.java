package co.edu.uptc.admisiones_service.dominio;

/**
 * Estados posibles de una Admision, dentro del subdominio "Admisiones y camas".
 * Lenguaje Ubicuo: estos son exactamente los nombres que usa el personal de
 * admisiones y los médicos al hablar del caso de un paciente (HU-01, HU-03,
 * HU-09).
 */
public enum EstadoAdmision {
    ACTIVA,
    DADA_DE_ALTA
}
