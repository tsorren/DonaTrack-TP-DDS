package grupo5.notificaciones.models.entities.contacto;

import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.ports.NotificacionSender;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;

/**
 * Un mensaje del formulario de contacto (HB-21, ADR proposed 20261110). El {@code numero} lo pone
 * el repositorio al guardarlo: es el que ve quien escribe y el que cita la respuesta.
 */
@Getter
public class MensajeContacto {
  private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Argentina/Buenos_Aires");

  private Long numero;
  private final UUID claveEnvio;
  private final TemaContacto tema;
  private final String texto;
  private final String codigoError;
  private final String correo;
  private final String ipOrigen;
  private final LocalDateTime recibidoEn;
  private EstadoMensajeContacto estado;
  private String respuesta;
  private LocalDateTime respondidoEn;

  @SuppressWarnings("java:S107") // constructor de persistencia: un parámetro por columna
  public MensajeContacto(
      Long numero,
      UUID claveEnvio,
      TemaContacto tema,
      String texto,
      String codigoError,
      String correo,
      String ipOrigen,
      LocalDateTime recibidoEn,
      EstadoMensajeContacto estado,
      String respuesta,
      LocalDateTime respondidoEn) {
    this.numero = numero;
    this.claveEnvio = claveEnvio;
    this.tema = tema;
    this.texto = texto;
    this.codigoError = codigoError;
    this.correo = correo;
    this.ipOrigen = ipOrigen;
    this.recibidoEn = recibidoEn;
    this.estado = estado;
    this.respuesta = respuesta;
    this.respondidoEn = respondidoEn;
  }

  /** Un mensaje recién llegado, todavía sin número. */
  public static MensajeContacto recibido(
      UUID claveEnvio,
      TemaContacto tema,
      String texto,
      String codigoError,
      String correo,
      String ipOrigen,
      LocalDateTime recibidoEn) {
    return new MensajeContacto(
        null,
        claveEnvio,
        tema,
        texto,
        codigoError,
        correo,
        ipOrigen,
        recibidoEn,
        EstadoMensajeContacto.RECIBIDO,
        null,
        null);
  }

  /** Lo usa el repositorio en memoria; el de JPA lo toma de la identidad de la tabla. */
  public void asignarNumero(long numero) {
    if (this.numero != null) {
      throw new IllegalStateException("El mensaje ya tiene número");
    }
    this.numero = numero;
  }

  /**
   * Manda la respuesta al correo del mensaje. Si el proveedor la rechaza o falla, el mensaje queda
   * en ENVIO_FALLIDO con la respuesta guardada, para reintentar.
   */
  public void responder(String textoRespuesta, NotificacionSender sender) {
    this.respuesta = textoRespuesta;
    Correo destino = new Correo();
    destino.setDireccionCorreo(this.correo);
    boolean enviado;
    try {
      enviado =
          sender.enviarA(destino, "Respuesta a tu mensaje Nº " + numero + "\n\n" + textoRespuesta);
    } catch (RuntimeException e) {
      enviado = false;
    }
    if (enviado) {
      this.estado = EstadoMensajeContacto.RESPONDIDO;
      this.respondidoEn = LocalDateTime.now(ZONA_HORARIA);
    } else {
      this.estado = EstadoMensajeContacto.ENVIO_FALLIDO;
    }
  }
}
