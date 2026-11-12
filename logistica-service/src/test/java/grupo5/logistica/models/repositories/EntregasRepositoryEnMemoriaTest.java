package grupo5.logistica.models.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.repositories.impl.EntregasRepository;
import grupo5.logistica.testutils.EntregaMother;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntregasRepositoryEnMemoriaTest {

  @Test
  void findByDonanteId_devuelveSoloLasEntregasDelDonanteEnOrdenDeAlta() {
    EntregasRepository repositorio = new EntregasRepository();
    UUID donanteId = UUID.randomUUID();
    Entrega primera = EntregaMother.pendiente();
    primera.registrarDonante(donanteId);
    Entrega deOtro = EntregaMother.pendiente();
    deOtro.registrarDonante(UUID.randomUUID());
    Entrega segunda = EntregaMother.pendiente();
    segunda.registrarDonante(donanteId);
    repositorio.save(primera);
    repositorio.save(deOtro);
    repositorio.save(EntregaMother.pendiente());
    repositorio.save(segunda);

    assertEquals(
        List.of(primera.getId(), segunda.getId()),
        repositorio.findByDonanteId(donanteId).stream().map(Entrega::getId).toList());
    assertTrue(repositorio.findByDonanteId(null).isEmpty());
  }

  private static final CoordenadasGeograficas PUNTO = new CoordenadasGeograficas(-34.6, -58.4);
  private static final Instant DESDE = Instant.parse("2026-10-13T03:00:00Z");
  private static final Instant HASTA = Instant.parse("2026-11-13T03:00:00Z");

  @Test
  void
      findEntregadasParaMapa_devuelveLasDelPeriodoConCoordenadasDeLaMasRecienteALaMasViejaYConTope() {
    EntregasRepository repositorio = new EntregasRepository();
    Entrega vieja = EntregaMother.entregadaEl(DESDE, PUNTO);
    Entrega media = EntregaMother.entregadaEl(Instant.parse("2026-11-01T15:00:00Z"), PUNTO);
    Entrega nueva = EntregaMother.entregadaEl(Instant.parse("2026-11-12T15:00:00Z"), PUNTO);
    repositorio.save(vieja);
    repositorio.save(nueva);
    repositorio.save(media);
    repositorio.save(EntregaMother.entregadaEl(HASTA, PUNTO));
    repositorio.save(EntregaMother.entregadaEl(DESDE.minusSeconds(1), PUNTO));
    repositorio.save(EntregaMother.entregadaEl(Instant.parse("2026-11-02T15:00:00Z"), null));
    repositorio.save(EntregaMother.entregadaEl(null, PUNTO));
    repositorio.save(
        EntregaMother.enEstadoEl(
            EstadoEntrega.NO_RECIBIDA, Instant.parse("2026-11-03T15:00:00Z"), PUNTO));

    assertEquals(
        List.of(nueva.getId(), media.getId(), vieja.getId()),
        repositorio.findEntregadasParaMapa(DESDE, HASTA, 10).stream().map(Entrega::getId).toList());
    assertEquals(
        List.of(nueva.getId(), media.getId()),
        repositorio.findEntregadasParaMapa(DESDE, HASTA, 2).stream().map(Entrega::getId).toList());
    assertEquals(3, repositorio.contarEntregadasParaMapa(DESDE, HASTA));
    assertTrue(repositorio.findEntregadasParaMapa(null, HASTA, 10).isEmpty());
  }
}
