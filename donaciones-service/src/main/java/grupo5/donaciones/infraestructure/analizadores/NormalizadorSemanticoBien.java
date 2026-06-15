package grupo5.donaciones.infraestructure.analizadores;

import grupo5.donaciones.models.entities.categorias.AliasSubcategoria;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NormalizadorSemanticoBien implements Normalizador {

  private final Normalizador basicNormalizer;
  private final Map<String, String> semanticGoodsMap;

  public NormalizadorSemanticoBien(List<AliasSubcategoria> aliases) {
    this.basicNormalizer = new NormalizadorBasicoTexto();
    this.semanticGoodsMap = new HashMap<>();
    // Construye el mapa semántico a partir de los alias definidos en el dominio
    for (AliasSubcategoria aliasSubcategoria : aliases) {
      semanticGoodsMap.put(aliasSubcategoria.getAlias(), aliasSubcategoria.getSubcategoria().getNombre());
    }
  }

  @Override
  public String normalizar(String goodName) {
    if (goodName == null) {
      return null;
    }

    String normalizedGoodName = basicNormalizer.normalizar(goodName);

    for (Map.Entry<String, String> entry : semanticGoodsMap.entrySet()) {
      // Usamos replaceAll con límites de palabra para evitar reemplazar partes de palabras
      normalizedGoodName =
          normalizedGoodName.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
    }

    return normalizedGoodName;
  }
}
