package grupo5.common.donaciones.entidades.commands;

import java.util.List;

public record RegistrarEntidadBeneficiariaDTO(
    String razonSocial, String direccion, String telefono, List<String> emailsRepresentantes) {}
