package grupo5.notificaciones.models.ports;

public interface Anonimizable {
  Integer VALOR_NUMERICO = 0;
  String VALOR_STRING = "ANONIMIZADO";

  void anonimizar();
}
