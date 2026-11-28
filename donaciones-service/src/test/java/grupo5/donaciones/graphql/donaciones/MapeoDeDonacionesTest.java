package grupo5.donaciones.graphql.donaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDIResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.AsignacionRealizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnDeposito;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnPuerta;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnTraslado;
import grupo5.donaciones.models.entities.donacionesIndependientes.EntregaFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.Entregada;
import grupo5.donaciones.models.entities.donacionesIndependientes.EstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ListaParaEntregar;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.Vencida;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** El mapeo del DTO de REST al tipo {@code Donacion} del esquema (contrato de GraphQL, §4.1). */
class MapeoDeDonacionesTest {

  static Stream<EstadoDonacionIndependiente> estados() {
    return Stream.of(
        new EnDeposito(),
        new AsignacionRealizada(),
        new EnTraslado(),
        new EnPuerta(),
        new ListaParaEntregar(),
        new Entregada("AA000AA"),
        new EntregaFallida(),
        new Vencida());
  }

  @ParameterizedTest
  @MethodSource("estados")
  void cadaClaseDeEstadoCorrespondeASuValorDelEnum(EstadoDonacionIndependiente estado) {
    assertThat(MapeoDeDonaciones.estado(estado.getClass().getSimpleName()))
        .isEqualTo(estado.getTipo());
  }

  @Test
  void losOchoValoresDelEnumTienenSuClase() {
    assertThat(estados().map(EstadoDonacionIndependiente::getTipo))
        .containsExactlyInAnyOrder(TipoEstadoDonacion.values());
  }

  @Test
  void unEstadoNuloEsNuloYUnoDesconocidoEsUnError() {
    assertThat(MapeoDeDonaciones.estado(null)).isNull();
    assertThatThrownBy(() -> MapeoDeDonaciones.estado("Inventado"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void elTipoDonacionNoTieneQuienDonoNiLaDonacionOriginal() {
    assertThat(componentes(DonacionGraphQl.class))
        .doesNotContain("quienDono", "donacionOriginalId", "donanteId");
  }

  @Test
  void elCambioDeEstadoNoTieneElActorNiLaJustificacion() {
    assertThat(componentes(CambioEstadoGraphQl.class))
        .containsExactly("estadoAnterior", "estadoNuevo", "fecha");
  }

  @Test
  void unaDonacionSinItemsNiHistorialDaListasVacias() {
    DonacionIndependienteResponseDTO dto =
        new DonacionIndependienteResponseDTO(
            UUID.fromString("00000000-0000-4000-8000-0000000000c1"),
            null,
            null,
            "EnDeposito",
            LocalDateTime.of(2026, 11, 2, 10, 0),
            null,
            null,
            0,
            null);

    DonacionGraphQl donacion = MapeoDeDonaciones.donacion(dto);

    assertThat(donacion.items()).isEmpty();
    assertThat(donacion.historial()).isEmpty();
    assertThat(donacion.estado()).isEqualTo(TipoEstadoDonacion.EN_DEPOSITO);
    assertThat(donacion.fechaRegistro()).isEqualTo("2026-11-02T10:00:00");
  }

  @Test
  void elHistorialConservaElOrdenYDescartaActorYJustificacion() {
    List<CambioEstadoDIResponseDTO> historial =
        List.of(
            new CambioEstadoDIResponseDTO(
                "EnDeposito", "Vencida", LocalDateTime.of(2026, 11, 5, 9, 15, 7), "j", "a"));

    assertThat(MapeoDeDonaciones.historial(historial))
        .containsExactly(
            new CambioEstadoGraphQl(
                TipoEstadoDonacion.EN_DEPOSITO, TipoEstadoDonacion.VENCIDA, "2026-11-05T09:15:07"));
  }

  private static List<String> componentes(Class<?> registro) {
    return Arrays.stream(registro.getRecordComponents()).map(RecordComponent::getName).toList();
  }
}
