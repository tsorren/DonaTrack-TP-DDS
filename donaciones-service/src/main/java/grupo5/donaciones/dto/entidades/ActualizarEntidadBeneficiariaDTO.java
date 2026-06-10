package grupo5.donaciones.dto.entidades;

import java.util.List;

public record ActualizarEntidadBeneficiariaDTO(
    String razonSocial,
    String direccionCompleta,
    String telefono,
    List<String> correosRepresentantes) {}
