package grupo5.donaciones.dto.entidades;

import java.util.List;

public record EntidadBeneficiariaDTO(
    Long id,
    String razonSocial,
    String direccionCompleta,
    String telefono,
    List<String> correosRepresentantes) {}
