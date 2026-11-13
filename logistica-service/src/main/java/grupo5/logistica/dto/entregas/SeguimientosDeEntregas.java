package grupo5.logistica.dto.entregas;

import java.util.List;

/**
 * Una página del seguimiento (R11-43): los seguimientos pedidos y cuántas entregas cumplen el
 * filtro sin contar la página ({@code total}, que el controller informa en {@code X-Total-Count}).
 */
public record SeguimientosDeEntregas(List<SeguimientoEntregaResponseDTO> seguimientos, long total) {

  public SeguimientosDeEntregas {
    seguimientos = List.copyOf(seguimientos);
  }
}
