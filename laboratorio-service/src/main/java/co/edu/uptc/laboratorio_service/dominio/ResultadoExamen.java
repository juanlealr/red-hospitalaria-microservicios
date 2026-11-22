package co.edu.uptc.laboratorio_service.dominio;

public record ResultadoExamen(String valor, String unidad) {

    public ResultadoExamen {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "El valor del resultado es obligatorio."
            );
        }

        if (unidad == null || unidad.isBlank()) {
            throw new IllegalArgumentException(
                    "La unidad del resultado es obligatoria."
            );
        }

        valor = valor.trim();
        unidad = unidad.trim();
    }
}
