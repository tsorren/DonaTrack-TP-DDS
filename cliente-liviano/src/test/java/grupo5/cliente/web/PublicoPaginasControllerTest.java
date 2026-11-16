package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.vistas.PublicoCifra;
import grupo5.cliente.vistas.PublicoEntregas;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Las páginas públicas con lo que da el puerto, no con los datos de ejemplo de la plantilla. */
@WebMvcTest({
  PublicoPortadaController.class,
  PublicoNosotrosController.class,
  PublicoDonacionesController.class
})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, PublicoPaginasControllerTest.DatosDeLaApi.class})
class PublicoPaginasControllerTest {

  @TestConfiguration
  static class DatosDeLaApi {

    @Bean
    PublicoPuerto publico() {
      List<PublicoCifra> cifras =
          List.of(
              new PublicoCifra("7", "Entregas confirmadas"),
              new PublicoCifra("3", "Entidades en el mapa"),
              new PublicoCifra("5", "Donantes con al menos una donación"));
      return new PublicoPuerto() {
        @Override
        public List<PublicoCifra> cifrasDePortada() {
          return cifras;
        }

        @Override
        public List<PublicoCifra> cifrasDeNosotros() {
          return cifras;
        }

        @Override
        public PublicoEntregas entregasConfirmadas() {
          return entregas;
        }
      };
    }
  }

  /** Lo que devuelve el puerto; cada test lo arma. */
  private static PublicoEntregas entregas;

  @Autowired private MockMvc mvc;

  private static PublicoEntregas conEntregas(PublicoEntregas.Entrega... lista) {
    return new PublicoEntregas(
        List.of(lista),
        "del 11 de octubre al 10 de noviembre de 2026",
        "del 3 al 10 de noviembre de 2026",
        "10 de noviembre de 2026, 14:30");
  }

  @Test
  void lasEntregasConfirmadasSonLasDelPuertoConSuPeriodoYSuFecha() throws Exception {
    entregas =
        conEntregas(
            new PublicoEntregas.Entrega(
                "Arroz, 5 kg",
                null,
                "Otra categoría",
                "Comedor Barrio Ejemplo",
                "Comedor Barrio Ejemplo",
                "9 de noviembre",
                "2026",
                "2026-11-09",
                true,
                "https://fotos/2.jpg",
                "Foto de Arroz, 5 kg"));

    mvc.perform(get("/publico-donaciones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Otra categoría")))
        .andExpect(content().string(containsString("9 de noviembre de 2026")))
        .andExpect(content().string(containsString("del 11 de octubre al 10 de noviembre de 2026")))
        .andExpect(
            content().string(containsString("Actualizado el 10 de noviembre de 2026, 14:30")))
        .andExpect(content().string(not(containsString("Lentejas"))));
    mvc.perform(get("/publico-donaciones?periodo=semana"))
        .andExpect(content().string(containsString("del 3 al 10 de noviembre de 2026")));
  }

  @Test
  void sinEntregasConfirmadasMuestraElEstadoVacio() throws Exception {
    entregas = conEntregas();

    mvc.perform(get("/publico-donaciones"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("Todavía no hay entregas confirmadas en el último mes.")));
  }

  @Test
  void laPortadaEnSusDosDireccionesMuestraLasCifrasDelPuerto() throws Exception {
    for (String ruta : List.of("/", "/publico-portada")) {
      mvc.perform(get(ruta))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("<b>7</b>")))
          .andExpect(content().string(containsString("Donantes con al menos una donación")))
          .andExpect(content().string(not(containsString("1.284"))));
    }
  }

  @Test
  void nosotrosMuestraLasCifrasDelPuerto() throws Exception {
    mvc.perform(get("/publico-nosotros"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<b>3</b>")))
        .andExpect(content().string(not(containsString("318"))));
  }
}
