package grupo5.tests.utils;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/** Agrega el token de pruebas a cada pedido de RestAssured que no traiga su Authorization. */
public final class FiltroDeToken implements Filter {

  private final TokenDePruebas token;

  public FiltroDeToken(TokenDePruebas token) {
    this.token = token;
  }

  @Override
  public Response filter(
      FilterableRequestSpecification pedido,
      FilterableResponseSpecification respuesta,
      FilterContext contexto) {
    if (!pedido.getHeaders().hasHeaderWithName("Authorization")) {
      pedido.header("Authorization", "Bearer " + token.token());
    }
    return contexto.next(pedido, respuesta);
  }
}
