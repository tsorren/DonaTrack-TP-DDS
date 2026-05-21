package ar.edu.utn.frba.ddsi.notificaciones.infrastructure;

public interface CorreoAdapter {
  public boolean enviarMail(String correo, String mensaje);
}
