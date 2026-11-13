package grupo5.logistica.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.repositories.CriterioSeguimiento;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.TramoDeEntregas;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * El mismo caso del seguimiento paginado (R11-43, C2) para los tres repositorios de entregas: en
 * memoria, JPA y Mongo resuelven igual el filtro, el orden, el tramo y el total.
 */
public final class SeguimientoDePrueba {

  private SeguimientoDePrueba() {}

  public static void verificar(IEntregasRepository repositorio) {
    UUID organizacion = UUID.randomUUID();
    Instant base = Instant.parse("2026-11-10T12:00:00Z");
    Entrega pendiente =
        EntregaMother.deLaOrganizacion(organizacion, EstadoEntrega.PENDIENTE, null, null);
    Entrega enCamino =
        EntregaMother.deLaOrganizacion(
            organizacion, EstadoEntrega.EN_TRASLADO, null, base.plusSeconds(3600));
    Entrega enPuerta =
        EntregaMother.deLaOrganizacion(organizacion, EstadoEntrega.EN_PUERTA, null, base);
    Entrega recibidaNueva =
        EntregaMother.deLaOrganizacion(
            organizacion, EstadoEntrega.ENTREGADA, base.plusSeconds(7200), base);
    Entrega recibidaVieja =
        EntregaMother.deLaOrganizacion(
            organizacion, EstadoEntrega.ENTREGADA, base.minusSeconds(86_400), base);
    Entrega deOtra =
        EntregaMother.deLaOrganizacion(UUID.randomUUID(), EstadoEntrega.PENDIENTE, null, null);
    List.of(recibidaVieja, enPuerta, deOtra, recibidaNueva, pendiente, enCamino)
        .forEach(repositorio::save);

    TramoDeEntregas todas =
        repositorio.buscarSeguimiento(
            new CriterioSeguimiento(organizacion, null, Set.of(), Set.of(), 0, 20));
    assertEquals(
        ids(pendiente, enCamino, enPuerta, recibidaNueva, recibidaVieja), ids(todas.entregas()));
    assertEquals(5, todas.total());

    TramoDeEntregas segunda =
        repositorio.buscarSeguimiento(
            new CriterioSeguimiento(organizacion, null, Set.of(), Set.of(), 1, 2));
    assertEquals(ids(enPuerta, recibidaNueva), ids(segunda.entregas()));
    assertEquals(5, segunda.total());

    TramoDeEntregas porEstado =
        repositorio.buscarSeguimiento(
            new CriterioSeguimiento(
                organizacion,
                null,
                Set.of(EstadoEntrega.EN_PUERTA, EstadoEntrega.EN_TRASLADO),
                Set.of(),
                0,
                20));
    assertEquals(ids(enCamino, enPuerta), ids(porEstado.entregas()));
    assertEquals(2, porEstado.total());

    TramoDeEntregas porDonacion =
        repositorio.buscarSeguimiento(
            new CriterioSeguimiento(
                organizacion,
                null,
                Set.of(),
                Set.of(recibidaVieja.getIdDonacion(), deOtra.getIdDonacion()),
                0,
                20));
    assertEquals(ids(recibidaVieja), ids(porDonacion.entregas()));
    assertEquals(1, porDonacion.total());

    assertEquals(
        Set.copyOf(ids(pendiente, enPuerta)),
        Set.copyOf(ids(repositorio.findByIds(Set.of(pendiente.getId(), enPuerta.getId())))));
  }

  private static List<UUID> ids(Entrega... entregas) {
    return ids(List.of(entregas));
  }

  private static List<UUID> ids(List<Entrega> entregas) {
    return entregas.stream().map(Entrega::getId).toList();
  }
}
