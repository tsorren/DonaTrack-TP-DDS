package grupo5.donaciones.dto.donaciones.outputs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Donación destacada de la landing (HB-01, RF-01): qué se donó, a qué entidad llegó, cuándo y con
 * qué fotos. Sin el donante ni la descripción libre del bien.
 *
 * @param fechaEntrega en UTC, como la manda Logística en {@code entrega.exitosa}
 * @param fotos URLs estables que redirigen a la foto (ADR-E6-08), en el orden de la entrega
 */
public record DonacionDestacadaDTO(
    UUID id,
    String categoria,
    String subcategoria,
    int cantidad,
    String unidad,
    UUID entidadId,
    String entidad,
    LocalDateTime fechaEntrega,
    List<String> fotos) {}
