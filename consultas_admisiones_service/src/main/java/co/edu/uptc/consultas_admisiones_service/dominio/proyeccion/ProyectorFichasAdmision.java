package co.edu.uptc.consultas_admisiones_service.dominio.proyeccion;

import co.edu.uptc.consultas_admisiones_service.dominio.modelo.FichaAdmision;
import co.edu.uptc.consultas_admisiones_service.dominio.modelo.ResumenTraslado;
import co.edu.uptc.consultas_admisiones_service.dominio.repositorio.FichaAdmisionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProyectorFichasAdmision {

    private static final Logger log = LoggerFactory.getLogger(ProyectorFichasAdmision.class);
    private final FichaAdmisionRepository repositorio;
    private final ObjectMapper objectMapper;

    public ProyectorFichasAdmision(FichaAdmisionRepository repositorio, ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.objectMapper = objectMapper;
    }

    public synchronized void alIngresarPaciente(String mensajeJson) {
        try {
            JsonNode json = objectMapper.readTree(mensajeJson);
            String admisionId = json.get("admisionId").asText();
            String pacienteId = json.get("pacienteId").asText();
            String sedeInicialId = json.get("sedeInicialId").asText();
            String fechaIngreso = json.get("fechaIngreso").asText();

            FichaAdmision ficha = repositorio.findById(admisionId)
                    .orElseGet(() -> new FichaAdmision(admisionId, pacienteId, sedeInicialId, "ACTIVA", fechaIngreso));

            repositorio.save(ficha);
            log.info("Proyección CQRS: Ficha de admisión {} (Paciente {}) creada/actualizada", admisionId, pacienteId);
        } catch (Exception e) {
            log.error("Error al proyectar el evento PacienteIngresado", e);
        }
    }

    public synchronized void alTrasladarPaciente(String mensajeJson) {
        try {
            JsonNode json = objectMapper.readTree(mensajeJson);
            String admisionId = json.get("admisionId").asText();
            String sedeOrigenId = json.get("sedeOrigenId").asText();
            String sedeDestinoId = json.get("sedeDestinoId").asText();
            String fechaTraslado = json.get("fechaTraslado").asText();

            FichaAdmision ficha = repositorio.findById(admisionId).orElse(null);
            if (ficha != null) {
                ResumenTraslado traslado = new ResumenTraslado(sedeOrigenId, sedeDestinoId, fechaTraslado);
                boolean agregado = ficha.agregarTraslado(traslado);
                if (agregado) {
                    repositorio.save(ficha);
                    log.info("Proyección CQRS: Traslado registrado en admisión {} a la sede {}", admisionId,
                            sedeDestinoId);
                } else {
                    log.info("Proyección CQRS: Traslado duplicado ignorado para admisión {}", admisionId);
                }
            } else {
                log.warn("Proyección CQRS: Traslado recibido antes de crear la admisión {}", admisionId);
            }
        } catch (Exception e) {
            log.error("Error al proyectar el evento PacienteTrasladado", e);
        }
    }

    public synchronized void alDarDeAltaPaciente(String mensajeJson) {
        try {
            JsonNode json = objectMapper.readTree(mensajeJson);
            String admisionId = json.get("admisionId").asText();
            String fechaAlta = json.get("fechaAlta").asText();

            FichaAdmision ficha = repositorio.findById(admisionId).orElse(null);
            if (ficha != null) {
                ficha.darDeAlta(fechaAlta);
                repositorio.save(ficha);
                log.info("Proyección CQRS: Admisión {} dada de alta en el modelo de lectura", admisionId);
            } else {
                log.warn("Proyección CQRS: Alta recibida para admisión no registrada {}", admisionId);
            }
        } catch (Exception e) {
            log.error("Error al proyectar el evento PacienteDadoDeAlta", e);
        }
    }
}
