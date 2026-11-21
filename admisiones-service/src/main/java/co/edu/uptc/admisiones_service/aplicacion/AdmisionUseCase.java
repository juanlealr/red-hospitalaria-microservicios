package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.Admision;
import java.util.List;

public interface AdmisionUseCase {

    Admision registrarIngreso(
            String pacienteId,
            String sedeId,
            String servicio,
            String medicoTratante,
            String motivoIngreso);

    Admision verificarCoberturaAdmision(String admisionId);

    int consultarCoberturaPaciente(String pacienteId);

    void trasladarPaciente(String admisionId, String sedeDestinoId, String servicioDestino);

    void darDeAlta(String admisionId);

    Admision buscarPorId(String admisionId);

    List<Admision> listarTodas();
}
