package grupo5.common.donaciones.entidades.commands;

import java.util.UUID;

public record RegistrarNecesidadDTO(
    UUID entidadId,
    String subcategoria,
    Integer cantidadNecesitada,
    String tipoNecesidad // "RECURRENTE" o "EXTRAORDINARIA"
    ) {}
