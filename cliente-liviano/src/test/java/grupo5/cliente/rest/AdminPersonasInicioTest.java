package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Camiones;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

/**
 * R11-21: el inicio del admin por temas. Las cifras salen del resumen de Donaciones (ninguna lista
 * se trae para contarla, tampoco las propuestas), con separador de miles en las frases, como mucho
 * 3 nombres de organizaciones y la tarjeta Camiones sola si Logística no responde.
 */
class AdminPersonasInicioTest {

  private static final LocalDateTime AHORA = LocalDateTime.parse("2026-10-15T18:31:00");
  private static final String AL = "15 de octubre de 2026, 18:31";

  private final DonacionesApi donaciones = mock(DonacionesApi.class);
  private final LogisticaApi logistica = mock(LogisticaApi.class);

  private static DonacionesApi.Resumen resumen(
      Map<String, Long> porEstado, long vencen, long propuestas) {
    return new DonacionesApi.Resumen(
        porEstado,
        new DonacionesApi.EntregadasEnElMes("2026-10", 9L),
        new DonacionesApi.PorVencer(
            7,
            vencen,
            vencen == 0
                ? null
                : new DonacionesApi.Proxima(
                    UUID.fromString("00000000-0000-4000-8000-0000000000d1"),
                    "Leche en polvo",
                    LocalDate.parse("2026-10-17"))),
        new DonacionesApi.CuantosDonantes(20_530L, 20_412L),
        1_046L,
        propuestas,
        OffsetDateTime.parse("2026-10-15T18:30:00-03:00"));
  }

  private AdminInicioVista inicio(List<String> pendientes) {
    return new AdminPersonasInicio(donaciones, logistica).inicio(AHORA, AL, pendientes);
  }

  @Test
  void lasCifrasVanPorTemaConLosEstadosAgrupadosComoEnLaLista() {
    when(donaciones.resumen())
        .thenReturn(
            resumen(
                Map.of(
                    "EN_DEPOSITO", 1_207L,
                    "ASIGNACION_REALIZADA", 3L,
                    "LISTA_PARA_ENTREGAR", 1L,
                    "EN_TRASLADO", 1L,
                    "EN_PUERTA", 1L,
                    "ENTREGA_FALLIDA", 1L,
                    "VENCIDA", 2L,
                    "ENTREGADA", 1_284L),
                4,
                2));
    when(logistica.camiones(any()))
        .thenReturn(
            List.of(
                new LogisticaApi.Camion(null, "AA000AA", null, null, null, "EN_RUTA", null),
                new LogisticaApi.Camion(null, "AA000AB", null, null, null, "DISPONIBLE", null)));

    AdminInicioVista inicio = inicio(List.of());

    assertEquals(
        new AdminInicioVista.Cifras(
            1_207,
            4,
            2,
            1,
            2,
            9,
            "en octubre, del 1 al 15",
            1_284,
            1_046,
            20_412,
            20_530,
            new Camiones(1, 1)),
        inicio.cifras());
    assertEquals("15 de octubre de 2026, 18:30", inicio.al());
    verify(donaciones, never()).propuestas(any());
  }

  @Test
  void lasFrasesLlevanSeparadorDeMilesYComoMuchoTresNombres() {
    when(donaciones.resumen()).thenReturn(resumen(Map.of("EN_DEPOSITO", 1_207L), 1_050L, 1_100L));
    when(logistica.camiones(any())).thenReturn(List.of());
    List<String> pendientes =
        List.of("Hogar Ejemplo Norte", "Merendero Ejemplo Este", "Comedor Ejemplo Oeste", "Otra");

    AdminInicioVista inicio = inicio(pendientes);

    assertEquals(
        "Hoy esperan tu decisión 4 organizaciones y 1.100 propuestas de asignación. Hay 1.207"
            + " donaciones sin asignar en el depósito; 1.050 vencen en los próximos 7 días.",
        inicio.resumen());
    assertEquals(
        new Decisiones(
            4,
            List.of("Hogar Ejemplo Norte", "Merendero Ejemplo Este", "Comedor Ejemplo Oeste"),
            1,
            1_207,
            1_050,
            "La primera, Leche en polvo, vence el 17 de octubre, en 2 días.",
            1_100),
        inicio.decisiones());
  }

