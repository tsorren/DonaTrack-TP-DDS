package grupo5.cliente.puertos;

/** Un mensaje de Contacto que Notificaciones no aceptó (400 o 429). */
public final class ContactoRechazado extends RuntimeException {

  private final PublicoContactoPuerto.Motivo motivo;

  public ContactoRechazado(PublicoContactoPuerto.Motivo motivo, Throwable causa) {
    super(motivo.name(), causa);
    this.motivo = motivo;
  }

  public PublicoContactoPuerto.Motivo motivo() {
    return motivo;
  }
}
