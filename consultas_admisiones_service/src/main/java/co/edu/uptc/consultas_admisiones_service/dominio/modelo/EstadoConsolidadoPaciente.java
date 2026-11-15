package co.edu.uptc.consultas_admisiones_service.dominio.modelo;

import java.util.List;

/**
 * Vista consolidada de HU-08. Hoy junta admisión + medicación.
 * Cuando laboratorio-service exista, se añade aquí
 * List<FichaOrdenLaboratorio> ordenesLaboratorio, con el mismo patrón:
 * nuevo tópico Kafka, nuevo listener, nuevo proyector, un campo más aquí.
 */
public record EstadoConsolidadoPaciente(
        String pacienteId,
        List<FichaAdmision> admisiones,
        List<FichaMedicacion> medicaciones) {
}