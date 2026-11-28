package grupo5.donaciones.graphql.comun;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El tope de tamaño de las consultas raíz de GraphQL, leído de {@code
 * donatrack.graphql.pagina.tamanio-maximo} (contrato de GraphQL, §4.4). Una página negativa o un
 * tamaño fuera de 1 al tope da {@code ERR-CSR-003}.
 */
@Component
public class TopeDePagina {

  /** El mismo valor por defecto que declara el esquema para {@code tamanio}. */
  public static final int TAMANIO_POR_DEFECTO = 20;

  private final int tamanioMaximo;

  public TopeDePagina(@Value("${donatrack.graphql.pagina.tamanio-maximo}") int tamanioMaximo) {
    if (tamanioMaximo < 1) {
      throw new IllegalArgumentException("El tope de página de GraphQL tiene que ser al menos 1");
    }
    this.tamanioMaximo = tamanioMaximo;
  }

  /** Valida los argumentos de una raíz; un argumento nulo toma el valor por defecto del esquema. */
  public PedidoDePagina validar(Integer pagina, Integer tamanio) {
    int numero = pagina == null ? 0 : pagina;
    int cantidad = tamanio == null ? TAMANIO_POR_DEFECTO : tamanio;
    if (numero < 0 || cantidad < 1 || cantidad > tamanioMaximo) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    return new PedidoDePagina(numero, cantidad);
  }

  public int tamanioMaximo() {
    return tamanioMaximo;
  }
}
