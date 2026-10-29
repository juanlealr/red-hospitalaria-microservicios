package co.edu.uptc.farmacia_service.aplicacion;

import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Frontera transaccional de escritura del subdominio.
 *
 * Existe separada del caso de uso por dos razones concretas:
 * 1. La llamada a la aseguradora es de red y no debe ejecutarse dentro de
 *    una transacción de base de datos (no se retiene una conexión mientras
 *    se espera a un tercero).
 * 2. El cambio de estado y el evento en el outbox tienen que confirmarse
 *    en la misma transacción — es la garantía del patrón Transactional
 *    Outbox.
 */
@Service
public class RegistroSolicitudTransaccional {

    private final RepositorioSolicitudesMedicamento repositorio;
    private final PublicadorEventosSolicitud publicador;

    public RegistroSolicitudTransaccional(
            RepositorioSolicitudesMedicamento repositorio,
            PublicadorEventosSolicitud publicador) {
        this.repositorio = repositorio;
        this.publicador = publicador;
    }

    @Transactional
    public SolicitudMedicamento guardar(SolicitudMedicamento solicitud, String tipoEvento) {
        SolicitudMedicamento persistida = repositorio.guardar(solicitud);
        publicador.publicar(tipoEvento, persistida);
        return persistida;
    }
}
