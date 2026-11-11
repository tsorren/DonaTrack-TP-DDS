package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.PublicoMapaPuerto;
import grupo5.cliente.vistas.PublicoMapaVista;
import grupo5.cliente.vistas.PublicoMapaVista.Entrega;
import grupo5.cliente.vistas.PublicoMapaVista.Estacion;
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
 * El mapa con lo que da el puerto, sin lo que Logística no trae: sin entidad, categoría ni foto,
 * con el punto como un lugar.
 */
@WebMvcTest(PublicoMapaController.class)
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, PublicoMapaControllerTest.DatosDeLaApi.class})
class PublicoMapaControllerTest {

  @TestConfiguration
  static class DatosDeLaApi {

    @Bean
    PublicoMapaPuerto mapa() {
      return () -> datos;
    }
  }

  /** Lo que devuelve el puerto; cada test lo arma. */
  private static PublicoMapaVista datos;

  @Autowired private MockMvc mvc;

  private static PublicoMapaVista conLugares(Estacion... estaciones) {
    return new PublicoMapaVista(
        List.of(estaciones),
        "del 11 de octubre al 10 de noviembre de 2026",
        "10 de noviembre de 2026, 14:30",
        false,
        false,
        "Todavía no mostramos X.");
  }

  private static Estacion palermo() {
    return new Estacion(
        "lugar-1",
        1,
        -34.6037,
        -58.3816,
        "Palermo",
        "Buenos Aires",
        List.of(),
        List.of(
            new Entrega(
                "Entrega de 12,5 kg",
                null,
                null,
                "2 de noviembre",
                "2026",
                "2026-11-02",
                "Palermo",
                null,
                null)));
  }

  @Test
  void muestraLosLugaresDelPuertoYDiceLoQueNoSabe() throws Exception {
    datos = conLugares(palermo());

    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Lugar por lugar")))
        .andExpect(content().string(containsString("Palermo")))
        .andExpect(content().string(containsString("Entrega de 12,5 kg · 2 de noviembre")))
        .andExpect(content().string(containsString("data-lat=\"-34.6037\"")))
        .andExpect(content().string(containsString("en 1 lugar, del 11 de octubre")))
        .andExpect(
            content().string(containsString("Actualizado el 10 de noviembre de 2026, 14:30")))
        .andExpect(content().string(containsString("Mostramos la localidad, nunca la dirección")))
        .andExpect(content().string(containsString("Todavía no mostramos X.")))
        .andExpect(content().string(not(containsString("Ver la foto de la entrega</button>"))))
        .andExpect(content().string(not(containsString("Qué se donó"))))
        .andExpect(content().string(not(containsString("Pide:"))))
        .andExpect(content().string(not(containsString("Comedor Barrio Ejemplo"))));
  }

  @Test
  void unLugarSinProvinciaNoLlevaElSeparadorSuelto() throws Exception {
    Estacion palermo = palermo();
    datos =
        conLugares(
            new Estacion(
                palermo.clave(),
                palermo.numero(),
                palermo.lat(),
                palermo.lng(),
                palermo.nombre(),
                "",
                palermo.pide(),
                palermo.entregas()));

    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">1 entrega confirmada<")))
        .andExpect(content().string(not(containsString(" · 1 entrega confirmada"))));
  }

  @Test
  void sinEntregasMuestraElEstadoVacio() throws Exception {
    datos = conLugares();

    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("Todavía no hay entregas confirmadas en el último mes.")));
  }
}
