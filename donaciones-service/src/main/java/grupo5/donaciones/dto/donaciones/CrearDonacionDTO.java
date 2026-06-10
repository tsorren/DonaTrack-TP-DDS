package grupo5.donaciones.dto.donaciones;

import java.util.List;

public record CrearDonacionDTO(Long donanteId, String descripcion, List<String> bienes) {}
