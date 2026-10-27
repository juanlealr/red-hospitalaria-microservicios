package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.Admision;

import java.util.List;
import java.util.Optional;

/**
 * Puerto secundario mínimo (paso 3 del taller de Hexagonal), equivalente a
 * RepositorioInvestigadores en RICA: solo los métodos que AdmisionService y
 * AdmisionFactory realmente llaman — no los ~30 que regala JpaRepository.
 *
 * A diferencia de RICA, aquí no hubo que "adelgazar" un JpaRepository
 * existente (nuestro proyecto no tenía persistencia todavía): este puerto
 * se diseñó directamente desde el caso de uso, antes de escribir el
 * adaptador — así que ya nace mínimo.
 */
public interface RepositorioAdmisiones {

    List<Admision> listarTodos();

    Optional<Admision> buscarPorId(String id);

    Admision guardar(Admision admision);
}
