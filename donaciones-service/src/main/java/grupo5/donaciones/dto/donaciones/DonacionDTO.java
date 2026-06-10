package grupo5.donaciones.dto.donaciones;

import java.util.List;

public record DonacionDTO(
    Long id, Long donanteId, String descripcion, String estado, List<String> bienes) {}
