package co.edu.uptc.consultas_admisiones_service.dominio.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "fichas_admision")
public class FichaAdmision {

    @Id
    private String admisionId;
    private String pacienteId;
    private String sedeActualId;
    private String servicioActual;
    private String medicoTratante;
    private String motivoIngreso;
    private Integer coberturaPorcentaje;
    private String estado; // ACTIVA, DADA_DE_ALTA
    private String fechaIngreso;
    private String fechaAlta;
    private List<ResumenTraslado> traslados = new ArrayList<>();
    private int totalTraslados;

    public FichaAdmision() {
    }

    public FichaAdmision(
            String admisionId,
            String pacienteId,
            String sedeActualId,
            String servicioActual,
            String medicoTratante,
            String motivoIngreso,
            String estado,
            String fechaIngreso) {
        this.admisionId = admisionId;
        this.pacienteId = pacienteId;
        this.sedeActualId = sedeActualId;
        this.servicioActual = servicioActual;
        this.medicoTratante = medicoTratante;
        this.motivoIngreso = motivoIngreso;
        this.estado = estado;
        this.fechaIngreso = fechaIngreso;
        this.totalTraslados = 0;
    }

    public boolean agregarTraslado(ResumenTraslado traslado) {
        boolean duplicado = traslados.stream()
                .anyMatch(t -> Objects.equals(t.sedeOrigenId(), traslado.sedeOrigenId()) &&
                        Objects.equals(t.servicioOrigen(), traslado.servicioOrigen()) &&
                        Objects.equals(t.sedeDestinoId(), traslado.sedeDestinoId()) &&
                        Objects.equals(t.servicioDestino(), traslado.servicioDestino()) &&
                        Objects.equals(t.fecha(), traslado.fecha()));
        if (duplicado) {
            return false;
        }
        this.traslados.add(traslado);
        this.sedeActualId = traslado.sedeDestinoId();
        if (traslado.servicioDestino() != null) {
            this.servicioActual = traslado.servicioDestino();
        }
        this.totalTraslados = this.traslados.size();
        return true;
    }

    public void darDeAlta(String fechaAlta) {
        this.estado = "DADA_DE_ALTA";
        this.fechaAlta = fechaAlta;
    }

    public String getAdmisionId() {
        return admisionId;
    }

    public void setAdmisionId(String admisionId) {
        this.admisionId = admisionId;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getSedeActualId() {
        return sedeActualId;
    }

    public void setSedeActualId(String sedeActualId) {
        this.sedeActualId = sedeActualId;
    }

    public String getServicioActual() {
        return servicioActual;
    }

    public void setServicioActual(String servicioActual) {
        this.servicioActual = servicioActual;
    }

    public String getMedicoTratante() {
        return medicoTratante;
    }

    public void setMedicoTratante(String medicoTratante) {
        this.medicoTratante = medicoTratante;
    }

    public String getMotivoIngreso() {
        return motivoIngreso;
    }

    public void setMotivoIngreso(String motivoIngreso) {
        this.motivoIngreso = motivoIngreso;
    }

    public Integer getCoberturaPorcentaje() {
        return coberturaPorcentaje;
    }

    public void setCoberturaPorcentaje(Integer coberturaPorcentaje) {
        this.coberturaPorcentaje = coberturaPorcentaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(String fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(String fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public List<ResumenTraslado> getTraslados() {
        return traslados;
    }

    public void setTraslados(List<ResumenTraslado> traslados) {
        this.traslados = traslados;
    }

    public int getTotalTraslados() {
        return totalTraslados;
    }

    public void setTotalTraslados(int totalTraslados) {
        this.totalTraslados = totalTraslados;
    }
}
