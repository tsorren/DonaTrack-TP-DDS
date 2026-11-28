package grupo5.donaciones.graphql.relaciones;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** El armado del resultado de cada carga por lotes (SPEC-11, G2, lote {@code g2-relaciones}). */
class CargaPorIdTest {

  private record Padre(String nombre, UUID hijoId) {}

  private record Hijo(UUID id, String nombre) {}

  private final List<Collection<UUID>> lecturas = new ArrayList<>();

  private Function<Collection<UUID>, List<Hijo>> leerDe(Hijo... hijos) {
    return ids -> {
      lecturas.add(List.copyOf(ids));
      return List.of(hijos);
    };
  }

  @Test
  void unaSolaLecturaConLosIdsSinNulosNiRepetidos_yCadaPadreConSuHijo() {
    UUID uno = UUID.randomUUID();
    UUID dos = UUID.randomUUID();
    UUID perdido = UUID.randomUUID();
    Padre a = new Padre("a", uno);
    Padre b = new Padre("b", dos);
    Padre c = new Padre("c", uno);
    Padre sinHijo = new Padre("sin hijo", null);
    Padre conHijoPerdido = new Padre("perdido", perdido);
    Hijo hijoUno = new Hijo(uno, "uno");
    Hijo hijoDos = new Hijo(dos, "dos");

    Map<Padre, String> resultado =
        CargaPorId.unir(
            List.of(a, b, c, sinHijo, conHijoPerdido),
            Padre::hijoId,
            leerDe(hijoUno, hijoDos),
            Hijo::id,
            Hijo::nombre);

    assertThat(lecturas).hasSize(1);
    assertThat(lecturas.get(0)).containsExactly(uno, dos, perdido);
    assertThat(resultado)
        .containsOnly(Map.entry(a, "uno"), Map.entry(b, "dos"), Map.entry(c, "uno"));
  }

  @Test
  void sinIds_noLee() {
    Map<Padre, String> resultado =
        CargaPorId.unir(
            List.of(new Padre("a", null), new Padre("b", null)),
            Padre::hijoId,
            leerDe(),
            Hijo::id,
            Hijo::nombre);

    assertThat(resultado).isEmpty();
    assertThat(lecturas).isEmpty();
  }
}
