package grupo5.donaciones.models.entities.donaciones;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.models.entities.donaciones.events.DonacionCargada;
import grupo5.donaciones.models.entities.donaciones.events.DonacionSegmentada;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class EventosDominioSinConsumidorTest {

  @Test
  void donacion_alCrearse_registraDonacionCargada() {
    Donacion donacion = DonacionMother.simple(UUID.randomUUID());

    assertTrue(
        donacion.getDomainEvents().stream().anyMatch(e -> e instanceof DonacionCargada),
        "Se debe registrar DonacionCargada al crear la donación");
  }

  @Test
  void donacion_alCrearse_donacionCargadaContieneIdsCorrectos() {
    UUID donanteId = UUID.randomUUID();
    Donacion donacion = DonacionMother.simple(donanteId);

    DonacionCargada evento =
        donacion.getDomainEvents().stream()
            .filter(e -> e instanceof DonacionCargada)
            .map(e -> (DonacionCargada) e)
            .findFirst()
            .orElseThrow();

    assertEquals(donacion.getId(), evento.donacionId());
    assertEquals(donanteId, evento.donanteId());
  }

  @Test
  void donacion_alSegmentarse_registraDonacionSegmentada() {
    Donacion donacion =
        DonacionMother.normalizada(UUID.randomUUID(), BienMother.simple("Abrigo"), 5);
    donacion.clearDomainEvents();

    donacion.marcarSegmentada();

    assertTrue(
        donacion.getDomainEvents().stream().anyMatch(e -> e instanceof DonacionSegmentada),
        "Se debe registrar DonacionSegmentada al marcar la donación como segmentada");
  }

  @Test
  void donacion_alSegmentarse_donacionSegmentadaContieneIdsCorrectos() {
    UUID donanteId = UUID.randomUUID();
    Donacion donacion = DonacionMother.normalizada(donanteId, BienMother.simple("Silla"), 3);
    donacion.clearDomainEvents();

    donacion.marcarSegmentada();

    DonacionSegmentada evento =
        donacion.getDomainEvents().stream()
            .filter(e -> e instanceof DonacionSegmentada)
            .map(e -> (DonacionSegmentada) e)
            .findFirst()
            .orElseThrow();

    assertEquals(donacion.getId(), evento.donacionId());
    assertEquals(donanteId, evento.donanteId());
  }

  @Test
  void donacionCargada_alPublicarse_noLanzaExcepcionSinConsumidor() {
    Donacion donacion = DonacionMother.simple(UUID.randomUUID());
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

    assertDoesNotThrow(() -> donacion.getDomainEvents().forEach(publisher::publishEvent));

    verify(publisher, atLeastOnce()).publishEvent(any(DonacionCargada.class));
  }

  @Test
  void donacionSegmentada_alPublicarse_noLanzaExcepcionSinConsumidor() {
    Donacion donacion =
        DonacionMother.normalizada(UUID.randomUUID(), BienMother.simple("Silla"), 3);
    donacion.clearDomainEvents();
    donacion.marcarSegmentada();
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

    assertDoesNotThrow(() -> donacion.getDomainEvents().forEach(publisher::publishEvent));

    verify(publisher, atLeastOnce()).publishEvent(any(DonacionSegmentada.class));
  }
}
