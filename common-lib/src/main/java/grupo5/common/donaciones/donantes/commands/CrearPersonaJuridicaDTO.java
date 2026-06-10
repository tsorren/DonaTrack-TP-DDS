package grupo5.common.donaciones.donantes.commands;

import java.util.List;

public record CrearPersonaJuridicaDTO(
    String razonSocial,
    String tipoJuridico, // "GUBERNAMENTAL", "ONG", "EMPRESA", "INSTITUCION"
    String rubro,
    String tipoDocumento, // "CUIT"
    String nroDocumento,
    String direccion,
    String email,
    String telefono,
    List<String> representantes // Nombres o identificadores de representantes
    ) {}
