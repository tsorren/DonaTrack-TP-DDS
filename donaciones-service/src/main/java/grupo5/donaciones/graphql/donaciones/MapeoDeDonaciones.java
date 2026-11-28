package grupo5.donaciones.graphql.donaciones;

import grupo5.donaciones.dto.donacionesIndependientes.BienNormalizadoDTO;
import grupo5.donaciones.dto.donacionesIndependientes.BienResumenDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDIResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.ItemDonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.AsignacionRealizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnDeposito;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnPuerta;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnTraslado;
import grupo5.donaciones.models.entities.donacionesIndependientes.EntregaFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.Entregada;
import grupo5.donaciones.models.entities.donacionesIndependientes.ListaParaEntregar;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.Vencida;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Del DTO de REST al tipo {@code Donacion} del esquema (contrato de GraphQL, §4.1). Descarta {@code
 * quienDono}, {@code donacionOriginalId} y el actor y la justificación del historial. Los estados
 * pasan del nombre de la clase del estado ({@code EnTraslado}) al valor de {@link
 * TipoEstadoDonacion} ({@code EN_TRASLADO}), el mismo que recibe el filtro.
 */
final class MapeoDeDonaciones {

  private static final DateTimeFormatter FECHA_CON_HORA =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private static final Map<String, TipoEstadoDonacion> ESTADOS =
      Map.of(
          EnDeposito.class.getSimpleName(), TipoEstadoDonacion.EN_DEPOSITO,
          AsignacionRealizada.class.getSimpleName(), TipoEstadoDonacion.ASIGNACION_REALIZADA,
          EnTraslado.class.getSimpleName(), TipoEstadoDonacion.EN_TRASLADO,
          EnPuerta.class.getSimpleName(), TipoEstadoDonacion.EN_PUERTA,
          ListaParaEntregar.class.getSimpleName(), TipoEstadoDonacion.LISTA_PARA_ENTREGAR,
          Entregada.class.getSimpleName(), TipoEstadoDonacion.ENTREGADA,
          EntregaFallida.class.getSimpleName(), TipoEstadoDonacion.ENTREGA_FALLIDA,
          Vencida.class.getSimpleName(), TipoEstadoDonacion.VENCIDA);

  private MapeoDeDonaciones() {}

  static DonacionGraphQl donacion(DonacionIndependienteResponseDTO dto) {
    return new DonacionGraphQl(
        dto.id(),
        dto.descripcion(),
        estado(dto.estadoActual()),
        fechaConHora(dto.fechaRegistro()),
        dto.cantidad(),
        dto.cancelacionPedida(),
        dto.entidadBeneficiariaId(),
        dto.necesidadId(),
        items(dto.items()),
        historial(dto.historial()));
  }

  static List<CambioEstadoGraphQl> historial(List<CambioEstadoDIResponseDTO> historial) {
    if (historial == null) {
      return List.of();
    }
    return historial.stream()
        .map(
            cambio ->
                new CambioEstadoGraphQl(
                    estado(cambio.estadoAnterior()),
                    estado(cambio.estadoNuevo()),
                    fechaConHora(cambio.timestamp())))
        .toList();
  }

  /** El valor del enum de un nombre de clase de estado; {@code null} si no hay estado. */
  static TipoEstadoDonacion estado(String nombreDeClase) {
    if (nombreDeClase == null) {
      return null;
    }
    TipoEstadoDonacion tipo = ESTADOS.get(nombreDeClase);
    if (tipo == null) {
      throw new IllegalStateException("Estado de donación desconocido: " + nombreDeClase);
    }
    return tipo;
  }

  private static List<ItemDonacionGraphQl> items(List<ItemDonacionIndependienteResponseDTO> items) {
    if (items == null) {
      return List.of();
    }
    return items.stream().map(MapeoDeDonaciones::item).toList();
  }

  private static ItemDonacionGraphQl item(ItemDonacionIndependienteResponseDTO item) {
    BienNormalizadoDTO normalizado = item.bien();
    if (normalizado == null) {
      return new ItemDonacionGraphQl(item.cantidad(), bien(null), null, null);
    }
    return new ItemDonacionGraphQl(
        item.cantidad(),
        bien(normalizado.bien()),
        normalizado.subcategoria(),
        normalizado.categoria());
  }

  private static BienGraphQl bien(BienResumenDTO bien) {
    if (bien == null) {
      return new BienGraphQl(null, null, null, null);
    }
    return new BienGraphQl(
        bien.descripcion(), bien.fotoUrl(), fecha(bien.fechaVencimiento()), bien.estado());
  }

  private static String fecha(LocalDate fecha) {
    return fecha == null ? null : fecha.toString();
  }

  private static String fechaConHora(LocalDateTime fecha) {
    return fecha == null ? null : FECHA_CON_HORA.format(fecha);
  }
}
