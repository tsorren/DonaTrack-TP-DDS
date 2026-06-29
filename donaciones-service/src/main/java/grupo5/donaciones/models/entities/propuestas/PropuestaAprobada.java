package grupo5.donaciones.models.entities.propuestas;

import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.List;
import java.util.UUID;

public record PropuestaAprobada(
    UUID propuestaId,
    Necesidad necesidad,
    List<PosibleFragmentacion> fragmentaciones,
    String actor) {}
