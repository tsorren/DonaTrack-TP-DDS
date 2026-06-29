package grupo5.donaciones.models.entities.propuestas;

import java.util.List;
import java.util.UUID;

public record PropuestaAprobada(
    UUID propuestaId, UUID necesidadId, List<PosibleFragmentacion> fragmentaciones, String actor) {}
