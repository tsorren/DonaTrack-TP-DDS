package grupo5.donaciones.models.normalizacion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ComparadorTexto {

  private final Normalizador normalizador;

  public ComparadorTexto(Normalizador normalizador) {
    this.normalizador = normalizador;
  }

  private static String[] listaDePalabras(String texto) {
    // Divide el texto por uno o más espacios en blanco
    return texto.split("\\s+");
  }

  private Set<String> textoASetUnico(String texto) {
    String normalizado = normalizador.normalizar(texto); // Usa la instancia inyectada
    if (normalizado == null || normalizado.isEmpty()) {
      return new HashSet<>();
    }
    String[] lista = listaDePalabras(normalizado);
    return new HashSet<>(Arrays.asList(lista));
  }

  public Integer contarPalabrasEnComun(String textoA, String textoB) {
    if (textoA == null || textoB == null) {
      return 0;
    }

    Set<String> palabrasTextoA = textoASetUnico(textoA);
    Set<String> palabrasTextoB = textoASetUnico(textoB);

    // Para contar palabras en común, podemos usar la intersección de los sets
    Set<String> interseccion = new HashSet<>(palabrasTextoA);
    interseccion.retainAll(
        palabrasTextoB); // Retiene solo los elementos que también están en palabrasTextoB

    return interseccion.size();
  }
}
