package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.puertos.AdminOperacionPuerto.BienNuevo;
import grupo5.cliente.puertos.AdminOperacionPuerto.DonacionNueva;
import grupo5.cliente.puertos.AdministradorActual;
import grupo5.cliente.vistas.AdminDonacionesRegistroVista;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.AdminDonacionesVista.Donacion;
import grupo5.cliente.vistas.ConsultaDonaciones;
import grupo5.cliente.vistas.Tramo;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Donaciones fuera de la maqueta: el detalle de una donación que está (y el 404 de una que no),
 * marcarla vencida con el nombre del admin y el 409 si cambió a la vez.
 */
@WebMvcTest({AdminDonacionesController.class, AdminDonacionesEnvios.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({
  Pantallas.class,
  AdminCamionesEnviosTest.Cuenta.class,
  AdminDonacionesEnviosTest.Reloj.class
})
class AdminDonacionesEnviosTest {

  private static final String ID = "6f1c2c1e-0000-4000-8000-0000000000d1";
  // R11-10: el historial guarda la cuenta (el sub de la sesión), no el nombre del menú
  private static final String CUENTA_DE_LEO = "6f1c2c1e-0000-4000-8000-00000000001e";

  @TestConfiguration
  static class Reloj {

    @Bean
    Clock reloj() {
      return Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
    }
  }

  @MockitoBean private AdminOperacionPuerto operacion;
  @MockitoBean private AdministradorActual administrador;
  @Autowired private MockMvc mvc;

  private static final Donacion LECHE =
      new Donacion(
          ID,
          "Leche en polvo",
          "10 unidades",
          "10 u",
          "Ana Ejemplo",
          "8 de noviembre",
          "deposito",
          "12 de noviembre, en 2 días",
          true,
          "Nuevo",
          null,
          null,
          false,
          null,
          List.of("8 de noviembre: llegó al depósito."));

  @BeforeEach
  void unaDonacion() {
    when(operacion.donacion(ID, "")).thenReturn(LECHE);
    when(administrador.cuenta(any())).thenReturn(Optional.of(CUENTA_DE_LEO));
  }

  @Test
  void laListaPideSuPaginaConElFiltroYMuestraElPaginador() throws Exception {
    ConsultaDonaciones consulta = new ConsultaDonaciones("deposito", false, "antigua", 2);
    when(operacion.donaciones(consulta, ""))
        .thenReturn(
            new AdminDonacionesVista(
                new Tramo<>(List.of(LECHE), 21, 2, 2),
                consulta,
                List.of(
                    new AdminDonacionesVista.Criterio(
                        "", "Registradas más recientes", "Recientes."),
                    new AdminDonacionesVista.Criterio(
                        "antigua", "Registradas más antiguas", "Antiguas.")),
                false,
                null,
                "1 en el depósito."));

    mvc.perform(get("/admin-donaciones?estado=deposito&orden=antigua&pagina=2"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Leche en polvo")))
        .andExpect(content().string(containsString("Página 2 de 2")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/admin-donaciones?estado=deposito&amp;orden=antigua&amp;pagina=1\"")))
        .andExpect(content().string(not(containsString("En los próximos 7 días"))));
  }

  private static AdminDonacionesVista pagina2(ConsultaDonaciones consulta) {
    return new AdminDonacionesVista(
        new Tramo<>(List.of(LECHE), 41, 2, 3),
        consulta,
        List.of(new AdminDonacionesVista.Criterio("", "Registradas más recientes", "Recientes.")),
        false,
        null,
        "1 en el depósito.");
  }

  // R11-34 (ficha r11-p2-admin-bandeja §4): elegir un renglón de la página 2 la conserva
  @Test
  void elegirUnRenglonDeLaPagina2NoVuelveALaPrimera() throws Exception {
    when(operacion.donaciones(any(), any())).thenAnswer(i -> pagina2(i.getArgument(0)));
    mvc.perform(get("/admin-donaciones?estado=deposito&pagina=2&donacion=" + ID))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/admin-donaciones?estado=deposito&amp;pagina=2&amp;donacion="
                            + ID
                            + "#donacion-"
                            + ID
                            + "\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/admin-donaciones?estado=deposito&amp;pagina=2&amp;#donacion-"
                            + ID
                            + "\"")))
        .andExpect(content().string(containsString("aria-current=\"true\"")));
  }

  // R11-34 (§5): la pedida que no existe o no se pudo leer se avisa en el lugar de la ficha
  @Test
  void unaDonacionQueNoExisteONoCargaSeAvisaEnElLugarDeLaFicha() throws Exception {
    when(operacion.donaciones(any(), any())).thenAnswer(i -> pagina2(i.getArgument(0)));
    mvc.perform(get("/admin-donaciones?donacion=otra"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No encontramos esa donación.")))
        .andExpect(
            content()
                .string(containsString("Puede que el enlace esté mal; elegí una de la lista.")))
        .andExpect(content().string(containsString("Leche en polvo")))
        .andExpect(content().string(not(containsString("data-elegido=\"partida\""))));
    when(operacion.donacion("caida", "")).thenThrow(new ResourceAccessException("caída"));
    mvc.perform(get("/admin-donaciones?donacion=caida"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No pudimos cargar la ficha de esta donación.")))
        .andExpect(
            content().string(containsString("La lista sigue a mano; probá elegirla de nuevo.")))
        .andExpect(content().string(containsString("Leche en polvo")));
  }

  @Test
  void elDetalleMuestraLaDonacionYUnaQueNoEstaEsUn404() throws Exception {
    mvc.perform(get("/admin-donaciones-detalle?donacion=" + ID))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("8 de noviembre: llegó al depósito.")))
        .andExpect(content().string(containsString("Marcar como vencida")));
    mvc.perform(get("/admin-donaciones-detalle?donacion=otra")).andExpect(status().isNotFound());
    mvc.perform(get("/admin-donaciones-detalle")).andExpect(status().isNotFound());
    mvc.perform(get("/admin-donaciones-detalle-vencida")).andExpect(status().isNotFound());
  }

  @Test
  void marcarVencidaGuardaConLaCuentaDelAdminYVuelveAlDetalle() throws Exception {
    mvc.perform(
            post("/admin-donaciones-detalle-vencida")
                .param("donacion", ID)
                .param("motivo", "Se rompió el envase"))
        .andExpect(redirectedUrl("/admin-donaciones-detalle?donacion=" + ID))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO, "La marcaste como vencida el 10 de noviembre de 2026, 18:30."));
    verify(operacion).marcarVencida(ID, CUENTA_DE_LEO, "Se rompió el envase");
  }

  @Test
  void siCambioALaVezNoLaMarca() throws Exception {
    doThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null))
        .when(operacion)
        .marcarVencida(ID, CUENTA_DE_LEO, null);

    mvc.perform(post("/admin-donaciones-detalle-vencida").param("donacion", ID))
        .andExpect(redirectedUrl("/admin-donaciones-detalle?donacion=" + ID))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Esta donación cambió mientras la mirabas: no la marcamos como vencida."
                        + " Volvimos a cargar su estado."));
  }

  private static final String ANA = "6f1c2c1e-0000-4000-8000-0000000000a1";

  @Test
  void registrarBuscaDonantesYLasPantallasDeResultadoNoExisten() throws Exception {
    AdminDonacionesRegistroVista.Donante ana =
        new AdminDonacionesRegistroVista.Donante(ANA, "Ana Ejemplo", "DNI 20.000.001");
    when(operacion.registro("ana", null))
        .thenReturn(new AdminDonacionesRegistroVista(List.of(ana), List.of(ana), 1));
    when(operacion.registro("a", null))
        .thenReturn(new AdminDonacionesRegistroVista(List.of(), List.of(ana), 137));

    mvc.perform(get("/admin-donaciones-registrar?buscar=ana"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1 donante coincide con la búsqueda.")))
        .andExpect(content().string(containsString("donante=" + ANA)));
    mvc.perform(get("/admin-donaciones-registrar?buscar=a"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Hay 137 donantes que coinciden. Escribí más del nombre o el documento"
                            + " para encontrarlo.")));
    mvc.perform(get("/admin-donaciones-registrada")).andExpect(status().isNotFound());
    mvc.perform(get("/admin-donaciones-registrar-error")).andExpect(status().isNotFound());
  }

  @Test
  void registrarGuardaLosBienesConSuFotoYVuelveALaLista() throws Exception {
    mvc.perform(
            multipart("/admin-donaciones-registrada")
                .file(
                    new MockMultipartFile(
                        "bienes[0].foto", "arroz.jpg", "image/jpeg", new byte[] {1, 2, 3}))
                .param("donante", ANA)
                .param("descripcion", "Caja de alimentos")
                .param("bienes[0].que", "Arroz")
                .param("bienes[0].cantidad", "10")
                .param("bienes[0].vence", "2027-01-31")
                .param("bienes[0].estado", "Nuevo")
                .param("bienes[1].que", "Fideos")
                .param("bienes[1].cantidad", "5")
                .param("bienes[1].vence", "")
                .param("bienes[1].estado", "Usado")
                .param("deposito", "Depósito Central")
                .param("recepcion", "2026-11-10"))
        .andExpect(redirectedUrl("/admin-donaciones"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Registraste la donación: Caja de alimentos, 2 bienes. Quedó en el Depósito"
                        + " Central."));
    ArgumentCaptor<DonacionNueva> guardada = ArgumentCaptor.forClass(DonacionNueva.class);
    verify(operacion).registrarDonacion(guardada.capture());
    DonacionNueva donacion = guardada.getValue();
    assertEquals(ANA, donacion.donante());
    assertEquals(LocalDate.of(2026, 11, 10), donacion.recepcion());
    BienNuevo arroz = donacion.bienes().get(0);
    assertEquals("NUEVO", arroz.estado());
    assertEquals(LocalDate.of(2027, 1, 31), arroz.vence());
    assertEquals("image/jpeg", arroz.tipoFoto());
    BienNuevo fideos = donacion.bienes().get(1);
    assertEquals(5, fideos.cantidad());
    assertNull(fideos.vence());
    assertNull(fideos.foto());
  }

  @Test
  void sinDonanteElegidoODadoDeBajaVuelveAlFormulario() throws Exception {
    mvc.perform(
            multipart("/admin-donaciones-registrada")
                .param("descripcion", "Caja")
                .param("deposito", "Depósito Central")
                .param("recepcion", "2026-11-10"))
        .andExpect(redirectedUrl("/admin-donaciones-registrar"))
        .andExpect(flash().attribute(Envios.AVISO, "Elegí quién dona para registrar la donación."));
    doThrow(
            HttpClientErrorException.create(
                HttpStatus.CONFLICT,
                "Conflict",
                null,
                "{\"code\": \"ERR-EST-211\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8))
        .when(operacion)
        .registrarDonacion(any());
    mvc.perform(
            multipart("/admin-donaciones-registrada")
                .param("donante", ANA)
                .param("descripcion", "Caja")
                .param("deposito", "Depósito Central")
                .param("recepcion", "2026-11-10"))
        .andExpect(redirectedUrl("/admin-donaciones-registrar"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Ese donante está dado de baja: no se le pueden registrar donaciones. Si"
                        + " volvió, registralo de nuevo en Donantes."));
  }

  @Test
  void unaCantidadQueNoEsUnNumeroVuelveAlFormularioSinGuardar() throws Exception {
    mvc.perform(
            multipart("/admin-donaciones-registrada")
                .param("donante", ANA)
                .param("descripcion", "Caja")
                .param("bienes[0].que", "Arroz")
                .param("bienes[0].cantidad", "veinte")
                .param("deposito", "Depósito Central")
                .param("recepcion", "2026-11-10"))
        .andExpect(redirectedUrl("/admin-donaciones-registrar?donante=" + ANA))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "La cantidad del bien 1 tiene que ser un número. Por ejemplo: 20."));
    verifyNoInteractions(operacion);
  }
}
