package co.edu.uptc.consultas_admisiones_service.dominio.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "fichas_admision")
public class FichaAdmision {

    @Id
    private String admisionId;
    private String pacienteId;
    private String sedeActualId;
    private String estado; // ACTIVA, DADA_DE_ALTA
    private String fechaIngreso;
    private String fechaAlta;
    private List<ResumenTraslado> traslados = new ArrayList<>();
    private int totalTraslados;

    public FichaAdmision() {
    }

    public FichaAdmision(String admisionId, String pacienteId, String sedeActualId, String estado,
            String fechaIngreso) {
        this.admisionId = admisionId;
        this.pacienteId = pacienteId;
        this.sedeActualId = sedeActualId;
        this.estado = estado;
        this.fechaIngreso = fechaIngreso;
        this.totalTraslados = 0;
    }

    public boolean agregarTraslado(ResumenTraslado traslado) {
        boolean duplicado = traslados.stream()
                .anyMatch(t -> t.sedeOrigenId().equals(traslado.sedeOrigenId()) &&
                        t.sedeDestinoId().equals(traslado.sedeDestinoId()) &&
                        t.fecha().equals(traslado.fecha()));
        if (duplicado) {
            return false;
        }
        this.traslados.add(traslado);
        this.sedeActualId = traslado.sedeDestinoId();
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
