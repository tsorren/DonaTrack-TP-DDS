package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.donaciones.outputs.EvolucionAdminDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * R11-42: los gráficos del inicio del admin, con el reloj fijo el 15 de octubre de 2026. Todo sale
 * de conteos agrupados; ninguna lista de donaciones se recorre.
 */
class EvolucionAdminServiceTest {

  private static final Instant AHORA = Instant.parse("2026-10-15T18:30:00Z");
  private static final LocalDateTime INICIO_12 = LocalDateTime.of(2025, 11, 1, 0, 0);
  private static final LocalDateTime FIN = LocalDateTime.of(2026, 11, 1, 0, 0);
  private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

  private final IDonacionesIndependientesRepository independientes =
      mock(IDonacionesIndependientesRepository.class);
  private final ICategoriasRepository categorias = mock(ICategoriasRepository.class);

  private final EvolucionAdminService service =
      new EvolucionAdminService(independientes, categorias, Clock.fixed(AHORA, ZoneOffset.UTC));

  private static Categoria categoria(String nombre) {
    return new Categoria(UUID.randomUUID(), nombre, false, false, Unidad.UNIDADES);
  }

  @Test
  void ventanaCompleta_docesMesesTodosPresentes_conCeroDondeNoHay() {
    when(independientes.primeraFechaRegistro())
        .thenReturn(Optional.of(LocalDateTime.of(2024, 3, 1, 10, 0)));
    when(independientes.contarRegistradasPorMes(YearMonth.of(2025, 11), OCTUBRE))
        .thenReturn(Map.of(YearMonth.of(2025, 11), 96L, YearMonth.of(2026, 10), 58L));
    when(independientes.contarQuePasaronAPorMes(
            TipoEstadoDonacion.ENTREGADA, YearMonth.of(2025, 11), OCTUBRE))
        .thenReturn(Map.of(YearMonth.of(2026, 1), 90L, YearMonth.of(2026, 10), 9L));
    when(categorias.findAll()).thenReturn(List.of());

    EvolucionAdminDTO evolucion = service.calcular();

    assertEquals("2025-11", evolucion.desde());
    assertEquals("2026-10", evolucion.hasta());
    assertEquals(12, evolucion.meses().size());
    assertEquals(new EvolucionAdminDTO.Mes("2025-11", 96, 0), evolucion.meses().get(0));
    assertEquals(new EvolucionAdminDTO.Mes("2026-01", 0, 90), evolucion.meses().get(2));
    assertEquals(new EvolucionAdminDTO.Mes("2026-10", 58, 9), evolucion.meses().get(11));
    assertEquals(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC), evolucion.calculadoEn());
    verify(independientes, never()).findAll();
  }

  @Test
  void plataformaNueva_losMesesEmpiezanEnElDeLaPrimeraDonacion() {
    when(independientes.primeraFechaRegistro())
        .thenReturn(Optional.of(LocalDateTime.of(2026, 7, 20, 9, 0)));
    YearMonth julio = YearMonth.of(2026, 7);
    when(independientes.contarRegistradasPorMes(julio, OCTUBRE)).thenReturn(Map.of());
    when(independientes.contarQuePasaronAPorMes(TipoEstadoDonacion.ENTREGADA, julio, OCTUBRE))
        .thenReturn(Map.of());
    when(categorias.findAll()).thenReturn(List.of());

    EvolucionAdminDTO evolucion = service.calcular();

    assertEquals("2026-07", evolucion.desde());
    assertEquals(
        List.of("2026-07", "2026-08", "2026-09", "2026-10"),
        evolucion.meses().stream().map(EvolucionAdminDTO.Mes::mes).toList());
  }

  @Test
  void sinDonaciones_sinMesesNiCategorias_yNoCuentaNada() {
    when(independientes.primeraFechaRegistro()).thenReturn(Optional.empty());

    EvolucionAdminDTO evolucion = service.calcular();

    assertNull(evolucion.desde());
    assertNull(evolucion.hasta());
    assertTrue(evolucion.meses().isEmpty());
    assertTrue(evolucion.categorias().isEmpty());
    assertNull(evolucion.otras());
    verify(independientes, never()).contarRegistradasPorMes(any(), any());
  }

  @Test
  void categorias_todasConSuCero_deMasAMenos_yPorNombreSiEmpatan() {
    when(independientes.primeraFechaRegistro())
        .thenReturn(Optional.of(LocalDateTime.of(2024, 3, 1, 10, 0)));
    Categoria ropa = categoria("Ropa");
    Categoria alimentos = categoria("Alimentos");
    Categoria juguetes = categoria("Juguetes");
    Categoria higiene = categoria("Higiene");
    when(categorias.findAll()).thenReturn(List.of(ropa, juguetes, alimentos, higiene));
    when(independientes.contarRegistradasPorCategoria(INICIO_12, FIN))
        .thenReturn(Map.of(alimentos.getId(), 542L, ropa.getId(), 287L, higiene.getId(), 287L));
    when(independientes.contarQuePasaronAPorCategoria(TipoEstadoDonacion.ENTREGADA, INICIO_12, FIN))
        .thenReturn(Map.of(alimentos.getId(), 461L));

    EvolucionAdminDTO evolucion = service.calcular();

    assertEquals(
        List.of(
            new EvolucionAdminDTO.Categoria(alimentos.getId(), "Alimentos", 542, 461),
            new EvolucionAdminDTO.Categoria(higiene.getId(), "Higiene", 287, 0),
            new EvolucionAdminDTO.Categoria(ropa.getId(), "Ropa", 287, 0),
            new EvolucionAdminDTO.Categoria(juguetes.getId(), "Juguetes", 0, 0)),
        evolucion.categorias());
    assertNull(evolucion.otras());
  }

  @Test
  void masDeOchoCategorias_lasOchoPrimerasYLaSumaDeLasOtras() {
    when(independientes.primeraFechaRegistro())
        .thenReturn(Optional.of(LocalDateTime.of(2024, 3, 1, 10, 0)));
    List<Categoria> once = new ArrayList<>();
    Map<UUID, Long> registradas = new HashMap<>();
    Map<UUID, Long> entregadas = new HashMap<>();
    for (int i = 0; i < 11; i++) {
      Categoria c = categoria("Categoria " + (char) ('A' + i));
      once.add(c);
      registradas.put(c.getId(), 100L - i);
      entregadas.put(c.getId(), 1L);
    }
    when(categorias.findAll()).thenReturn(once);
    when(independientes.contarRegistradasPorCategoria(INICIO_12, FIN)).thenReturn(registradas);
    when(independientes.contarQuePasaronAPorCategoria(TipoEstadoDonacion.ENTREGADA, INICIO_12, FIN))
        .thenReturn(entregadas);

    EvolucionAdminDTO evolucion = service.calcular();

    assertEquals(8, evolucion.categorias().size());
    assertEquals("Categoria A", evolucion.categorias().get(0).nombre());
    // Las tres que pasan del tope: 92 + 91 + 90
    assertEquals(new EvolucionAdminDTO.Otras(3, 273, 3), evolucion.otras());
  }
}
