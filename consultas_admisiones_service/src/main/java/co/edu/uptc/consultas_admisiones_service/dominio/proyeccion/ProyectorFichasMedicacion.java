package co.edu.uptc.consultas_admisiones_service.dominio.proyeccion;

import co.edu.uptc.consultas_admisiones_service.dominio.modelo.FichaMedicacion;
import co.edu.uptc.consultas_admisiones_service.dominio.repositorio.FichaMedicacionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProyectorFichasMedicacion {

    private static final Logger log = LoggerFactory.getLogger(ProyectorFichasMedicacion.class);
    private final FichaMedicacionRepository repositorio;
    private final ObjectMapper objectMapper;

    public ProyectorFichasMedicacion(FichaMedicacionRepository repositorio, ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.objectMapper = objectMapper;
    }

    // farmacia-service publica los 3 eventos (MedicamentoSolicitado,
    // CoberturaVerificada, MedicamentoDespachado) en UN SOLO tópico
    // ("farmacia.solicitudes"), diferenciados por el campo "tipo" dentro
    // del JSON — a diferencia de admisiones, que usa un tópico por evento.
    // Por eso aquí no hace falta un método por tipo de evento: basta con
    // upsert sobre el mismo documento por solicitudId.
    public synchronized void alRecibirEventoFarmacia(String mensajeJson) {
        try {
            JsonNode json = objectMapper.readTree(mensajeJson);
            String tipo = json.get("tipo").asText();
            String solicitudId = json.get("solicitudId").asText();
            String pacienteId = json.get("pacienteId").asText();

            FichaMedicacion ficha = repositorio.findById(solicitudId)
                    .orElseGet(() -> new FichaMedicacion(solicitudId, pacienteId));

            ficha.setMedicoId(textoONulo(json, "medicoId"));
            ficha.setMedicamento(textoONulo(json, "medicamento"));
            ficha.setEstado(textoONulo(json, "estado"));
            ficha.setFechaSolicitud(textoONulo(json, "fechaSolicitud"));
            ficha.setFechaDespacho(textoONulo(json, "fechaDespacho"));
            if (json.hasNonNull("cobertura")) {
                ficha.setCoberturaPorcentaje(json.get("cobertura").asInt());
            }

            repositorio.save(ficha);
            log.info("Proyección CQRS: ficha de medicación {} ({}) actualizada para paciente {}",
                    solicitudId, tipo, pacienteId);
        } catch (Exception e) {
            log.error("Error al proyectar un evento de farmacia-service", e);
        }
    }

    private String textoONulo(JsonNode json, String campo) {
        return json.hasNonNull(campo) ? json.get(campo).asText() : null;
    }
}