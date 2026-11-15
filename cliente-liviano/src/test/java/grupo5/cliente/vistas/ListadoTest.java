package grupo5.cliente.vistas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** La barra del componente listado (R11-B04): lo que cuenta como elegido y los textos comunes. */
class ListadoTest {

  private static final List<Listado.Opcion> ESTADOS =
      List.of(new Listado.Opcion("", "Todas"), new Listado.Opcion("sin-leer", "Sin leer"));

  private static Listado.Armado barra() {
    return Listado.en("prueba", "/prueba", "Filtrar la prueba")
        .buscar("Buscar", "", "Por ejemplo: algo")
        .sacar("/prueba");
  }

  @Test
  void laPrimeraOpcionDiceLoMismoEnTodasLasListas() {
    Listado l =
        barra()
            .estado("", ESTADOS)
            .categoria("", List.of(new Listado.Opcion("ropa", "Ropa")))
            .listo();

    assertThat(l.estado().opciones())
        .containsExactly(
            new Listado.Opcion("", Listado.TODOS_LOS_ESTADOS),
            new Listado.Opcion("sin-leer", "Sin leer"));
    assertThat(l.categoria().opciones().getFirst())
        .isEqualTo(new Listado.Opcion("", Listado.TODAS_LAS_CATEGORIAS));

    Listado persona = l.conCategorias(List.of(Map.of("valor", "", "texto", "Todas")));
    assertThat(persona.categoria().opciones())
        .containsExactly(new Listado.Opcion("", Listado.TODAS_LAS_CATEGORIAS));
  }

  @Test
  void loQueSoloCambiaComoSeVeNoCuentaComoFiltro() {
    Listado soloAgrupar =
        barra()
            .otra(
                Listado.Eleccion.deVista(
                    "agrupar",
                    "Agrupar",
                    "fecha",
                    List.of(
                        new Listado.Opcion("", "Sin agrupar"),
                        new Listado.Opcion("fecha", "Por fecha"))))
            .listo();

    assertThat(soloAgrupar.elegidos()).isZero();
    assertThat(soloAgrupar.filtrada()).isFalse();
    assertThat(soloAgrupar.conSacar()).isFalse();
  }

  @Test
  void elOrdenCuentaParaElBotonPeroNoFiltraYUnValorQueNoEstaNoCuenta() {
    Listado conOrden = barra().orden(Listado.ANTIGUAS, Listado.POR_FECHA).listo();
    assertThat(conOrden.elegidos()).isEqualTo(1);
    assertThat(conOrden.filtrada()).isFalse();

    Listado valorAjeno = barra().estado("no-existe", ESTADOS).listo();
    assertThat(valorAjeno.elegidos()).isZero();
  }

  @Test
  void lasFechasAlRevesSeDanVueltaYUnaMalEscritaNoAcota() {
    assertThat(Listado.rango("2026-10-20", "2026-10-10"))
        .containsExactly(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20));
    assertThat(Listado.rango("ayer", "2026-10-10"))
        .containsExactly(null, LocalDate.of(2026, 10, 10));
    assertThat(Listado.entre(null, null, null)).isTrue();
    assertThat(Listado.entre(null, LocalDate.of(2026, 10, 1), null)).isFalse();
    assertThat(Listado.contiene("Fundación Ejemplo Sur", "FUNDACION")).isTrue();
  }

  /** El botón que pliega los campos dice lo que hay adentro y solo aparece con dos o más campos. */
  @Test
  void elPliegueDependeDeLosCamposQueTieneLaBarra() {
    Listado soloBuscar = barra().listo();
    assertThat(soloBuscar.soloBusqueda()).isTrue();
    assertThat(soloBuscar.conPliegue()).isFalse();

    Listado unCampo = barra().otra(new Listado.Eleccion("periodo", "Período", "", ESTADOS)).listo();
    assertThat(unCampo.soloBusqueda()).isFalse();
    assertThat(unCampo.conPliegue()).isFalse();

    Listado filtros = barra().estado("", ESTADOS).categoria("", ESTADOS).listo();
    assertThat(filtros.conPliegue()).isTrue();
    assertThat(filtros.rotuloDelPliegue()).isEqualTo("Filtros");

    Listado conOrden = barra().estado("", ESTADOS).orden("", Listado.POR_FECHA).listo();
    assertThat(conOrden.rotuloDelPliegue()).isEqualTo("Filtros y orden");

    Listado soloOrden =
        barra()
            .otra(Listado.Eleccion.deVista("agrupar", "Agrupar", "", ESTADOS))
            .orden("", Listado.POR_FECHA)
            .listo();
    assertThat(soloOrden.rotuloDelPliegue()).isEqualTo("Orden");
    assertThat(soloOrden.otra("agrupar").rotulo()).isEqualTo("Agrupar");
    assertThat(soloOrden.otra("no-esta")).isNull();
  }
}
