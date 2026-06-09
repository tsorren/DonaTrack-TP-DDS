package grupo5.common.donaciones.donantes.commands;

import java.util.List;

public record ModificarPersonaJuridicaDTO(
    String razonSocial,
    String tipoJuridico, // "GUBERNAMENTAL", "ONG", "EMPRESA", "INSTITUCION"
    String rubro,
    String direccion,
    String email,
    String telefono,
    List<String> representantes) {}
