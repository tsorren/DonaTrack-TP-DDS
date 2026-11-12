package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;

import grupo5.cliente.puertos.PublicoMapaPuerto;
import grupo5.cliente.rest.LogisticaApi.EntregaEnElMapa;
import grupo5.cliente.vistas.PublicoMapaVista;
import grupo5.cliente.vistas.PublicoMapaVista.Entrega;
import grupo5.cliente.vistas.PublicoMapaVista.Estacion;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * El mapa de entregadas con las del mapa público de Logística ({@code /api/entregas/mapa}): las
 * confirmadas del último mes, con su punto exacto (D24, ADR-E6-07) y sin el texto de la dirección.
 * Cada punto es un lugar (las entregas al mismo punto van juntas): Logística no dice qué entidad
 * recibió ni qué se entregó, y Donaciones no da el lugar, así que no hay entidad, categoría, foto
 * ni "qué pide". La pantalla lo dice y oculta el filtro y las fotos.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.publico", havingValue = "rest")
class PublicoMapaRest implements PublicoMapaPuerto {

  private static final String NOTA =
      "Todavía no mostramos qué entidad recibió cada entrega, qué se entregó ni su foto: las fotos están en Entregas confirmadas.";
  private static final int DIAS_DEL_MES = 30;
  private static final double MIL = 10_000.0;
  private static final DateTimeFormatter ACTUALIZADO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Comun.ES_AR);
  private static final DateTimeFormatter SOLO_DIA = DateTimeFormatter.ofPattern("d", Comun.ES_AR);

  private final LogisticaApi logistica;
  private final Clock reloj;

  PublicoMapaRest(LogisticaApi logistica, Clock reloj) {
    this.logistica = logistica;
    this.reloj = reloj.withZone(ARGENTINA);
  }

  @Override
  public PublicoMapaVista mapa() {
    LocalDateTime ahora = LocalDateTime.now(reloj);
    LocalDate hoy = ahora.toLocalDate();
    LocalDate desde = hoy.minusDays(DIAS_DEL_MES);
    // Un punto por lugar: las de un mismo destino caen en las mismas coordenadas (a unos 10 m).
    Map<String, List<EntregaEnElMapa>> porLugar =
        logistica.mapa().stream()
            .filter(e -> e.fechaEntrega() != null && e.latitud() != null && e.longitud() != null)
            .filter(e -> !fecha(e).isBefore(desde))
            .sorted(Comparator.comparing(EntregaEnElMapa::fechaEntrega).reversed())
            .collect(
                Collectors.groupingBy(
                    e -> Math.round(e.latitud() * MIL) + "," + Math.round(e.longitud() * MIL),
                    LinkedHashMap::new,
                    Collectors.toList()));
    List<Estacion> estaciones = new ArrayList<>();
    for (List<EntregaEnElMapa> entregas : porLugar.values()) {
      estaciones.add(estacion(estaciones.size() + 1, entregas));
    }
    return new PublicoMapaVista(
        estaciones, rango(desde, hoy), ACTUALIZADO.format(ahora), false, false, NOTA);
  }

  private static LocalDate fecha(EntregaEnElMapa e) {
    return e.fechaEntrega().atZone(ARGENTINA).toLocalDate();
  }

  private static Estacion estacion(int numero, List<EntregaEnElMapa> entregas) {
    EntregaEnElMapa primera = entregas.getFirst();
    String nombre =
        primera.localidad() == null || primera.localidad().isBlank()
            ? "Lugar sin nombre"
            : primera.localidad();
    return new Estacion(
        "lugar-" + numero,
        numero,
        primera.latitud(),
        primera.longitud(),
        nombre,
        primera.provincia() == null ? "" : primera.provincia(),
        List.of(),
        entregas.stream().map(e -> entrega(e, nombre)).toList());
  }

  private static Entrega entrega(EntregaEnElMapa e, String lugar) {
    LocalDate dia = fecha(e);
    return new Entrega(
        peso(e.pesoTotalKG()),
        null,
        null,
        DIA.format(dia),
        String.valueOf(dia.getYear()),
        dia.toString(),
        lugar,
        null,
        null);
  }

  /** "Entrega de 12,5 kg"; sin peso, "Entrega confirmada". */
  private static String peso(Float kg) {
    if (kg == null || kg <= 0) {
      return "Entrega confirmada";
    }
    NumberFormat formato = NumberFormat.getNumberInstance(Comun.ES_AR);
    formato.setMaximumFractionDigits(1);
    return "Entrega de " + formato.format(kg) + " kg";
  }

  /** "del 8 al 15 de octubre de 2026", "del 16 de septiembre al 15 de octubre de 2026". */
  private static String rango(LocalDate desde, LocalDate hasta) {
    DateTimeFormatter inicio;
    if (desde.getYear() != hasta.getYear()) {
      inicio = DIA_Y_ANIO;
    } else if (desde.getMonthValue() != hasta.getMonthValue()) {
      inicio = DIA;
    } else {
      inicio = SOLO_DIA;
    }
    return "del " + inicio.format(desde) + " al " + DIA_Y_ANIO.format(hasta);
  }
}
