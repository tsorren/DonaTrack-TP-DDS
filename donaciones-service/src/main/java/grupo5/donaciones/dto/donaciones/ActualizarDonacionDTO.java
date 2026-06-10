package grupo5.donaciones.dto.donaciones;

import java.util.List;

public record ActualizarDonacionDTO(Long donanteId, String descripcion, List<String> bienes) {}
