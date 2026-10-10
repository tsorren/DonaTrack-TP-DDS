package grupo5.incentivos.infrastructure.adapters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class N8nClientAdapterTest {

  private N8nClientAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter =
        new N8nClientAdapter(
            "http://localhost:5678/webhook/badge", "http://localhost:5678/webhook/ranking");
  }

  @Test
  void publicarInsigniaGanada_noDeberiaLanzarExcepcionSincronica() {
    assertDoesNotThrow(
        () ->
            adapter.publicarInsigniaGanada(
                UUID.randomUUID(), "Donante Juan", "Insignia Oro", "Por donar"));
  }

  @Test
  void notificarRankingCalculado_noDeberiaLanzarExcepcionSincronica() {
    assertDoesNotThrow(
        () ->
            adapter.notificarRankingCalculado(
                "2026-05", List.of(Map.of("donanteId", UUID.randomUUID(), "posicion", 1))));
  }
}
