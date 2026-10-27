package co.edu.uptc.admisiones_service.infraestructura.salida.persistencia;

import co.edu.uptc.admisiones_service.aplicacion.RepositorioAdmisiones;
import co.edu.uptc.admisiones_service.dominio.Admision;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador secundario (paso 3 del taller de Hexagonal), equivalente a
 * InvestigadorRepositoryJpaAdapter en RICA: traduce entre el puerto que
 * diseñó el núcleo (RepositorioAdmisiones) y el que genera Spring Data JPA
 * (AdmisionJpaRepository). Es la única clase del proyecto que conoce a la
 * vez los dos contratos.
 */
@Component
public class AdmisionRepositoryJpaAdapter implements RepositorioAdmisiones {

    private final AdmisionJpaRepository admisionJpaRepository;

    public AdmisionRepositoryJpaAdapter(AdmisionJpaRepository admisionJpaRepository) {
        this.admisionJpaRepository = admisionJpaRepository;
    }

    @Override
    public List<Admision> listarTodos() {
        return admisionJpaRepository.findAll();
    }

    @Override
    public Optional<Admision> buscarPorId(String id) {
        return admisionJpaRepository.findById(id);
    }

    @Override
    public Admision guardar(Admision admision) {
        return admisionJpaRepository.save(admision);
    }
}
