package grupo5.auth.correo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventoEnlaceDeCuentaTest {

  private static final Instant FECHA = Instant.parse("2026-11-12T02:07:56Z");

  private static EventoEnlaceDeCuenta evento(String tipo, String enlace) {
    return new EventoEnlaceDeCuenta(tipo, "cuenta-1", "ana@example.com", enlace, 30, "es", FECHA);
  }

  @Test
  void aJsonTieneLosCamposDelContratoEnOrden() {
    String json = evento(EventoEnlaceDeCuenta.RECUPERAR_CLAVE, "http://kc/enlace").aJson();

    assertEquals(
        "{\"tipo\":\"RECUPERAR_CLAVE\",\"cuentaId\":\"cuenta-1\","
            + "\"destinatario\":\"ana@example.com\",\"enlace\":\"http://kc/enlace\","
            + "\"venceEnMinutos\":30,\"idioma\":\"es\",\"fecha\":\"2026-11-12T02:07:56Z\"}",
        json);
  }

  @Test
  void laFechaSeEmiteSinFraccionesDeSegundo() {
    EventoEnlaceDeCuenta conFracciones =
        new EventoEnlaceDeCuenta(
            EventoEnlaceDeCuenta.RECUPERAR_CLAVE,
            "cuenta-1",
            "ana@example.com",
            "http://kc/enlace",
            30,
            "es",
            Instant.parse("2026-11-12T02:07:56.987654321Z"));

    assertTrue(conFracciones.aJson().contains("\"fecha\":\"2026-11-12T02:07:56Z\""));
  }

  @Test
  void elEnlaceConAmpersandsIgualesYBarrasViajaTalCual() {
    String enlace =
        "http://kc/realms/d/login-actions/action-token?key=a.b-c_d&client_id=x&tab_id=1";

    assertTrue(evento(EventoEnlaceDeCuenta.VERIFICAR_EMAIL, enlace).aJson().contains(enlace));
  }

  @Test
  void escapaComillasBarrasInvertidasYSaltosDeLinea() {
    assertEquals("\"a\\\"b\"", EventoEnlaceDeCuenta.texto("a\"b"));
    assertEquals("\"a\\\\b\"", EventoEnlaceDeCuenta.texto("a\\b"));
    assertEquals("\"a\\nb\\rc\\td\"", EventoEnlaceDeCuenta.texto("a\nb\rc\td"));
  }

  @Test
  void escapaLosCaracteresDeControlYDejaPasarLosAcentos() {
    assertEquals("\"\\u0001\"", EventoEnlaceDeCuenta.texto("\u0001"));
    assertEquals(
        "\"Mar\u00eda \u00f1and\u00fa\"", EventoEnlaceDeCuenta.texto("Mar\u00eda \u00f1and\u00fa"));
  }

  @Test
  void unTextoMaliciosoNoRompeLaEstructuraDelJson() {
    String json = evento(EventoEnlaceDeCuenta.RECUPERAR_CLAVE, "x\",\"tipo\":\"OTRO").aJson();

    assertTrue(json.contains("\"enlace\":\"x\\\",\\\"tipo\\\":\\\"OTRO\""));
    assertEquals(1, json.split("\"tipo\":\"RECUPERAR_CLAVE\"", -1).length - 1);
  }

  @Test
  void lasConstantesSonLasDelContrato() {
    assertEquals("cuenta.enlace-solicitado.v1", EventoEnlaceDeCuenta.ROUTING_KEY);
    assertEquals("RECUPERAR_CLAVE", EventoEnlaceDeCuenta.RECUPERAR_CLAVE);
    assertEquals("VERIFICAR_EMAIL", EventoEnlaceDeCuenta.VERIFICAR_EMAIL);
  }

  @Test
  void exigeTodosLosCampos() {
    assertThrows(IllegalArgumentException.class, () -> evento(null, "http://kc"));
    assertThrows(
        IllegalArgumentException.class, () -> evento(EventoEnlaceDeCuenta.RECUPERAR_CLAVE, " "));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new EventoEnlaceDeCuenta(
                EventoEnlaceDeCuenta.RECUPERAR_CLAVE, "", "a@b.c", "l", 5, "es", FECHA));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new EventoEnlaceDeCuenta(
                EventoEnlaceDeCuenta.RECUPERAR_CLAVE, "c", null, "l", 5, "es", FECHA));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new EventoEnlaceDeCuenta(
                EventoEnlaceDeCuenta.RECUPERAR_CLAVE, "c", "a@b.c", "l", 5, "", FECHA));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new EventoEnlaceDeCuenta(
                EventoEnlaceDeCuenta.RECUPERAR_CLAVE, "c", "a@b.c", "l", 5, "es", null));
  }

  @Test
  void rechazaUnTipoDesconocidoYUnVencimientoQueNoEsPositivo() {
    assertThrows(IllegalArgumentException.class, () -> evento("OTRO_TIPO", "http://kc"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new EventoEnlaceDeCuenta(
                EventoEnlaceDeCuenta.RECUPERAR_CLAVE, "c", "a@b.c", "l", 0, "es", FECHA));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new EventoEnlaceDeCuenta(
                EventoEnlaceDeCuenta.RECUPERAR_CLAVE, "c", "a@b.c", "l", -1, "es", FECHA));
  }
}
