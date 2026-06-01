package grupo5.notificaciones.models.entities.persona;

public interface Anonimizable {
  Integer valorNumerico = 0;
  String valorString = "ANONIMIZADO";

  void anonimizar();
}
