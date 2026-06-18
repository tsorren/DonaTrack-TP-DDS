package grupo5.donaciones.infrastructure.analizadores;

import grupo5.donaciones.models.entities.categorias.AliasSubcategoria;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NormalizadorSemantico implements Normalizador {

  private final Normalizador basicNormalizer;
  private final Map<String, String> semanticMap;

  public NormalizadorSemantico(List<AliasSubcategoria> aliases) {
    this.basicNormalizer = new NormalizadorBasicoTexto();
    this.semanticMap = new HashMap<>();
    // Construye el mapa semántico a partir de los alias definidos en el dominio
    for (AliasSubcategoria aliasSubcategoria : aliases) {
      semanticMap.put(
          aliasSubcategoria.getAlias(), aliasSubcategoria.getSubcategoria().getNombre());
    }
  }

  @Override
  public String normalizar(String texto) {
    if (texto == null) {
      return null;
    }

    String normalizedText = basicNormalizer.normalizar(texto);

    for (Map.Entry<String, String> entry : semanticMap.entrySet()) {
      // Reemplaza el alias por el nombre canónico de la subcategoría, respetando límites de palabra
      normalizedText = normalizedText.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
    }

    return normalizedText;
  }
}
