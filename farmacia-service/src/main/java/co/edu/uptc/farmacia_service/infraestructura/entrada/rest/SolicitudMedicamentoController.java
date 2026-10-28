package co.edu.uptc.farmacia_service.infraestructura.entrada.rest;

import co.edu.uptc.farmacia_service.aplicacion.SolicitudMedicamentoUseCase;
import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import co.edu.uptc.farmacia_service.infraestructura.entrada.rest.dto.RecibirSolicitudRequest;
import co.edu.uptc.farmacia_service.infraestructura.entrada.rest.dto.SolicitudMedicamentoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador primario (driving) de Farmacia y cobertura.
 *
 * Solo depende del puerto primario SolicitudMedicamentoUseCase y del mapper
 * de su propia capa. No conoce la implementación del caso de uso, ni el
 * repositorio, ni la aseguradora.
 */
@RestController
@RequestMapping("/api/farmacia/solicitudes")
public class SolicitudMedicamentoController {

    private final SolicitudMedicamentoUseCase solicitudMedicamentoUseCase;
    private final SolicitudMedicamentoRestMapper mapper;

    public SolicitudMedicamentoController(
            SolicitudMedicamentoUseCase solicitudMedicamentoUseCase,
            SolicitudMedicamentoRestMapper mapper) {
        this.solicitudMedicamentoUseCase = solicitudMedicamentoUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<SolicitudMedicamentoResponse> recibir(@Valid @RequestBody RecibirSolicitudRequest request) {
        SolicitudMedicamento solicitud = solicitudMedicamentoUseCase.recibir(
                request.pacienteId(), request.medicoId(), request.medicamento());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(solicitud));
    }

    @PostMapping("/{id}/verificar-cobertura")
    public SolicitudMedicamentoResponse verificarCobertura(@PathVariable String id) {
        return mapper.toResponse(solicitudMedicamentoUseCase.verificarCobertura(id));
    }

    @PostMapping("/{id}/despachar")
    public SolicitudMedicamentoResponse despachar(@PathVariable String id) {
        return mapper.toResponse(solicitudMedicamentoUseCase.despachar(id));
    }

    @GetMapping("/{id}")
    public SolicitudMedicamentoResponse buscarPorId(@PathVariable String id) {
        return mapper.toResponse(solicitudMedicamentoUseCase.buscarPorId(id));
    }

    @GetMapping
    public List<SolicitudMedicamentoResponse> listarTodas() {
        return solicitudMedicamentoUseCase.listarTodas().stream()
                .map(mapper::toResponse)
                .toList();
    }
}
