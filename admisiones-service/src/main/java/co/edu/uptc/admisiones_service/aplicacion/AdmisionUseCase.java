package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;

import java.util.List;

/**
 * Puerto primario (paso 2 del taller de Hexagonal): contrato explícito
 * entre "lo que el caso de uso de Admisiones promete" y "cómo
 * AdmisionService lo cumple" — equivalente a InvestigadorUseCase en RICA.
 *
 * Nada aquí depende de Spring, de JPA ni de HTTP: cualquier adaptador
 * primario futuro (un AdmisionController REST, una tarea programada, una
 * prueba) programa contra esta interfaz, nunca contra AdmisionService
 * directamente.
 */
public interface AdmisionUseCase {

    Admision registrarIngreso(String pacienteId, String sedeId, DisponibilidadCama disponibilidadSede);

    void trasladarPaciente(String admisionId, DisponibilidadCama disponibilidadDestino);

    void darDeAlta(String admisionId, boolean tieneOrdenesLaboratorioPendientes);

    Admision buscarPorId(String admisionId);

    List<Admision> listarTodas();
}
