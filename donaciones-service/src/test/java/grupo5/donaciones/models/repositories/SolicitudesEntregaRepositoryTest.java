package grupo5.donaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.impl.SolicitudesEntregaRepositoryEnMemoria;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitudesEntregaRepositoryTest {

  private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 7, 12, 0);

  private final SolicitudesEntregaRepositoryEnMemoria repositorio =
      new SolicitudesEntregaRepositoryEnMemoria();

  @Test
  void findActivaPorDonacion_noDeberiaDevolverUnaFallida() {
    UUID donacionId = UUID.randomUUID();
    SolicitudEntrega fallida = new SolicitudEntrega(donacionId, AHORA);
    fallida.marcarFallida();
    repositorio.save(fallida);

    assertTrue(repositorio.findActivaPorDonacion(donacionId).isEmpty());
  }

  @Test
  void findMasRecientePorDonacion_deberiaDevolverLaUltimaAunqueEsteFallida() {
    UUID donacionId = UUID.randomUUID();
    SolicitudEntrega vieja = new SolicitudEntrega(donacionId, AHORA);
    vieja.marcarFallida();
    SolicitudEntrega nueva = new SolicitudEntrega(donacionId, AHORA.plusMinutes(5));
    nueva.marcarFallida();
    repositorio.save(vieja);
    repositorio.save(nueva);
    repositorio.save(new SolicitudEntrega(UUID.randomUUID(), AHORA.plusHours(1)));

    assertEquals(
        nueva.getId(), repositorio.findMasRecientePorDonacion(donacionId).orElseThrow().getId());
  }

  @Test
  void findMasRecientePorDonacion_deberiaEstarVacio_CuandoLaDonacionNoTieneSolicitudes() {
    assertTrue(repositorio.findMasRecientePorDonacion(UUID.randomUUID()).isEmpty());
  }
}
