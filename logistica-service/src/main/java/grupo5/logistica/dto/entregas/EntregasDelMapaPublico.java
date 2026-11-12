package grupo5.logistica.dto.entregas;

import java.util.List;

/**
 * Lo que responde el mapa público (RF-02, R11-19): las entregas del período, ya con el tope, y
 * cuántas cumplen el período sin el tope ({@code total}, que el controller informa en {@code
 * X-Total-Count}).
 */
public record EntregasDelMapaPublico(List<EntregaMapaPublicoResponseDTO> entregas, long total) {

  public EntregasDelMapaPublico {
    entregas = List.copyOf(entregas);
  }
}
