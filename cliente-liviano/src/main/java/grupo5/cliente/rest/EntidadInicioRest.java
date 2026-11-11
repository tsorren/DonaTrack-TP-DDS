package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;
import static grupo5.cliente.rest.Comun.ES_AR;
import static grupo5.cliente.rest.Comun.cantidad;
import static grupo5.cliente.rest.Comun.icono;

import grupo5.cliente.puertos.EntidadInicioPuerto;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.rest.DonacionesApi.Periodo;
import grupo5.cliente.rest.DonacionesApi.Subcategoria;
import grupo5.cliente.vistas.EntidadInicioVista;
import grupo5.cliente.vistas.EntidadInicioVista.Necesidad;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * El Inicio de la entidad desde Donaciones (las necesidades vigentes, con lo recibido del período
 * en curso de las recurrentes: el más nuevo, que pasa a CUMPLIDO apenas se cumple). Lo recibido de
 * una extraordinaria no se sabe todavía (fuera de alcance): va sin cifra.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class EntidadInicioRest implements EntidadInicioPuerto {

  private static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES_AR);
  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", ES_AR);

  private final DonacionesApi donaciones;
  private final Clock reloj;

  EntidadInicioRest(DonacionesApi donaciones, Clock reloj) {
    this.donaciones = donaciones;
    this.reloj = reloj;
  }

  @Override
  public EntidadInicioVista inicio(UUID entidad) {
    LocalDateTime ahora = LocalDateTime.now(reloj.withZone(ARGENTINA));
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("entidadId", entidad.toString());
    filtros.add("estado", "VIGENTE");
    List<DonacionesApi.Necesidad> vigentes = donaciones.necesidades(filtros);
    Map<UUID, Subcategoria> subcategorias =
        vigentes.isEmpty()
            ? Map.of()
            : donaciones.subcategorias().stream()
                .filter(s -> s.id() != null)
                .collect(Collectors.toMap(Subcategoria::id, Function.identity(), (a, b) -> a));
    return new EntidadInicioVista(
        ahora.format(FECHA),
        ahora.format(DIA_Y_HORA),
        vigentes.stream().map(n -> necesidad(n, subcategorias.get(n.idSubcategoria()))).toList());
  }

  private Necesidad necesidad(DonacionesApi.Necesidad n, Subcategoria subcategoria) {
    Categoria categoria = subcategoria == null ? null : subcategoria.categoria();
    // Como en Necesidades: el nombre es el de la subcategoría; la descripción es el motivo.
    String nombre =
        subcategoria != null && subcategoria.nombre() != null
            ? subcategoria.nombre()
            : n.descripcion();
    String icono =
        categoria == null || categoria.nombre() == null ? null : icono(categoria.nombre());
    Categoria conUnidad = new Categoria(null, null, n.unidad(), null, null, null);
    if ("RECURRENTE".equals(n.tipo())) {
      Periodo actual =
          donaciones.periodos(n.id()).stream()
              .filter(p -> p.inicio() != null)
              .max(Comparator.comparing(Periodo::inicio))
              .orElse(null);
      if (actual != null && actual.objetivo() != null && actual.recibido() != null) {
        return new Necesidad(
            nombre,
            icono,
            actual.recibido() + " de " + cantidad(actual.objetivo(), conUnidad),
            actual.recibido(),
            actual.objetivo(),
            hastaFinDe(actual.fin()));
      }
    }
    LocalDate hasta = n.fechaLimite() != null ? n.fechaLimite() : n.fechaFin();
    int pedida = n.cantidadNecesitada() == null ? 0 : n.cantidadNecesitada();
    return new Necesidad(
        nombre, icono, null, null, pedida, hasta == null ? null : "Hasta el " + hasta.format(DIA));
  }

  /** "Octubre, hasta el 31". */
  private static String hastaFinDe(LocalDate fin) {
    if (fin == null) {
      return null;
    }
    String mes = fin.format(MES);
    return Character.toUpperCase(mes.charAt(0))
        + mes.substring(1)
        + ", hasta el "
        + fin.getDayOfMonth();
  }
}
