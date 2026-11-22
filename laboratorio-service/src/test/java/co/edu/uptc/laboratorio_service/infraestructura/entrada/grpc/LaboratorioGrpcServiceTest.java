package co.edu.uptc.laboratorio_service.infraestructura.entrada.grpc;

import co.edu.uptc.laboratorio_service.aplicacion.OrdenLaboratorioUseCase;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.grpc.OrdenesPendientesRequest;
import co.edu.uptc.laboratorio_service.grpc.OrdenesPendientesResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** El adaptador gRPC solo traduce: se prueba con un caso de uso falso y un observer manual. */
class LaboratorioGrpcServiceTest {

    @Test
    void respondeTienePendientesSegunElCasoDeUso() {
        var grpc = new LaboratorioGrpcService(new CasoDeUsoFalso(true));
        var observer = new ObserverManual();

        grpc.verificarOrdenesPendientes(solicitud("PAC-1001"), observer);

        assertEquals(1, observer.respuestas.size());
        assertTrue(observer.respuestas.get(0).getTienePendientes());
        assertTrue(observer.completado);
        assertNull(observer.error);
    }

    @Test
    void respondeSinPendientesCuandoElCasoDeUsoDiceQueNo() {
        var grpc = new LaboratorioGrpcService(new CasoDeUsoFalso(false));
        var observer = new ObserverManual();

        grpc.verificarOrdenesPendientes(solicitud("PAC-1001"), observer);

        assertFalse(observer.respuestas.get(0).getTienePendientes());
    }

    @Test
    void unPacienteVacioSeRespondeConInvalidArgument() {
        var grpc = new LaboratorioGrpcService(new CasoDeUsoFalso(false));
        var observer = new ObserverManual();

        grpc.verificarOrdenesPendientes(solicitud(""), observer);

        assertTrue(observer.respuestas.isEmpty());
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(observer.error).getCode());
    }

    @Test
    void unFalloInesperadoSeRespondeConInternalSinFiltrarElDetalle() {
        var grpc = new LaboratorioGrpcService(new CasoDeUsoFalso(false) {
            @Override
            public boolean tieneOrdenesPendientes(String pacienteId) {
                throw new RuntimeException("detalle interno secreto");
            }
        });
        var observer = new ObserverManual();

        grpc.verificarOrdenesPendientes(solicitud("PAC-1001"), observer);

        StatusRuntimeException error = (StatusRuntimeException) observer.error;
        assertEquals(Status.Code.INTERNAL, error.getStatus().getCode());
        assertFalse(String.valueOf(error.getStatus().getDescription()).contains("secreto"));
    }

    private static OrdenesPendientesRequest solicitud(String pacienteId) {
        return OrdenesPendientesRequest.newBuilder().setPacienteId(pacienteId).build();
    }

    private static class ObserverManual implements StreamObserver<OrdenesPendientesResponse> {
        final List<OrdenesPendientesResponse> respuestas = new ArrayList<>();
        Throwable error;
        boolean completado;

        @Override
        public void onNext(OrdenesPendientesResponse value) {
            respuestas.add(value);
        }

        @Override
        public void onError(Throwable t) {
            error = t;
        }

        @Override
        public void onCompleted() {
            completado = true;
        }
    }

    private static class CasoDeUsoFalso implements OrdenLaboratorioUseCase {
        private final boolean pendientes;

        CasoDeUsoFalso(boolean pendientes) {
            this.pendientes = pendientes;
        }

        @Override
        public boolean tieneOrdenesPendientes(String pacienteId) {
            if (pacienteId == null || pacienteId.isBlank()) {
                throw new IllegalArgumentException("El id del paciente es obligatorio.");
            }
            return pendientes;
        }

        @Override
        public OrdenLaboratorio solicitarExamen(String pacienteId, String tipoExamen) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdenLaboratorio registrarResultado(UUID ordenId, String valor, String unidad) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cerrar(UUID ordenId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdenLaboratorio buscarPorId(UUID ordenId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OrdenLaboratorio> listarOrdenes(String pacienteId) {
            throw new UnsupportedOperationException();
        }
    }
}
