package co.edu.uptc.laboratorio_service.infraestructura.entrada.grpc;

import co.edu.uptc.laboratorio_service.aplicacion.OrdenLaboratorioUseCase;
import co.edu.uptc.laboratorio_service.grpc.LaboratorioGrpcGrpc;
import co.edu.uptc.laboratorio_service.grpc.OrdenesPendientesRequest;
import co.edu.uptc.laboratorio_service.grpc.OrdenesPendientesResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

/**
 * Adaptador de entrada gRPC: atiende la llamada que admisiones-service hace
 * (VerificadorLaboratorioGrpcAdapter) antes de dar el alta (HU-09).
 * Solo traduce entre el contrato gRPC y el caso de uso; no tiene lógica de negocio.
 */
@GrpcService
public class LaboratorioGrpcService extends LaboratorioGrpcGrpc.LaboratorioGrpcImplBase {

    private static final Logger log = LoggerFactory.getLogger(LaboratorioGrpcService.class);

    private final OrdenLaboratorioUseCase ordenUseCase;

    public LaboratorioGrpcService(OrdenLaboratorioUseCase ordenUseCase) {
        this.ordenUseCase = ordenUseCase;
    }

    @Override
    public void verificarOrdenesPendientes(
            OrdenesPendientesRequest request,
            StreamObserver<OrdenesPendientesResponse> responseObserver) {
        try {
            boolean pendientes = ordenUseCase.tieneOrdenesPendientes(request.getPacienteId());
            responseObserver.onNext(OrdenesPendientesResponse.newBuilder()
                    .setTienePendientes(pendientes)
                    .build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error al verificar órdenes pendientes", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error interno al consultar órdenes pendientes.")
                    .asRuntimeException());
        }
    }
}
