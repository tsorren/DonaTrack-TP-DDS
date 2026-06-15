package grupo5.donaciones.infraestructure.analizadores;

import java.util.HashMap;
import java.util.Map;

public class NormalizadorSemanticoBien implements Normalizador {

  private final Normalizador basicNormalizer;
  private final Map<String, String> semanticGoodsMap;

  public NormalizadorSemanticoBien() {
    this.basicNormalizer = new NormalizadorBasicoTexto();
    this.semanticGoodsMap = new HashMap<>();
    // Mapeo de bienes específicos a nombres estandarizados o categorías
    semanticGoodsMap.put("fideos tirabuzon", "pasta");
    semanticGoodsMap.put("spaghetti", "pasta");
    semanticGoodsMap.put("arroz blanco", "arroz");
    semanticGoodsMap.put("arroz integral", "arroz");
    semanticGoodsMap.put("leche entera", "leche");
    semanticGoodsMap.put("leche descremada", "leche");
    semanticGoodsMap.put("notebook", "computadora portatil");
    semanticGoodsMap.put("laptop", "computadora portatil");
    semanticGoodsMap.put(
        "silla de ruedas", "silla de ruedas"); // Ejemplo de un bien que ya es estándar
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
