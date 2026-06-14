package grupo5.donaciones.infraestructure.analizadores;

public class NormalizadorBasicoTexto implements Normalizador {
  @Override
  public String normalizar(String texto) {
    if (texto == null) {
      return null;
    }
    return texto.trim().toLowerCase().replaceAll("[^a-z0-9\\s]", "");
  }
}
