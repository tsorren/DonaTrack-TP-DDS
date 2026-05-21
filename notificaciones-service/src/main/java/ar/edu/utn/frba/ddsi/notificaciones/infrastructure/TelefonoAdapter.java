package ar.edu.utn.frba.ddsi.notificaciones.infrastructure;

public interface TelefonoAdapter {
  public boolean enviarSms(String telefono, String mensaje);
}
