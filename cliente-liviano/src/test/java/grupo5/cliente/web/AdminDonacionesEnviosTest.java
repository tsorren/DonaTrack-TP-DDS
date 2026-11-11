package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
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
import grupo5.cliente.vistas.AdminDonacionesRegistroVista;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.AdminDonacionesVista.Donacion;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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

  @TestConfiguration
  static class Reloj {

    @Bean
    Clock reloj() {
      return Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
    }
  }

  @MockitoBean private AdminOperacionPuerto operacion;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void unaDonacion() {
    when(operacion.donaciones(""))
        .thenReturn(
            new AdminDonacionesVista(
                List.of(
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
                        List.of("8 de noviembre: llegó al depósito."))),
                Map.of(),
                "1 en el depósito."));
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
  void marcarVencidaGuardaConElNombreDelAdminYVuelveAlDetalle() throws Exception {
    mvc.perform(
            post("/admin-donaciones-detalle-vencida")
                .param("donacion", ID)
                .param("motivo", "Se rompió el envase"))
        .andExpect(redirectedUrl("/admin-donaciones-detalle?donacion=" + ID))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO, "La marcaste como vencida el 10 de noviembre de 2026, 18:30."));
    verify(operacion).marcarVencida(ID, "Leo Ejemplo", "Se rompió el envase");
  }

  @Test
  void siCambioALaVezNoLaMarca() throws Exception {
    doThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null))
        .when(operacion)
        .marcarVencida(ID, "Leo Ejemplo", null);

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
    when(operacion.registro("ana"))
        .thenReturn(new AdminDonacionesRegistroVista(List.of(ana), List.of(ana)));

    mvc.perform(get("/admin-donaciones-registrar?buscar=ana"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1 donante coincide con la búsqueda.")))
        .andExpect(content().string(containsString("donante=" + ANA)));
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
