package grupo5.cliente.vistas;

import static org.assertj.core.api.Assertions.assertThat;

import grupo5.cliente.vistas.PublicoEntidadesVista.Entidad;
import grupo5.cliente.vistas.PublicoEntidadesVista.Pedido;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El filtro, el orden, la página y los grupos de Organizaciones públicas (PUB1-05). */
class ConsultaOrganizacionesTest {

  private static Pedido pedido(String cat, String tipo, boolean emergencia) {
    return new Pedido("Algo", cat, "Faltan 3", "Cada mes", 3, 10, tipo, emergencia);
  }

  private static Entidad entidad(String clave, String tipo, String nombre, Pedido... pedidos) {
    return new Entidad(clave, tipo, nombre, null, null, null, null, null, List.of(pedidos));
  }

  private static final PublicoEntidadesVista DATOS =
      new PublicoEntidadesVista(
          List.of(
              entidad("club", "Club", "Club Ejemplo", pedido("ropa", "recurrente", false)),
              entidad(
                  "hogar",
                  "Hogar",
                  "Hogar Ejemplo",
                  pedido("alimentos", "recurrente", false),
                  pedido("alimentos", "extraordinaria", true)),
              entidad(
                  "comedor",
                  "Comedor",
                  "Comedor Ejemplo",
                  pedido("alimentos", "recurrente", false)),
              entidad(
                  "otra", "Otra", "Centro Ejemplo", pedido("muebles", "extraordinaria", false))),
          4,
          "15 de octubre de 2026",
          "octubre de 2026",
          true,
          true,
          null);

  @Test
  void sinFiltrosOrdenaPorTipoYAgrupa() {
    ConsultaOrganizaciones c = new ConsultaOrganizaciones(null, null, null, false, 1);

    Tramo<Entidad> pagina = c.pagina(DATOS, 6);

    assertThat(c.filtrada()).isFalse();
    assertThat(pagina.filas())
        .extracting(Entidad::clave)
        .containsExactly("comedor", "hogar", "club", "otra");
    assertThat(ConsultaOrganizaciones.porGrupo(pagina.filas()).keySet())
        .containsExactly("Comedores y merenderos", "Hogares", "Clubes", "Otras organizaciones");
  }

  @Test
  void losFiltrosDejanSoloLasNecesidadesQueCumplenYPrimeroLasEmergencias() {
    ConsultaOrganizaciones c = new ConsultaOrganizaciones(" ", "alimentos", "", false, 1);

    Tramo<Entidad> pagina = c.pagina(DATOS, 6);

    assertThat(c.filtrada()).isTrue();
    assertThat(pagina.filas()).extracting(Entidad::clave).containsExactly("comedor", "hogar");
    assertThat(pagina.filas().get(1).pedidos())
        .extracting(Pedido::emergencia)
        .containsExactly(true, false);
    assertThat(new ConsultaOrganizaciones("", "", "", true, 1).pagina(DATOS, 6).filas())
        .extracting(Entidad::clave)
        .containsExactly("hogar");
  }

  @Test
  void buscaPorNombreSinMirarMayusculas() {
    Tramo<Entidad> pagina = new ConsultaOrganizaciones(" CLUB ", "", "", false, 1).pagina(DATOS, 6);

    assertThat(pagina.filas()).extracting(Entidad::clave).containsExactly("club");
  }

  @Test
  void cortaLaPaginaYCuentaLasVigentesSinFiltrar() {
    Tramo<Entidad> pagina = new ConsultaOrganizaciones("", "", "", false, 9).pagina(DATOS, 3);

    assertThat(pagina.pagina()).isEqualTo(2);
    assertThat(pagina.paginas()).isEqualTo(2);
    assertThat(pagina.total()).isEqualTo(4);
    assertThat(pagina.filas()).extracting(Entidad::clave).containsExactly("otra");
    assertThat(ConsultaOrganizaciones.vigentes(DATOS))
        .containsEntry("hogar", 2)
        .containsEntry("club", 1)
        .hasSize(4);
  }
}
