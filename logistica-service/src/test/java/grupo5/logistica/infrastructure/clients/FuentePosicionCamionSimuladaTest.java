package grupo5.logistica.infrastructure.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.testutils.EntregaMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FuentePosicionCamionSimuladaTest {

  private static final Instant INICIO = Instant.parse("2026-11-08T12:00:00Z");
  private static final long INTERVALO_MS = 30_000;

  private final CoordenadasGeograficas a = new CoordenadasGeograficas(-34.0, -58.0);
  private final CoordenadasGeograficas b = new CoordenadasGeograficas(-34.2, -58.2);
  private final CoordenadasGeograficas c = new CoordenadasGeograficas(-34.4, -58.0);

  private IEntregasRepository entregas;
  private Ruta ruta;

  @BeforeEach
  void setUp() {
    entregas = mock(IEntregasRepository.class);
    Entrega ea = registrar(EntregaMother.pendienteEn(a));
    Entrega sinCoordenadas = registrar(EntregaMother.pendiente());
    Entrega eb = registrar(EntregaMother.pendienteEn(b));
    Entrega ec = registrar(EntregaMother.pendienteEn(c));
    ruta =
        enTraslado(
            List.of(ea.getId(), sinCoordenadas.getId(), eb.getId(), ec.getId()),
            LocalDateTime.ofInstant(INICIO, ZoneOffset.UTC));
  }

  @Test
  void conUnPasoPorTramo_dosTicksAvanzanDosTramosEnElOrdenDeVisitaYSalteanLaParadaSinCoordenadas() {
    assertEquals(posicion(a, INICIO), fuenteA(INICIO, 1).posicionActual(ruta).orElseThrow());

    Instant tick1 = INICIO.plusMillis(INTERVALO_MS);
    assertEquals(posicion(b, tick1), fuenteA(tick1, 1).posicionActual(ruta).orElseThrow());

    Instant tick2 = INICIO.plusMillis(2 * INTERVALO_MS);
    assertEquals(posicion(c, tick2), fuenteA(tick2, 1).posicionActual(ruta).orElseThrow());
  }

  @Test
  void conVariosPasosPorTramo_interpolaEntreParadas() {
    Instant tick = INICIO.plusMillis(INTERVALO_MS + 5_000);

    PosicionCamion p = fuenteA(tick, 2).posicionActual(ruta).orElseThrow();

    assertEquals(-34.1, p.latitud(), 1e-9);
    assertEquals(-58.1, p.longitud(), 1e-9);
    assertEquals(INICIO.plusMillis(INTERVALO_MS), p.registradaEn());
  }

  @Test
  void pasadoElFinalDelRecorrido_quedaQuietoEnLaUltimaParadaConLaMismaHora() {
    Instant fin = INICIO.plusMillis(2 * INTERVALO_MS);

    PosicionCamion p = fuenteA(INICIO.plusSeconds(3_600), 1).posicionActual(ruta).orElseThrow();

    assertEquals(posicion(c, fin), p);
  }

  @Test
  void sinInicioRealOSinParadasConCoordenadas_noHayPosicion() {
    Ruta sinInicio = enTraslado(ruta.getEntregaIds(), null);
    Entrega sinCoordenadas = registrar(EntregaMother.pendiente());
    Ruta sinCoordenadasRuta =
        enTraslado(
            List.of(sinCoordenadas.getId()), LocalDateTime.ofInstant(INICIO, ZoneOffset.UTC));

    assertTrue(fuenteA(INICIO, 1).posicionActual(sinInicio).isEmpty());
    assertTrue(fuenteA(INICIO, 1).posicionActual(sinCoordenadasRuta).isEmpty());
  }

  @Test
  void unaRutaQueNoEstaEnTraslado_noMueveAlCamion() {
    LocalDateTime inicio = LocalDateTime.ofInstant(INICIO, ZoneOffset.UTC);
    Ruta completada = conEstado(EstadoRuta.COMPLETADA, ruta.getEntregaIds(), inicio);
    Ruta pendiente = conEstado(EstadoRuta.PENDIENTE, ruta.getEntregaIds(), inicio);

    assertTrue(fuenteA(INICIO.plusSeconds(60), 1).posicionActual(completada).isEmpty());
    assertTrue(fuenteA(INICIO.plusSeconds(60), 1).posicionActual(pendiente).isEmpty());
  }

  @Test
  void lasParadasAlcanzadasSonLasDelTramoEnCursoSinLasQueNoTienenCoordenadas() {
    List<UUID> ids = ruta.getEntregaIds();
    UUID ea = ids.get(0);
    UUID eb = ids.get(2);
    UUID ec = ids.get(3);

    assertEquals(List.of(ea), fuenteA(INICIO, 2).entregasAlcanzadas(ruta));
    // A mitad del primer tramo todavía no llegó a la segunda parada
    assertEquals(List.of(ea), fuenteA(INICIO.plusMillis(INTERVALO_MS), 2).entregasAlcanzadas(ruta));
    assertEquals(
        List.of(ea, eb), fuenteA(INICIO.plusMillis(2 * INTERVALO_MS), 2).entregasAlcanzadas(ruta));
    // Un tick perdido no deja una parada sin marcar: pasado el final, todas
    assertEquals(
        List.of(ea, eb, ec), fuenteA(INICIO.plusSeconds(3_600), 2).entregasAlcanzadas(ruta));
  }

  @Test
  void unaRutaQueNoEstaEnTraslado_noAlcanzaNingunaParada() {
    Ruta completada =
        conEstado(
            EstadoRuta.COMPLETADA,
            ruta.getEntregaIds(),
            LocalDateTime.ofInstant(INICIO, ZoneOffset.UTC));

    assertTrue(fuenteA(INICIO.plusSeconds(60), 1).entregasAlcanzadas(completada).isEmpty());
  }

  @Test
  void rechazaConfiguracionNoPositiva() {
    Clock clock = Clock.fixed(INICIO, ZoneOffset.UTC);
    assertThrows(
        IllegalArgumentException.class,
        () -> new FuentePosicionCamionSimulada(entregas, clock, 0, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FuentePosicionCamionSimulada(entregas, clock, INTERVALO_MS, 0));
  }

  private FuentePosicionCamionSimulada fuenteA(Instant ahora, int pasosPorTramo) {
    return new FuentePosicionCamionSimulada(
        entregas, Clock.fixed(ahora, ZoneOffset.UTC), INTERVALO_MS, pasosPorTramo);
  }

  private Entrega registrar(Entrega entrega) {
    when(entregas.findById(entrega.getId())).thenReturn(Optional.of(entrega));
    return entrega;
  }

  private static PosicionCamion posicion(CoordenadasGeograficas punto, Instant en) {
    return new PosicionCamion(punto.latitud(), punto.longitud(), en);
  }

  private static Ruta enTraslado(List<UUID> entregaIds, LocalDateTime inicio) {
    return conEstado(EstadoRuta.EN_TRASLADO, entregaIds, inicio);
  }

  private static Ruta conEstado(EstadoRuta estado, List<UUID> entregaIds, LocalDateTime inicio) {
    return new Ruta(
        UUID.randomUUID(),
        LocalDate.of(2026, 11, 8),
        entregaIds,
        UUID.randomUUID(),
        UUID.randomUUID(),
        estado,
        List.of(),
        inicio,
        null,
        0L);
  }
}
