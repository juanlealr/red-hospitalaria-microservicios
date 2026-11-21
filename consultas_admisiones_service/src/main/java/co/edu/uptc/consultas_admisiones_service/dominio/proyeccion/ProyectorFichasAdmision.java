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
            String servicioInicial = textoONulo(json, "servicioInicial");
            String medicoTratante = textoONulo(json, "medicoTratante");
            String motivoIngreso = textoONulo(json, "motivoIngreso");
            String fechaIngreso = json.get("fechaIngreso").asText();

            FichaAdmision ficha = repositorio.findById(admisionId).orElse(null);
            if (ficha == null) {
                ficha = new FichaAdmision(
                        admisionId,
                        pacienteId,
                        sedeInicialId,
                        servicioInicial,
                        medicoTratante,
                        motivoIngreso,
                        "ACTIVA",
                        fechaIngreso);
            } else {
                // Si llegó antes un evento secundario (cobertura/traslado/alta), completamos
                // los datos base sin perderlos
                ficha.setPacienteId(pacienteId);
                if (ficha.getTotalTraslados() == 0) {
                    ficha.setSedeActualId(sedeInicialId);
                    ficha.setServicioActual(servicioInicial);
                }
                ficha.setMedicoTratante(medicoTratante);
                ficha.setMotivoIngreso(motivoIngreso);
                if (ficha.getEstado() == null) {
                    ficha.setEstado("ACTIVA");
                }
                ficha.setFechaIngreso(fechaIngreso);
            }

            repositorio.save(ficha);
            log.info("Proyección CQRS: Ficha de admisión {} (Paciente {}) creada/actualizada", admisionId, pacienteId);
        } catch (Exception e) {
            log.error("Error al proyectar el evento PacienteIngresado", e);
        }
    }

    public synchronized void alVerificarCobertura(String mensajeJson) {
        try {
            JsonNode json = objectMapper.readTree(mensajeJson);
            String admisionId = json.get("admisionId").asText();
            String pacienteId = textoONulo(json, "pacienteId");
            int porcentaje = json.get("coberturaPorcentaje").asInt();

            FichaAdmision ficha = repositorio.findById(admisionId).orElseGet(() -> {
                FichaAdmision nueva = new FichaAdmision();
                nueva.setAdmisionId(admisionId);
                nueva.setPacienteId(pacienteId);
                nueva.setEstado("ACTIVA");
                return nueva;
            });

            ficha.setCoberturaPorcentaje(porcentaje);
            repositorio.save(ficha);
            log.info("Proyección CQRS: Cobertura {}% proyectada en admisión {}", porcentaje, admisionId);
        } catch (Exception e) {
            log.error("Error al proyectar el evento CoberturaPacienteVerificada", e);
        }
    }

    public synchronized void alTrasladarPaciente(String mensajeJson) {
        try {
            JsonNode json = objectMapper.readTree(mensajeJson);
            String admisionId = json.get("admisionId").asText();
            String pacienteId = textoONulo(json, "pacienteId");
            String sedeOrigenId = json.get("sedeOrigenId").asText();
            String servicioOrigen = textoONulo(json, "servicioOrigen");
            String sedeDestinoId = json.get("sedeDestinoId").asText();
            String servicioDestino = textoONulo(json, "servicioDestino");
            String fechaTraslado = json.get("fechaTraslado").asText();

            FichaAdmision ficha = repositorio.findById(admisionId).orElseGet(() -> {
                FichaAdmision nueva = new FichaAdmision();
                nueva.setAdmisionId(admisionId);
                nueva.setPacienteId(pacienteId);
                nueva.setEstado("ACTIVA");
                return nueva;
            });

            ResumenTraslado traslado = new ResumenTraslado(
                    sedeOrigenId, servicioOrigen, sedeDestinoId, servicioDestino, fechaTraslado);
            boolean agregado = ficha.agregarTraslado(traslado);
            if (agregado) {
                repositorio.save(ficha);
                log.info("Proyección CQRS: Traslado registrado en admisión {} a la sede {}", admisionId, sedeDestinoId);
            } else {
                log.info("Proyección CQRS: Traslado duplicado ignorado para admisión {}", admisionId);
            }
        } catch (Exception e) {
            log.error("Error al proyectar el evento PacienteTrasladado", e);
        }
    }

    public synchronized void alDarDeAltaPaciente(String mensajeJson) {
        try {
            JsonNode json = objectMapper.readTree(mensajeJson);
            String admisionId = json.get("admisionId").asText();
            String pacienteId = textoONulo(json, "pacienteId");
            String fechaAlta = json.get("fechaAlta").asText();

            FichaAdmision ficha = repositorio.findById(admisionId).orElseGet(() -> {
                FichaAdmision nueva = new FichaAdmision();
                nueva.setAdmisionId(admisionId);
                nueva.setPacienteId(pacienteId);
                return nueva;
            });

            ficha.darDeAlta(fechaAlta);
            repositorio.save(ficha);
            log.info("Proyección CQRS: Admisión {} dada de alta en el modelo de lectura", admisionId);
        } catch (Exception e) {
            log.error("Error al proyectar el evento PacienteDadoDeAlta", e);
        }
    }

    private String textoONulo(JsonNode json, String campo) {
        return json.hasNonNull(campo) ? json.get(campo).asText() : null;
    }
}
