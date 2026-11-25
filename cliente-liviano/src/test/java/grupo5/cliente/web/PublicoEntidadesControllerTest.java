package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.PublicoEntidadesPuerto;
import grupo5.cliente.vistas.PublicoEntidadesVista;
import grupo5.cliente.vistas.PublicoEntidadesVista.Entidad;
import grupo5.cliente.vistas.PublicoEntidadesVista.Pedido;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Entidades y necesidades con lo que da el puerto, sin lo que la API pública no trae: sin
 * categorías, sin emergencias, sin misión ni lo cubierto del mes.
 */
@WebMvcTest(PublicoEntidadesController.class)
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, PublicoEntidadesControllerTest.DatosDeLaApi.class})
class PublicoEntidadesControllerTest {

  @TestConfiguration
  static class DatosDeLaApi {

    @Bean
    PublicoEntidadesPuerto entidades() {
      return () -> datos;
    }
  }

  /** Lo que devuelve el puerto; cada test lo arma. */
  private static PublicoEntidadesVista datos;

  @Autowired private MockMvc mvc;

  private static PublicoEntidadesVista conEntidades(Entidad... entidades) {
    return new PublicoEntidadesVista(
        List.of(entidades),
        31,
        "10 de noviembre de 2026",
        null,
        false,
        false,
        "Todavía no mostramos X.");
  }

  private static Entidad parroquia(String nombre, String tipo) {
    return new Entidad(
        "e2",
        tipo,
        nombre,
        null,
        null,
        null,
        null,
        null,
        List.of(
            new Pedido(
                "Frazadas",
                null,
                "Piden 20",
                "Sin fecha de cierre",
                null,
                20,
                "extraordinaria",
                false)));
  }

  @Test
  void muestraLasEntidadesDelPuertoYDiceLoQueNoSabe() throws Exception {
    datos = conEntidades(parroquia("Parroquia Ejemplo", "Otra"));

    mvc.perform(get("/publico-entidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Otras organizaciones")))
        .andExpect(content().string(containsString("Parroquia Ejemplo")))
        .andExpect(content().string(containsString("de las 31 que están en DonaTrack")))
        .andExpect(content().string(containsString("Al 10 de noviembre de 2026")))
        .andExpect(content().string(containsString("Piden 20")))
        .andExpect(content().string(containsString("Todavía no mostramos X.")))
        .andExpect(content().string(not(containsString("Necesidades satisfechas"))))
        .andExpect(content().string(not(containsString("Misión:"))))
        .andExpect(content().string(not(containsString("Emergencias"))))
        .andExpect(content().string(not(containsString("Todas las categorías"))))
        .andExpect(content().string(not(containsString("Comedor Barrio Ejemplo"))))
        // HG-04: sin lo que falta, la cuenta va en palabras y sin barra
        .andExpect(content().string(not(containsString("<progress"))));
  }

  private static Entidad comedor(int numero) {
    return new Entidad(
        "c" + numero,
        "Comedor",
        "Comedor Número " + numero,
        null,
        null,
        null,
        null,
        null,
        List.of(
            new Pedido(
                "Arroz", null, "Faltan 25 de 40 kg", "Se repite", 25, 40, "recurrente", false)));
  }

  @Test
  void pasaDePaginaDeASeisConservandoLosFiltros() throws Exception {
    datos =
        conEntidades(
            comedor(1), comedor(2), comedor(3), comedor(4), comedor(5), comedor(6), comedor(7));

    mvc.perform(get("/publico-entidades?pedido=recurrente"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Comedor Número 6")))
        .andExpect(content().string(not(containsString("Comedor Número 7"))))
        .andExpect(content().string(containsString("Página 1 de 2.")))
        .andExpect(content().string(containsString("aria-label=\"Páginas de organizaciones\"")))
        .andExpect(
            content().string(containsString("/publico-entidades?pedido=recurrente&amp;pagina=2")))
        // HG-04: con lo que falta de cuánto, la barra acompaña la cuenta
        .andExpect(content().string(containsString("aria-label=\"Arroz: faltan 25 de 40 kg\"")));

    mvc.perform(get("/publico-entidades?pedido=recurrente&pagina=2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Comedor Número 7")))
        .andExpect(content().string(not(containsString("Comedor Número 1<"))))
        .andExpect(content().string(containsString("Página 2 de 2.")));
  }

  @Test
  void conUnaSolaPaginaNoHayPaginador() throws Exception {
    datos = conEntidades(comedor(1), comedor(2));

    mvc.perform(get("/publico-entidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Comedor Número 2")))
        .andExpect(content().string(not(containsString("Páginas de organizaciones"))))
        .andExpect(content().string(not(containsString("Página 1 de"))));
  }

  @Test
  void unaEntidadDeUnTipoConocidoVaEnSuGrupo() throws Exception {
    datos = conEntidades(parroquia("Comedor del Sur", "Comedor"));

    mvc.perform(get("/publico-entidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Comedores y merenderos")))
        .andExpect(content().string(not(containsString("Otras organizaciones"))));
  }

  @Test
  void conUnNombreConApostrofoLaPaginaSigue() throws Exception {
    datos = conEntidades(parroquia("Hogar 'San José' \" __${1+1}__", "Hogar"));

    mvc.perform(get("/publico-entidades?q=hogar"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Hogares")))
        .andExpect(content().string(not(containsString("__2__"))));
  }

  private static Pedido pide(String que, String cat) {
    return new Pedido(que, cat, "Piden 10", "Se repite", null, 10, "recurrente", false);
  }

  @Test
  void lasNecesidadesSatisfechasDelMesNoCambianConLosFiltros() throws Exception {
    datos =
        new PublicoEntidadesVista(
            List.of(
                new Entidad(
                    "comedor",
                    "Comedor",
                    "Comedor Ejemplo",
                    "alimentos",
                    null,
                    3,
                    null,
                    null,
                    List.of(
                        pide("Arroz", "alimentos"),
                        pide("Fideos", "alimentos"),
                        pide("Camperas", "ropa"),
                        pide("Medias", "ropa"),
                        pide("Frazadas", "ropa")))),
            31,
            "10 de noviembre de 2026",
            "octubre de 2026",
            true,
            false,
            null);

    // el total del mes son todas sus necesidades vigentes, no solo las que pasan el filtro
    for (String ruta : List.of("/publico-entidades", "/publico-entidades?categoria=ropa")) {
      mvc.perform(get(ruta))
          .andExpect(status().isOk())
          .andExpect(
              content().string(containsString("Necesidades satisfechas en octubre: 3 de 8.")));
    }
  }

  @Test
  void sinEntidadesConPedidosMuestraElEstadoVacio() throws Exception {
    datos = conEntidades();

    mvc.perform(get("/publico-entidades"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("Ninguna organización tiene necesidades vigentes ahora.")));
  }
}
