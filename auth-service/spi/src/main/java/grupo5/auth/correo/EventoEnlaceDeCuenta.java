package grupo5.auth.correo;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * El evento {@code cuenta.enlace-solicitado.v1}: una cuenta pidió un enlace de un solo uso
 * (recuperar la clave o verificar el email) y el correo con ese enlace lo manda {@code
 * notificaciones-service}.
 *
 * <p>Lleva el enlace y el destinatario, o sea un secreto y un dato personal: por eso no se guarda
 * en el historial de notificaciones ni se escribe en ningún log, y el mensaje vence cuando vence el
 * enlace.
 */
public record EventoEnlaceDeCuenta(
    String tipo,
    String cuentaId,
    String destinatario,
    String enlace,
    long venceEnMinutos,
    String idioma,
    Instant fecha) {

  public static final String RECUPERAR_CLAVE = "RECUPERAR_CLAVE";
  public static final String VERIFICAR_EMAIL = "VERIFICAR_EMAIL";

  /** Routing key y alias de tipo ({@code __TypeId__}) del evento. */
  public static final String ROUTING_KEY = "cuenta.enlace-solicitado.v1";

  public EventoEnlaceDeCuenta {
    exigir(tipo, "tipo");
    exigir(cuentaId, "cuentaId");
    exigir(destinatario, "destinatario");
    exigir(enlace, "enlace");
    exigir(idioma, "idioma");
    if (!RECUPERAR_CLAVE.equals(tipo) && !VERIFICAR_EMAIL.equals(tipo)) {
      throw new IllegalArgumentException("tipo desconocido: " + tipo);
    }
    if (venceEnMinutos <= 0) {
      throw new IllegalArgumentException("venceEnMinutos tiene que ser positivo");
    }
    if (fecha == null) {
      throw new IllegalArgumentException("falta fecha");
    }
  }

  private static void exigir(String valor, String campo) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException("falta " + campo);
    }
  }

  /**
   * El cuerpo del mensaje: JSON con los nombres de campo del contrato del evento. La fecha va sin
   * fracciones de segundo, como las de los demás eventos del sistema.
   */
  public String aJson() {
    return "{\"tipo\":"
        + texto(tipo)
        + ",\"cuentaId\":"
        + texto(cuentaId)
        + ",\"destinatario\":"
        + texto(destinatario)
        + ",\"enlace\":"
        + texto(enlace)
        + ",\"venceEnMinutos\":"
        + venceEnMinutos
        + ",\"idioma\":"
        + texto(idioma)
        + ",\"fecha\":"
        + texto(DateTimeFormatter.ISO_INSTANT.format(fecha.truncatedTo(ChronoUnit.SECONDS)))
        + "}";
  }

  static String texto(String valor) {
    StringBuilder json = new StringBuilder("\"");
    for (int i = 0; i < valor.length(); i++) {
      char c = valor.charAt(i);
      switch (c) {
        case '"' -> json.append("\\\"");
        case '\\' -> json.append("\\\\");
        case '\n' -> json.append("\\n");
        case '\r' -> json.append("\\r");
        case '\t' -> json.append("\\t");
        default -> {
          if (c < 0x20) {
            json.append(String.format("\\u%04x", (int) c));
          } else {
            json.append(c);
          }
        }
      }
    }
    return json.append('"').toString();
  }
}
