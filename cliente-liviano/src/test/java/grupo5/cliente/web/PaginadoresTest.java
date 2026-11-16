package grupo5.cliente.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.cliente.vistas.Paginador;
import grupo5.cliente.vistas.Tramo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** El paginador provisorio (R11-01): direcciones reales que conservan la búsqueda y los filtros. */
class PaginadoresTest {

  private static MockHttpServletRequest pedido(String consulta) {
    MockHttpServletRequest pedido = new MockHttpServletRequest("GET", "/admin-donantes");
    pedido.setQueryString(consulta);
    if (consulta != null) {
      for (String par : consulta.split("&")) {
        String[] partes = par.split("=", 2);
        pedido.addParameter(partes[0], partes.length > 1 ? partes[1] : "");
      }
    }
    return pedido;
  }

  @Test
  void conservaLosFiltrosCambiaLaPaginaYSacaLaEtapa() {
    Paginador p =
        Paginadores.de(
            pedido("buscar=ana&tipo=persona&pagina=2&etapa=importando"),
            "Páginas de donantes",
            new Tramo<>(List.of("x"), 60, 2, 3));

    assertEquals("Páginas de donantes", p.nombre());
    assertEquals("/admin-donantes?buscar=ana&tipo=persona&pagina=1", p.anterior());
    assertEquals("/admin-donantes?buscar=ana&tipo=persona&pagina=3", p.siguiente());
    assertEquals(true, p.cambio());
    // R11-11: primera, última y cada número también conservan la búsqueda y los filtros.
    assertEquals("/admin-donantes?buscar=ana&tipo=persona&pagina=1", p.primera());
    assertEquals("/admin-donantes?buscar=ana&tipo=persona&pagina=3", p.ultima());
    assertEquals("/admin-donantes?buscar=ana&tipo=persona&pagina=1", p.tramos().get(0).direccion());
    assertNull(p.tramos().get(1).direccion());
  }

  @Test
  void enLosExtremosNoHayEnlaceYConUnaPaginaNoHayPaginador() {
    Paginador primera =
        Paginadores.de(pedido(null), "Páginas", new Tramo<>(List.of("x"), 60, 1, 3));
    Paginador ultima =
        Paginadores.de(pedido("pagina=3"), "Páginas", new Tramo<>(List.of("x"), 60, 3, 3));

    assertNull(primera.anterior());
    assertEquals("/admin-donantes?pagina=2", primera.siguiente());
    assertEquals(false, primera.cambio());
    assertNull(ultima.siguiente());
    assertNull(Paginadores.de(pedido(null), "Páginas", new Tramo<>(List.of("x"), 1, 1, 1)));
    assertNull(Paginadores.de(pedido(null), "Páginas", null));
  }

  @Test
  void unaPaginaQueNoEsUnNumeroEsLaPrimera() {
    assertEquals(1, Paginadores.pedida(pedido("pagina=abc")));
    assertEquals(1, Paginadores.pedida(pedido("pagina=0")));
    assertEquals(4, Paginadores.pedida(pedido("pagina=4")));
  }
}
