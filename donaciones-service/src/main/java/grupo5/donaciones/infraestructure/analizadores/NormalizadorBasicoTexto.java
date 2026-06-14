package grupo5.donaciones.infraestructure.analizadores;

import java.text.Normalizer;

public class NormalizadorBasicoTexto implements Normalizador {
  @Override
  public String normalizar(String texto) {
    if (texto == null) {
      return null;
    }

    String result = texto.trim().toLowerCase();

    // separa letras base de acentos
    result = Normalizer.normalize(result, Normalizer.Form.NFD);

    // elimina acentos
    result = result.replaceAll("\\p{M}", "");

    // limpia símbolos pero conserva letras y números
    result = result.replaceAll("[^a-z0-9\\s]", "");

    // limpia espacios duplicados
    result = result.replaceAll("\\s+", " ").trim();

    return result;
  }
}
