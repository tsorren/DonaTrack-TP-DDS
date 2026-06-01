package grupo5.donaciones.models.privacidad;

public interface Anonimizable {
  Integer valorNumerico = 0;
  String valorString = "ANONIMIZADO";

  void anonimizar();
}
