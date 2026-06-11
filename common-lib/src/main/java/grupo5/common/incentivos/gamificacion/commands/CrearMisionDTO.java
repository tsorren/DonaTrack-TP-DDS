package grupo5.common.incentivos.gamificacion.commands;

public record CrearMisionDTO(
    String nombre,
    String categoriaAsociada,
    Integer cantidadObjetivo,
    String tipoMision // "RACHA", "COMPLETITUD", "VOLUMEN", "EXITO"
    ) {}