  @Test
  void enSingularYSinVencimientos() {
    assertEquals(
        "Hoy espera tu decisión 1 organización. Hay 1 donación sin asignar en el depósito; vence"
            + " en los próximos 7 días.",
        AdminPersonasInicio.resumen(new Decisiones(1, List.of("Hogar"), 0, 1, 1, null, 0)));
    assertEquals(
        "Hay 7 donaciones sin asignar en el depósito; 1 vence en los próximos 7 días.",
        AdminPersonasInicio.resumen(new Decisiones(0, List.of(), 0, 7, 1, null, 0)));
    assertEquals(
        "Hay 7 donaciones sin asignar en el depósito.",
        AdminPersonasInicio.resumen(new Decisiones(0, List.of(), 0, 7, 0, null, 0)));
    assertNull(AdminPersonasInicio.resumen(new Decisiones(0, List.of(), 0, 0, 0, null, 0)));
  }

  @Test
  void siLogisticaNoRespondeSoloFaltanLosCamiones() {
    when(donaciones.resumen()).thenReturn(resumen(Map.of("EN_DEPOSITO", 7L), 0, 0));
    when(logistica.camiones(any())).thenThrow(new RestClientException("sin Logística"));

    AdminInicioVista inicio = inicio(List.of());

    assertNull(inicio.cifras().camiones());
    assertEquals(7, inicio.cifras().enDeposito());
    assertEquals(7, inicio.decisiones().sinAsignar());
  }

  @Test
  void elPrimerDiaDelMesYSinHoraDelResumen() {
    when(donaciones.resumen())
        .thenReturn(new DonacionesApi.Resumen(null, null, null, null, null, null, null));
    when(logistica.camiones(any())).thenReturn(List.of());

    AdminInicioVista inicio =
        new AdminPersonasInicio(donaciones, logistica)
            .inicio(
                LocalDateTime.parse("2026-11-01T09:00:00"),
                "1 de noviembre de 2026, 09:00",
                List.of());

    assertEquals("el 1 de noviembre", inicio.cifras().delMes());
    assertEquals("1 de noviembre de 2026, 09:00", inicio.al());
    assertEquals(0, inicio.cifras().entregadasEnTotal());
  }

  // --- R11-42: los gráficos, con lo que ya cuenta Donaciones -----------------------------------

  private static final OffsetDateTime CALCULADO = OffsetDateTime.parse("2026-10-15T18:30:00-03:00");

  private static DonacionesApi.Evolucion doceMeses() {
    long[] registradas = {96, 132, 88, 74, 110, 121, 138, 152, 147, 165, 143, 58};
    long[] entregadas = {71, 104, 90, 66, 85, 102, 117, 128, 131, 139, 119, 9};
    List<DonacionesApi.MesEvolucion> meses = new java.util.ArrayList<>();
    for (int i = 0; i < 12; i++) {
      meses.add(
          new DonacionesApi.MesEvolucion(
              java.time.YearMonth.of(2025, 11).plusMonths(i).toString(),
              registradas[i],
              entregadas[i]));
    }
    return new DonacionesApi.Evolucion(
        "2025-11",
        "2026-10",
        meses,
        List.of(
            new DonacionesApi.CategoriaEvolucion(UUID.randomUUID(), "Alimentos", 542L, 461L),
            new DonacionesApi.CategoriaEvolucion(UUID.randomUUID(), "Ropa", 287L, 241L),
            new DonacionesApi.CategoriaEvolucion(UUID.randomUUID(), "Electrodomésticos", 0L, 0L)),
        new DonacionesApi.OtrasCategorias(3, 595L, 459L),
        CALCULADO);
  }

