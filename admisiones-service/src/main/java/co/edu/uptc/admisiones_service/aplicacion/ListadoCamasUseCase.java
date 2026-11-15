package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import java.util.List;

public interface ListadoCamasUseCase {
    List<DisponibilidadCama> listarTodas();
}