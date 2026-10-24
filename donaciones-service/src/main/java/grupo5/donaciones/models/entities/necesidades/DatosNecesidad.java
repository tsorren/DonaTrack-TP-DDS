package grupo5.donaciones.models.entities.necesidades;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos comunes con los que se reconstituye una {@link Necesidad} leída de la base. Agrupa los
 * campos de la raíz para que los constructores de reconstitución de cada subtipo no crezcan con
 * cada atributo nuevo. {@code version} es la del bloqueo optimista: el dominio no la usa, solo la
 * transporta entre la lectura y el guardado.
 */
public record DatosNecesidad(
    UUID id,
    UUID subcategoriaId,
    Integer cantidadNecesitada,
    String descripcion,
    LocalDate fechaInicio,
    UUID entidadId,
    Long version) {}
