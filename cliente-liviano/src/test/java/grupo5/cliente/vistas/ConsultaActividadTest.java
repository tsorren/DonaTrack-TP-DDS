package grupo5.cliente.vistas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Lo que el registro de actividad entiende de la dirección (R11-26, ficha §3 y §5). */
class ConsultaActividadTest {

  private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

  @Test
  void entiendeLosFiltrosValidos() {
    ConsultaActividad c =
        ConsultaActividad.de(
            " ana ", "cuenta-bruno", "cuentas", "mes-anterior", null, null, 3, HOY);

    assertThat(c.buscar()).isEqualTo("ana");
    assertThat(c.deUnaCuenta()).isTrue();
    assertThat(c.accion()).isEqualTo("cuentas");
    assertThat(c.desde()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(c.hasta()).isEqualTo(LocalDate.of(2026, 10, 31));
    assertThat(c.pagina()).isEqualTo(3);
    assertThat(c.avisos()).isEmpty();
    assertThat(c.filtrada()).isTrue();
  }

  @Test
  void loQueNoEntiendeLoDescartaYLoAvisa() {
    ConsultaActividad c =
        ConsultaActividad.de("", "<script>", "otra", "", "2026-13-01", null, 1, HOY);

    assertThat(c.autor()).isEmpty();
    assertThat(c.accion()).isEmpty();
    assertThat(c.fecha()).isEmpty();
    assertThat(c.desde()).isNull();
    assertThat(c.avisos())
        .containsExactly(
            ConsultaActividad.NO_ENTENDIMOS_LA_CUENTA,
            ConsultaActividad.NO_ENTENDIMOS_LA_ACCION,
            ConsultaImportaciones.NO_ENTENDIMOS_LA_FECHA);
    assertThat(c.filtrada()).isFalse();
  }

  @Test
  void lasFechasEscritasSonUnRango() {
    ConsultaActividad c =
        ConsultaActividad.de("", "yo", "", "", "2026-10-01", "2026-10-15", 1, HOY);

    assertThat(c.soloLoMio()).isTrue();
    assertThat(c.deUnaCuenta()).isFalse();
    assertThat(c.conRango()).isTrue();
    assertThat(
            AdminActividadVista.de(
                    null, Tramo.de(java.util.List.of(), 1, 20), c, true, null, java.util.List.of())
                .fechas()
                .getLast()
                .texto())
        .isEqualTo("Del 1 al 15 de octubre");
  }
}
