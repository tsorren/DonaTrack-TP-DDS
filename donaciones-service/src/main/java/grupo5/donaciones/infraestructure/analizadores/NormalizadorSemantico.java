package grupo5.donaciones.infraestructure.analizadores;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component; // Importar la anotación Component

@Component("semanticTextNormalizer")
public class NormalizadorSemantico implements Normalizador {

  private final Normalizador basicNormalizer;
  private final Map<String, String> semanticMap;

  public NormalizadorSemantico() {
    this.basicNormalizer = new NormalizadorBasicoTexto();
    this.semanticMap = new HashMap<>();
    // Inicializa el mapa semántico con reglas básicas
    semanticMap.put("celu", "celular");
    semanticMap.put("movil", "celular");
    semanticMap.put("telefono", "celular");
    semanticMap.put("pc", "computadora");
    semanticMap.put("compu", "computadora");
    semanticMap.put("fideos", "pasta");
    semanticMap.put("arroz", "cereal");
    semanticMap.put("leche", "lacteo");
  }

  @Override
  public String normalizar(String texto) {
    if (texto == null) {
      return null;
    }

    String normalizedText = basicNormalizer.normalizar(texto);

    for (Map.Entry<String, String> entry : semanticMap.entrySet()) {

      normalizedText = normalizedText.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
    }

    return normalizedText;
  }
}
