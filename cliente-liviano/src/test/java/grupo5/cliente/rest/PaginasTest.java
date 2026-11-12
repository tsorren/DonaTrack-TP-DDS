package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.cliente.vistas.Tramo;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** Pedir una página de un listado (R11-01): el total de la cabecera y la página que existe. */
class PaginasTest {

  private static ResponseEntity<List<String>> pagina(List<String> filas, String total) {
    return total == null
        ? ResponseEntity.ok(filas)
        : ResponseEntity.ok().header(Paginas.TOTAL, total).body(filas);
  }

  @Test
  void pideLaPaginaDesdeCeroYLeeElTotalDeLaCabecera() {
    List<Integer> pedidas = new ArrayList<>();
    Tramo<String> tramo =
        Paginas.pedir(
            2,
            20,
            n -> {
              pedidas.add(n);
              return pagina(List.of("a", "b"), "22");
            });

    assertEquals(List.of(1), pedidas);
    assertEquals(new Tramo<>(List.of("a", "b"), 22, 2, 2), tramo);
  }

  @Test
  void unaPaginaQueYaNoExisteTraeLaUltima() {
    List<Integer> pedidas = new ArrayList<>();
    Tramo<String> tramo =
        Paginas.pedir(
            50,
            20,
            n -> {
              pedidas.add(n);
              return n == 49 ? pagina(List.of(), "41") : pagina(List.of("z"), "41");
            });

    assertEquals(List.of(49, 2), pedidas);
    assertEquals(new Tramo<>(List.of("z"), 41, 3, 3), tramo);
  }

  @Test
  void sinFilasEsUnaSolaPaginaVaciaYSinCabeceraCuentaLoQueVe() {
    assertEquals(
        new Tramo<>(List.of(), 0, 1, 1), Paginas.pedir(1, 20, n -> pagina(List.of(), "0")));
    assertEquals(
        new Tramo<>(List.of("a"), 21, 2, 2), Paginas.pedir(2, 20, n -> pagina(List.of("a"), null)));
    assertEquals(
        new Tramo<>(List.of("a"), 1, 1, 1),
        Paginas.pedir(1, 20, n -> pagina(List.of("a"), "no es un número")));
  }
}
