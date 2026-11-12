package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteNotificacionesPuerto;
import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.DonanteNotificacionesVista;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Aviso;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Tema;
import grupo5.cliente.vistas.Tramo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Las notificaciones del donante con la página que da el puerto (R11-14). */
@WebMvcTest(controllers = {DonanteNotificacionesController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonanteNotificacionesControllerTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String AHORA = "2026-11-07T19:40";
  private static final List<Tema> TEMAS =
      List.of(
          new Tema("", "Todas las categorías", null),
          new Tema("asignadas", "Donaciones asignadas", "asigna"),
          new Tema("problemas", "Problemas en la entrega", "problema"),
          new Tema("otras", "Otras novedades", "mision"));

  @MockitoBean private DonanteNotificacionesPuerto notificaciones;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  private static DonanteNotificacionesVista dos(long total) {
    List<Aviso> avisos =
        List.of(
            new Aviso(
                "b1",
                "2026-11-07T18:00",
                "Subiste de categoría",
                "¡Felicitaciones! Has ascendido.",
                "otras",
                "/donante-donaciones",
                "Ver Mis donaciones",
                false),
            new Aviso(
                "b2",
                "2026-11-06T10:00",
                "Donación asignada",
                "Tu donación de Fideos fue asignada a Comedor",
                "asignadas",
                "/donante-donaciones",
                "Ver Mis donaciones",
                true));
    return new DonanteNotificacionesVista(
        new Tramo<>(avisos, total, 1, Tramo.paginas(total, ConsultaBandeja.TAMANIO)),
        3,
        TEMAS,
        AHORA);
  }

  @Test
  void muestraLaPaginaConTodasLasCategoriasYLasSinLeerDeLaCuenta() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(notificaciones.bandeja(eq(DONANTE), any(ConsultaBandeja.class))).thenReturn(dos(2));

    mvc.perform(get("/donante-notificaciones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("3 sin leer.")))
        .andExpect(content().string(containsString("Subiste de categoría")))
        .andExpect(content().string(containsString("¡Felicitaciones! Has ascendido.")))
        .andExpect(content().string(containsString("Ver Mis donaciones")))
        .andExpect(content().string(containsString("Otras novedades")))
        // El menú de Categoría muestra todas las del rol, también las que no tienen avisos.
        .andExpect(content().string(containsString("Problemas en la entrega")))
        // Con una sola página no hay paginador.
        .andExpect(content().string(not(containsString("Páginas de notificaciones"))));
  }

  @Test
  void losFiltrosYLaPaginaDeLaDireccionLleganAlPuertoYHayPaginador() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(notificaciones.bandeja(eq(DONANTE), any(ConsultaBandeja.class))).thenReturn(dos(45));

    mvc.perform(
            get("/donante-notificaciones")
                .param("ver", "sin-leer")
                .param("buscar", "fideos")
                .param("tema", "asignadas")
                .param("orden", "viejas")
                .param("pagina", "2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Páginas de notificaciones")));

    ArgumentCaptor<ConsultaBandeja> consulta = ArgumentCaptor.forClass(ConsultaBandeja.class);
    verify(notificaciones).bandeja(eq(DONANTE), consulta.capture());
    assertEquals(new ConsultaBandeja(true, "fideos", "asignadas", true, 2), consulta.getValue());
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-notificaciones")).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void unaBandejaVaciaMuestraElEstadoVacio() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(notificaciones.bandeja(eq(DONANTE), any(ConsultaBandeja.class)))
        .thenReturn(
            new DonanteNotificacionesVista(new Tramo<>(List.of(), 0, 1, 1), 0, TEMAS, AHORA));

    mvc.perform(get("/donante-notificaciones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Todavía no tenés notificaciones.")))
        .andExpect(content().string(not(containsString("Ninguna notificación coincide"))));
  }

  @Test
  void conFiltrosYNingunaQueCoincidaLoDice() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(notificaciones.bandeja(eq(DONANTE), any(ConsultaBandeja.class)))
        .thenReturn(
            new DonanteNotificacionesVista(new Tramo<>(List.of(), 0, 1, 1), 3, TEMAS, AHORA));

    mvc.perform(get("/donante-notificaciones").param("buscar", "zapallo"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Ninguna notificación coincide con lo que buscaste.")))
        .andExpect(content().string(not(containsString("Todavía no tenés notificaciones."))));
  }
}
