package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.fotos.SolicitudSubidaFotoInputDTO;
import grupo5.donaciones.dto.fotos.SubidaFotoOutputDTO;
import grupo5.donaciones.fixtures.AlmacenamientoFixtures;
import grupo5.donaciones.infrastructure.almacenamiento.StorageEnMemoria;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.services.impl.FotosBienesService;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

class FotosBienesServiceTest {

  private static final Instant AHORA = Instant.parse("2026-10-28T12:00:00Z");
  private static final String CLAVE = "3f2b9c1e-7a4d-4e8f-9b0a-1c2d3e4f5a6b.jpg";
  private static final String REFERENCIA = "/fotos-bienes/" + CLAVE;

  private StoragePort storage;
  private FotosBienesService service;

  @BeforeEach
  void setUp() {
    storage = new StorageEnMemoria();
    service =
        new FotosBienesService(
            storage, AlmacenamientoFixtures.propiedades(), Clock.fixed(AHORA, ZoneOffset.UTC));
  }

  @Test
  void solicitarSubida_deberiaDevolverUrlDeSubidaYUnaReferenciaConClavePropia() {
    SubidaFotoOutputDTO subida =
        service.solicitarSubida(new SolicitudSubidaFotoInputDTO("image/jpeg"));

    assertEquals("PUT", subida.metodo());
    assertEquals("image/jpeg", subida.tipoContenido());
    assertTrue(
        subida.referencia().matches("/fotos-bienes/[0-9a-f-]{36}\\.jpg"), subida.referencia());
    String clave = subida.referencia().substring("/fotos-bienes/".length());
    assertEquals("memoria://donaciones-fotos-bienes/" + clave, subida.uploadUrl());
    assertEquals(AHORA.plusSeconds(15 * 60), subida.expiraEn());
  }

  @Test
  void solicitarSubida_deberiaNormalizarElTipoYElegirLaExtension() {
    SubidaFotoOutputDTO subida =
        service.solicitarSubida(new SolicitudSubidaFotoInputDTO(" IMAGE/WEBP; q=1 "));

    assertEquals("image/webp", subida.tipoContenido());
    assertTrue(subida.referencia().endsWith(".webp"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"image/gif", "application/pdf", "text/html", "image/svg+xml"})
  void solicitarSubida_conTipoNoPermitidoDeberiaRechazarla(String tipo) {
    SolicitudSubidaFotoInputDTO solicitud = new SolicitudSubidaFotoInputDTO(tipo);

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.solicitarSubida(solicitud));

    assertEquals(ErrorCatalog.FOTO_BIEN_TIPO_NO_PERMITIDO, ex.getError());
  }

  @Test
  void validarFoto_conLaFotoSubidaYValidaNoDeberiaFallar() {
    subir(CLAVE, 1024, "image/jpeg");

    assertDoesNotThrow(() -> service.validarFoto(REFERENCIA));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"https://externa/foto.png", "http://fotos/alimento.png"})
  void validarFoto_sinFotoOConUrlExternaNoDeberiaValidarNada(String fotoUrl) {
    assertDoesNotThrow(() -> service.validarFoto(fotoUrl));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/fotos-bienes/../donaciones-padrones/x.csv",
        "/fotos-bienes/padron.csv",
        "/fotos-bienes/",
        "/fotos-bienes/3f2b9c1e-7a4d-4e8f-9b0a-1c2d3e4f5a6b.exe"
      })
  void validarFoto_conReferenciaMalFormadaDeberiaRechazarla(String referencia) {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.validarFoto(referencia));

    assertEquals(ErrorCatalog.FOTO_BIEN_REFERENCIA_INVALIDA, ex.getError());
  }

  @Test
  void validarFoto_queNoSeSubioDeberiaRechazarla() {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.validarFoto(REFERENCIA));

    assertEquals(ErrorCatalog.FOTO_BIEN_NO_ENCONTRADA, ex.getError());
  }

  @Test
  void validarFoto_subidaConOtroTipoDeberiaRechazarla() {
    subir(CLAVE, 10, "text/html");

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.validarFoto(REFERENCIA));

    assertEquals(ErrorCatalog.FOTO_BIEN_TIPO_NO_PERMITIDO, ex.getError());
  }

  @Test
  void validarFoto_mayorAlMaximoDeberiaRechazarla() {
    subir(CLAVE, (int) DataSize.ofMegabytes(10).toBytes() + 1, "image/jpeg");

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.validarFoto(REFERENCIA));

    assertEquals(ErrorCatalog.FOTO_BIEN_TAMANIO_EXCEDIDO, ex.getError());
  }

  @Test
  void urlParaMostrar_conReferenciaDeberiaFirmarUnaUrlDeDescarga() {
    assertEquals("memoria://donaciones-fotos-bienes/" + CLAVE, service.urlParaMostrar(REFERENCIA));
  }

  @ParameterizedTest
  @ValueSource(strings = {"https://externa/foto.png", "/fotos-bienes/../x", ""})
  void urlParaMostrar_conCualquierOtroValorDeberiaDevolverloSinCambios(String fotoUrl) {
    assertEquals(fotoUrl, service.urlParaMostrar(fotoUrl));
  }

  @Test
  void urlParaMostrar_sinFotoDeberiaDevolverNull() {
    assertNull(service.urlParaMostrar(null));
  }

  private void subir(String clave, int tamanio, String tipo) {
    storage.guardar(
        AlmacenamientoFixtures.BUCKET_FOTOS,
        clave,
        new ByteArrayInputStream(new byte[tamanio]),
        tamanio,
        tipo);
  }
}
