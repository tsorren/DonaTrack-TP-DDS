package grupo5.auth.correo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.keycloak.email.EmailException;
import org.keycloak.models.UserModel;

class EmisorDeEnlacesDeCuentaTest {

  private final List<EventoEnlaceDeCuenta> publicados = new ArrayList<>();
  private final List<String> trazas = new ArrayList<>();
  private final EmisorDeEnlacesDeCuenta emisor =
      new EmisorDeEnlacesDeCuenta(
          (evento, traceId) -> {
            publicados.add(evento);
            trazas.add(traceId);
          });

  /** Un UserModel con solo lo que el emisor lee: el id y el email. */
  private static UserModel usuario(String id, String email) {
    return (UserModel)
        Proxy.newProxyInstance(
            UserModel.class.getClassLoader(),
            new Class<?>[] {UserModel.class},
            (proxy, metodo, args) ->
                switch (metodo.getName()) {
                  case "getId" -> id;
                  case "getEmail" -> email;
                  case "hashCode" -> 1;
                  case "equals" -> proxy == args[0];
                  default -> null;
                });
  }

  private static EmisorDeEnlacesDeCuenta emisorQueFalla(Exception causa) {
    return new EmisorDeEnlacesDeCuenta(
        (evento, traceId) -> {
          if (causa instanceof PublicacionFallida fallida) {
            throw fallida;
          }
          throw new IllegalStateException(causa);
        });
  }

  @Test
  void publicaElEventoConLosDatosDeLaCuenta() throws Exception {
    emisor.emitir(
        EventoEnlaceDeCuenta.RECUPERAR_CLAVE,
        usuario("cuenta-1", "ana@example.com"),
        "es",
        "http://kc/enlace?key=1&a=2",
        30);

    assertEquals(1, publicados.size());
    EventoEnlaceDeCuenta evento = publicados.get(0);
    assertEquals(EventoEnlaceDeCuenta.RECUPERAR_CLAVE, evento.tipo());
    assertEquals("cuenta-1", evento.cuentaId());
    assertEquals("ana@example.com", evento.destinatario());
    assertEquals("http://kc/enlace?key=1&a=2", evento.enlace());
    assertEquals(30, evento.venceEnMinutos());
    assertEquals("es", evento.idioma());
  }

  @Test
  void elMismoCaminoSirveParaVerificarElEmail() throws Exception {
    emisor.emitir(
        EventoEnlaceDeCuenta.VERIFICAR_EMAIL,
        usuario("c", "ana@example.com"),
        "es",
        "http://kc/v",
        60);

    assertEquals(EventoEnlaceDeCuenta.VERIFICAR_EMAIL, publicados.get(0).tipo());
    assertEquals(60, publicados.get(0).venceEnMinutos());
  }

  @Test
  void elTraceIdTieneElFormatoDelSistemaYCambiaEnCadaEvento() throws Exception {
    emisor.emitir(EventoEnlaceDeCuenta.RECUPERAR_CLAVE, usuario("c", "a@b.c"), "es", "l", 5);
    emisor.emitir(EventoEnlaceDeCuenta.RECUPERAR_CLAVE, usuario("c", "a@b.c"), "es", "l", 5);

    assertTrue(trazas.get(0).matches("[0-9a-f]{32}"), trazas.get(0));
    assertNotEquals(trazas.get(0), trazas.get(1));
  }

  @Test
  void sinEmailNoPublicaNada() {
    for (String email : new String[] {null, "", "   "}) {
      EmailException error =
          assertThrows(
              EmailException.class,
              () ->
                  emisor.emitir(
                      EventoEnlaceDeCuenta.RECUPERAR_CLAVE, usuario("c", email), "es", "l", 5));

      // El aviso es el de la cuenta sin email, no el de un evento mal armado.
      assertTrue(error.getMessage().contains("no tiene un email"), error.getMessage());
    }
    assertEquals(0, publicados.size());
  }

  @Test
  void sinUsuarioNoPublicaNada() {
    assertThrows(
        EmailException.class,
        () -> emisor.emitir(EventoEnlaceDeCuenta.RECUPERAR_CLAVE, null, "es", "l", 5));
    assertEquals(0, publicados.size());
  }

  @Test
  void siElBrokerFallaSeTraduceAlErrorQueKeycloakEspera() {
    EmisorDeEnlacesDeCuenta caido = emisorQueFalla(new PublicacionFallida("broker caído"));

    EmailException error =
        assertThrows(
            EmailException.class,
            () ->
                caido.emitir(
                    EventoEnlaceDeCuenta.RECUPERAR_CLAVE, usuario("c", "a@b.c"), "es", "l", 5));

    assertInstanceOf(PublicacionFallida.class, error.getCause());
  }

  @Test
  void unVencimientoInvalidoTambienEsUnEmailException() {
    assertThrows(
        EmailException.class,
        () ->
            emisor.emitir(
                EventoEnlaceDeCuenta.RECUPERAR_CLAVE, usuario("c", "a@b.c"), "es", "l", 0));
    assertEquals(0, publicados.size());
  }
}