  @Test
  void losGraficosTraenLosTextosDeLaFichaYLaEscalaRedondeada() {
    AdminInicioVista.Evolucion e =
        AdminPersonasInicio.evolucion(doceMeses(), LocalDate.parse("2026-10-15"));

    assertEquals("de noviembre de 2025 a octubre de 2026", e.periodo());
    assertEquals(
        "En los últimos 12 meses se registraron 1.424 donaciones y se confirmaron 1.161 entregas."
            + " El mes con más donaciones registradas fue agosto de 2026, con 165.",
        e.frase());
    assertEquals(
        "Una entrega cuenta en el mes en que la organización la confirmó: puede ser de una donación"
            + " registrada antes. Octubre todavía no terminó.",
        e.nota());
    assertEquals(200, e.tope());
    assertEquals(100, e.mitad());
    AdminInicioVista.Mes primero = e.meses().get(0);
    assertEquals(
        new AdminInicioVista.Mes("nov", "2025", "Noviembre 2025", false, 96, 71, 48, 36), primero);
    assertEquals("2026", e.meses().get(2).anio());
    assertNull(e.meses().get(3).anio());
    AdminInicioVista.Mes ultimo = e.meses().get(11);
    assertEquals("Octubre 2026, del 1 al 15", ultimo.nombre());
    assertEquals(true, ultimo.enCurso());
    assertEquals(1_424, e.total().registradas());
    assertEquals(
        "Alimentos es la categoría con más donaciones registradas en estos 12 meses: 542 de"
            + " 1.424.",
        e.fraseCategorias());
    assertEquals(
        new AdminInicioVista.Fila("Alimentos", "cat-alimentos", 542, 461, 91, 77),
        e.categorias().get(0));
    assertEquals(
        new AdminInicioVista.Fila("Electrodomésticos", "cat-electrodomesticos", 0, 0, 0, 0),
        e.categorias().get(2));
    // "Otras 3 categorías" con la suma, la mayor de las barras (595 = 100)
    assertEquals(
        new AdminInicioVista.Fila("Otras 3 categorías", "cat-generico", 595, 459, 100, 77),
        e.categorias().get(3));
    assertEquals(1_424, e.totalCategorias().registradas());
  }

  @Test
  void plataformaNueva_noDiceDoceMeses_yElEmpateNombraALasDos() {
    DonacionesApi.Evolucion corta =
        new DonacionesApi.Evolucion(
            "2026-07",
            "2026-10",
            List.of(
                new DonacionesApi.MesEvolucion("2026-07", 1L, 0L),
                new DonacionesApi.MesEvolucion("2026-08", 0L, 0L),
                new DonacionesApi.MesEvolucion("2026-09", 0L, 0L),
                new DonacionesApi.MesEvolucion("2026-10", 3L, 1L)),
            List.of(
                new DonacionesApi.CategoriaEvolucion(UUID.randomUUID(), "Alimentos", 2L, 1L),
                new DonacionesApi.CategoriaEvolucion(UUID.randomUUID(), "Ropa", 2L, 0L)),
            null,
            CALCULADO);

    AdminInicioVista.Evolucion e =
        AdminPersonasInicio.evolucion(corta, LocalDate.parse("2026-10-15"));

    assertEquals("de julio a octubre de 2026", e.periodo());
    assertEquals(
        "Desde julio de 2026, cuando se registró la primera donación, se registraron 4 donaciones"
            + " y se confirmó 1 entrega. El mes con más donaciones registradas fue octubre de"
            + " 2026, con 3.",
        e.frase());
    assertEquals(10, e.tope());
    assertEquals(
        "Alimentos y Ropa son las categorías con más donaciones registradas desde julio de 2026:"
            + " 2 cada una, de 4.",
        e.fraseCategorias());
  }

  @Test
  void sinDonacionesLosGraficosQuedanVacios() {
    AdminInicioVista.Evolucion e =
        AdminPersonasInicio.evolucion(
            new DonacionesApi.Evolucion(null, null, List.of(), List.of(), null, CALCULADO),
            LocalDate.parse("2026-10-15"));

    assertEquals(true, e.vacia());
  }

  @Test
  void siLaEvolucionNoLlegaSoloFaltanLosGraficos() {
    when(donaciones.resumen()).thenReturn(resumen(Map.of("EN_DEPOSITO", 7L), 0, 0));
    when(logistica.camiones(any())).thenReturn(List.of());
    when(donaciones.evolucion()).thenThrow(new RestClientException("sin series"));

    AdminInicioVista inicio = inicio(List.of());

    assertNull(inicio.evolucion());
    assertEquals(7, inicio.cifras().enDeposito());
  }

  @Test
  void laEscalaTerminaEnUnNumeroFacil() {
    assertEquals(10, AdminPersonasInicio.tope(0));
    assertEquals(10, AdminPersonasInicio.tope(7));
    assertEquals(20, AdminPersonasInicio.tope(11));
    assertEquals(50, AdminPersonasInicio.tope(30));
    assertEquals(100, AdminPersonasInicio.tope(100));
    assertEquals(200, AdminPersonasInicio.tope(165));
    assertEquals(1_000, AdminPersonasInicio.tope(600));
  }
}
