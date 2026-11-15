package co.edu.uptc.consultas_admisiones_service.dominio.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "fichas_medicacion")
public class FichaMedicacion {

    @Id
    private String solicitudId;
    private String pacienteId;
    private String medicoId;
    private String medicamento;
    private String estado; // RECIBIDA, COBERTURA_VERIFICADA, DESPACHADA
    private Integer coberturaPorcentaje;
    private String fechaSolicitud;
    private String fechaDespacho;

    public FichaMedicacion() {
    }

    public FichaMedicacion(String solicitudId, String pacienteId) {
        this.solicitudId = solicitudId;
        this.pacienteId = pacienteId;
    }

    public String getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(String solicitudId) {
        this.solicitudId = solicitudId;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(String medicoId) {
        this.medicoId = medicoId;
    }

    public String getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(String medicamento) {
        this.medicamento = medicamento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getCoberturaPorcentaje() {
        return coberturaPorcentaje;
    }

    public void setCoberturaPorcentaje(Integer coberturaPorcentaje) {
        this.coberturaPorcentaje = coberturaPorcentaje;
    }

    public String getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(String fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getFechaDespacho() {
        return fechaDespacho;
    }

    public void setFechaDespacho(String fechaDespacho) {
        this.fechaDespacho = fechaDespacho;
    }
}
