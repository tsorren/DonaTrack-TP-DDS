package grupo5.donaciones.models.privacidad;

public interface Anonimizable {
  Integer VALOR_NUMERICO = 0;
  String VALOR_STRING = "ANONIMIZADO";

  void anonimizar();
}
