package grupo5.cliente.puertos;

import java.util.UUID;

/**
 * El formulario de Contacto (HB-21). Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.contacto} elige cuál.
 */
public interface PublicoContactoPuerto {

  /**
   * La clave de envío que lleva el formulario al abrirse (reenviar devuelve el mismo número); null
   * si el envío no guarda (la maqueta).
   */
  UUID nuevaClave();

  /** El número que muestra la pantalla de enviado sin un envío real: el de ejemplo, o null. */
  Long numeroDeEjemplo();

  /**
   * Manda el mensaje y devuelve su número. {@code ipOrigen}, la del navegador, para el límite por
   * IP.
   *
   * @throws Rechazado si Notificaciones no lo acepta (datos que no validan o el límite).
   */
  long enviar(Mensaje mensaje, String ipOrigen);

  /**
   * {@code tema}, con el nombre del contrato (ENTREGA, DONACION, …); {@code codigoError}, o null.
   */
  record Mensaje(String tema, String texto, String codigoError, String correo, UUID claveEnvio) {}

  /** Por qué Notificaciones no aceptó el mensaje. */
  enum Motivo {
    INVALIDO,
    LIMITE
  }

  /** Un mensaje que Notificaciones no aceptó (400 o 429). */
  final class Rechazado extends RuntimeException {

    private final Motivo motivo;

    public Rechazado(Motivo motivo, Throwable causa) {
      super(motivo.name(), causa);
      this.motivo = motivo;
    }

    public Motivo motivo() {
      return motivo;
    }
  }
}
