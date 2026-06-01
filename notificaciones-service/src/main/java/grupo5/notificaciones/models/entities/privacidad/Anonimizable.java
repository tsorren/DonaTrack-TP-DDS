package grupo5.notificaciones.models.entities.privacidad;

public interface Anonimizable {
  Integer VALOR_NUMERICO = 0;
  String VALOR_STRING = "ANONIMIZADO";

  void anonimizar();
}
