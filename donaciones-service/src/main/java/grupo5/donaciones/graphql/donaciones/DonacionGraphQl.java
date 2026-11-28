package grupo5.donaciones.graphql.donaciones;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.util.List;
import java.util.UUID;

/**
 * El tipo {@code Donacion} del esquema (contrato de GraphQL, §4.1): la donación independiente sin
 * quién donó y sin el id de la donación original. Las relaciones {@code necesidad} y {@code
 * entidad} no son campos de este registro: las resuelve el lote {@code g2-relaciones} por lotes a
 * partir de {@code necesidadId} y {@code entidadId}.
 */
public record DonacionGraphQl(
    UUID id,
    String descripcion,
    TipoEstadoDonacion estado,
    String fechaRegistro,
    int cantidad,
    boolean cancelacionPedida,
    UUID entidadId,
    UUID necesidadId,
    List<ItemDonacionGraphQl> items,
    List<CambioEstadoGraphQl> historial) {

  public DonacionGraphQl {
    items = List.copyOf(items);
    historial = List.copyOf(historial);
  }
}
