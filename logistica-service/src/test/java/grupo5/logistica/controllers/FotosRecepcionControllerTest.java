package grupo5.logistica.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.model.Request;
import com.jayway.jsonpath.JsonPath;
import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.common.testutils.OpenApiTestUtils;
import grupo5.logistica.controllers.impl.FotosRecepcionController;
import grupo5.logistica.infrastructure.almacenamiento.AlmacenFotosEnMemoria;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.impl.FotosRecepcionService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@code POST} y {@code GET /api/entregas/{id}/fotos} con el servicio real y el almacén en memoria:
 * solo el repositorio de entregas es un doble.
 */
@WebMvcTest(
    controllers = FotosRecepcionController.class,
    properties = "logistica.fotos.url-publica-base=http://gateway.test/")
@Import({
  CommonLibAutoConfiguration.class,
  LoggingAutoConfiguration.class,
  FotosRecepcionService.class,
  FotosRecepcionControllerTest.Almacen.class
})
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("logistica-webmvc-context")
class FotosRecepcionControllerTest {

  private static final UUID ID = UUID.randomUUID();
  private static final String RUTA = "/api/entregas/" + ID + "/fotos";

  @TestConfiguration
  static class Almacen {
    @Bean
    AlmacenFotosEnMemoria almacenFotosRecepcion() {
      return new AlmacenFotosEnMemoria();
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private AlmacenFotosEnMemoria almacen;
  @MockitoBean private IEntregasRepository entregasRepository;

  @BeforeEach
  void entregaEnTraslado() {
    entregaEnEstado(EstadoEntrega.EN_TRASLADO);
  }

  @Test
  void subir_devuelve201ConLaUrlEstable_yLaFotoQuedaGuardada() throws Exception {
    MvcResult result =
        mockMvc
            .perform(multipart(RUTA).file(foto("png")).file(foto("jpeg")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.length()").value(2))
            .andReturn();

    String json = result.getResponse().getContentAsString();
    String fotoId = JsonPath.read(json, "$[0].fotoId");
    String url = JsonPath.read(json, "$[0].url");
    assertThat(url).isEqualTo("http://gateway.test" + RUTA + "/" + fotoId);
    assertThat(almacen.existe("entregas/" + ID + "/" + fotoId + ".png")).isTrue();
    OpenApiTestUtils.assertResponseConformsToOpenApi(
        OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml"),
        "/api/entregas/{id}/fotos",
        Request.Method.POST,
        201,
        json);
  }

  @Test
  void subir_unPdf_da415() throws Exception {
    MockMultipartFile pdf =
        new MockMultipartFile(
            "foto", "recibo.jpg", "image/jpeg", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

    mockMvc.perform(multipart(RUTA).file(pdf)).andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void subir_cabeceraJpegIlegible_da400() throws Exception {
    byte[] basura = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5};

    mockMvc
        .perform(multipart(RUTA).file(new MockMultipartFile("foto", basura)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void subir_masDeCincoMegabytes_da413() throws Exception {
    byte[] grande = new byte[5 * 1024 * 1024 + 1];
    grande[0] = (byte) 0xFF;
    grande[1] = (byte) 0xD8;
    grande[2] = (byte) 0xFF;

    mockMvc
        .perform(multipart(RUTA).file(new MockMultipartFile("foto", grande)))
        .andExpect(status().isContentTooLarge());
  }

  @Test
  void subir_aUnaEntregaYaEntregada_da409() throws Exception {
    entregaEnEstado(EstadoEntrega.ENTREGADA);

    mockMvc.perform(multipart(RUTA).file(foto("png"))).andExpect(status().isConflict());
  }

  @Test
  void subir_aUnaEntregaInexistente_da404() throws Exception {
    when(entregasRepository.findById(ID)).thenReturn(Optional.empty());

    mockMvc.perform(multipart(RUTA).file(foto("png"))).andExpect(status().isNotFound());
  }

  @Test
  void leer_redirigeConUn302AlaUrlFirmada() throws Exception {
    UUID fotoId = UUID.randomUUID();
    almacen.guardar("entregas/" + ID + "/" + fotoId + ".jpg", new byte[] {1}, "image/jpeg");

    mockMvc
        .perform(get(RUTA + "/" + fotoId))
        .andExpect(status().isFound())
        .andExpect(
            header()
                .string(
                    "Location",
                    "memoria://logistica-fotos-recepcion/entregas/" + ID + "/" + fotoId + ".jpg"));
  }

  @Test
  void leer_unPng_buscaLaClaveConExtensionPng() throws Exception {
    UUID fotoId = UUID.randomUUID();
    almacen.guardar("entregas/" + ID + "/" + fotoId + ".png", new byte[] {1}, "image/png");

    mockMvc
        .perform(get(RUTA + "/" + fotoId))
        .andExpect(status().isFound())
        .andExpect(
            header()
                .string(
                    "Location",
                    "memoria://logistica-fotos-recepcion/entregas/" + ID + "/" + fotoId + ".png"));
  }

  @Test
  void subir_unaParteVacia_da400() throws Exception {
    mockMvc
        .perform(multipart(RUTA).file(new MockMultipartFile("foto", new byte[0])))
        .andExpect(status().isBadRequest());
  }

  @Test
  void leer_unaFotoQueNoExiste_da404() throws Exception {
    mockMvc.perform(get(RUTA + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
  }

  private void entregaEnEstado(EstadoEntrega estado) {
    Entrega entrega = mock(Entrega.class);
    when(entrega.getEstadoActual()).thenReturn(estado);
    when(entregasRepository.findById(ID)).thenReturn(Optional.of(entrega));
  }

  private static MockMultipartFile foto(String formato) throws IOException {
    BufferedImage imagen = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(imagen, formato, salida);
    return new MockMultipartFile(
        "foto", "foto." + formato, "image/" + formato, salida.toByteArray());
  }
}
