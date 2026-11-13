package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;
import static grupo5.cliente.rest.Comun.ENTREGADA;
import static grupo5.cliente.rest.Comun.ES_AR;
import static grupo5.cliente.rest.Comun.FALLIDA;
import static grupo5.cliente.rest.Comun.PUERTA;
import static grupo5.cliente.rest.Comun.TRASLADO;
import static grupo5.cliente.rest.Comun.VENCIDA;
import static grupo5.cliente.rest.Comun.cantidad;
import static grupo5.cliente.rest.Comun.categoria;
import static grupo5.cliente.rest.Comun.desde;
import static grupo5.cliente.rest.Comun.icono;
import static grupo5.cliente.rest.Comun.normalizado;

import grupo5.cliente.puertos.EntidadNecesidadesPuerto;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.rest.DonacionesApi.Subcategoria;
import grupo5.cliente.vistas.EntidadNecesidadesVista;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Donacion;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Entrega;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Grupo;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Necesidad;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Opcion;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Periodo;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Rubro;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Terminada;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Necesidades de la entidad desde Donaciones: las suyas ({@code GET /api/necesidades?entidadId}),
 * el catálogo ({@code /api/subcategorias}), lo asignado ({@code /donaciones-independientes
 * ?entidadId}, por {@code necesidadId}: las entregas y quién donó) y los períodos de las que se
 * repiten. Lo recibido de una que es una sola vez no está en la API (fuera de alcance): va sin
 * cifra. "En tu puerta" son las que llegaron ({@code EN_PUERTA}, HB-22).
 *
 * <p>Desde R11-43 no se leen todas las asignadas: las cifras de cada necesidad salen de una sola
 * llamada a {@code /donaciones-independientes/conteos} y, de cada vigente, se piden como mucho
 * {@value Necesidad#MUESTRA} en curso (las más urgentes, {@code orden=ATENCION}) y {@value
 * Necesidad#MUESTRA} recibidas (las más recientes), solo si las hay.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class EntidadNecesidadesRest implements EntidadNecesidadesPuerto {

  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", ES_AR);
  private static final DateTimeFormatter MES_Y_ANIO =
      DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ES_AR);
  private static final String VIGENTE = "VIGENTE";
  private static final String RECURRENTE = "RECURRENTE";
  private static final Map<String, String> REPITE =
      Map.of("SEMANAL", "Cada semana", "QUINCENAL", "Cada quincena", "MENSUAL", "Cada mes");
  private static final Map<String, String> TERMINO =
      Map.of(
          "CUMPLIDA", "Cumplida.",
          "TERMINADA", "Terminó sin cumplirse.",
          "DADA_DE_BAJA", "La diste de baja.");

  /** Los estados de lo que todavía no se recibió, de lo más urgente a lo que espera salir. */
  private static final List<String> EN_CURSO =
      List.of(
          "EN_PUERTA",
          "ENTREGA_FALLIDA",
          "VENCIDA",
          "EN_TRASLADO",
          "ASIGNACION_REALIZADA",
          "LISTA_PARA_ENTREGAR",
          "EN_DEPOSITO");

  private final DonacionesApi api;
  private final Clock reloj;

  EntidadNecesidadesRest(DonacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj;
  }

  @Override
  public EntidadNecesidadesVista necesidades(UUID entidad, String accion) {
    LocalDate hoy = LocalDate.now(reloj.withZone(ARGENTINA));
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("entidadId", entidad.toString());
    List<DonacionesApi.Necesidad> suyas = api.necesidades(filtros);
    List<Subcategoria> catalogo = api.subcategorias();
    Map<UUID, Subcategoria> subcategorias =
        catalogo.stream()
            .filter(s -> s.id() != null)
            .collect(Collectors.toMap(Subcategoria::id, Function.identity(), (a, b) -> a));
    Map<UUID, Map<String, Long>> porNecesidad =
        suyas.stream().noneMatch(n -> VIGENTE.equals(n.estado()))
            ? Map.of()
            : porNecesidad(api.conteos(entidad, null));
    List<Necesidad> vigentes =
        suyas.stream()
            .filter(n -> VIGENTE.equals(n.estado()))
            .map(
                n ->
                    necesidad(
                        entidad,
                        n,
                        subcategorias.get(n.idSubcategoria()),
                        porNecesidad.getOrDefault(n.id(), Map.of()),
                        hoy))
            .toList();
    List<Rubro> rubros =
        vigentes.stream()
            .map(
                n ->
                    new Rubro(
                        n.categoria() != null ? n.categoria() : n.rubro(),
                        n.rubro(),
                        n.categoria()))
            .distinct()
            .toList();
    return new EntidadNecesidadesVista(
        hoy.format(DIA_Y_ANIO),
        hoy.toString(),
        hoy.format(DIA),
        vigentes,
        rubros,
        suyas.stream()
            .filter(n -> !VIGENTE.equals(n.estado()))
            .map(n -> terminada(n, subcategorias.get(n.idSubcategoria())))
            .toList(),
        catalogo(catalogo),
        vigentes.stream()
            .flatMap(n -> n.entregas().stream())
            .map(Entrega::confirmar)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null),
        vigentes.stream()
            .filter(n -> n.problema() > 0)
            .map(Necesidad::clave)
            .findFirst()
            .orElse(null));
  }

  /** De cada necesidad con alguna asignada, cuántas hay en cada estado. */
  private static Map<UUID, Map<String, Long>> porNecesidad(DonacionesApi.Conteos conteos) {
    if (conteos == null || conteos.porNecesidad() == null) {
      return Map.of();
    }
    return conteos.porNecesidad().stream()
        .filter(n -> n.necesidadId() != null && n.porEstado() != null)
        .collect(
            Collectors.toMap(
                DonacionesApi.PorNecesidad::necesidadId,
                n ->
                    n.porEstado().stream()
                        .filter(e -> e.estado() != null && e.cantidad() != null)
                        .collect(
                            Collectors.toMap(
                                DonacionesApi.PorEstado::estado,
                                DonacionesApi.PorEstado::cantidad,
                                Long::sum)),
                (a, b) -> a));
  }

  /**
   * Como mucho {@value Necesidad#MUESTRA} asignadas a la necesidad en {@code estados}; sin llamada
   * si no hay ninguna ({@code cuantas} 0).
   */
  private List<DonacionesApi.Donacion> muestra(
      UUID entidad, UUID necesidad, List<String> estados, String orden, long cuantas) {
    if (cuantas <= 0) {
      return List.of();
    }
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("entidadId", entidad.toString());
    filtros.add("necesidadId", necesidad.toString());
    estados.forEach(e -> filtros.add("estado", e));
    filtros.add("orden", orden);
    filtros.add("size", String.valueOf(Necesidad.MUESTRA));
    List<DonacionesApi.Donacion> pagina = api.pagina(filtros).getBody();
    return pagina == null ? List.of() : pagina;
  }

  private static int suma(Map<String, Long> porEstado, List<String> estados) {
    return (int) estados.stream().mapToLong(e -> porEstado.getOrDefault(e, 0L)).sum();
  }

  private Necesidad necesidad(
      UUID entidad,
      DonacionesApi.Necesidad n,
      Subcategoria sub,
      Map<String, Long> porEstado,
      LocalDate hoy) {
    int puerta = suma(porEstado, RecibidasRest.ESTADOS.get("puerta"));
    int problema = suma(porEstado, RecibidasRest.ESTADOS.get("problema"));
    int salieron = suma(porEstado, RecibidasRest.ESTADOS.get("camino"));
    int noSalieron = suma(porEstado, RecibidasRest.ESTADOS.get("salir"));
    int recibidas = suma(porEstado, RecibidasRest.ESTADOS.get("recibida"));
    List<DonacionesApi.Donacion> asignadas = new ArrayList<>();
    asignadas.addAll(
        muestra(
            entidad,
            n.id(),
            EN_CURSO,
            "ATENCION",
            (long) puerta + problema + salieron + noSalieron));
    asignadas.addAll(
        muestra(entidad, n.id(), RecibidasRest.ESTADOS.get("recibida"), "RECIENTES", recibidas));
    Categoria conUnidad = new Categoria(null, null, n.unidad(), null, null, null);
    String unidad = unidad(conUnidad);
    boolean repite = RECURRENTE.equals(n.tipo());
    List<DonacionesApi.Periodo> periodos =
        repite
            ? api.periodos(n.id()).stream()
                .filter(p -> p.inicio() != null)
                .sorted(Comparator.comparing(DonacionesApi.Periodo::inicio).reversed())
                .toList()
            : List.of();
    // El período en curso es el más nuevo: pasa a CUMPLIDO apenas se cumple, sin terminar.
    DonacionesApi.Periodo actual = periodos.isEmpty() ? null : periodos.getFirst();
    int pedida = n.cantidadNecesitada() == null ? 0 : n.cantidadNecesitada();
    LocalDate hasta =
        actual != null ? actual.fin() : (n.fechaLimite() != null ? n.fechaLimite() : n.fechaFin());
    List<Entrega> entregas = new ArrayList<>();
    List<Donacion> donaron = new ArrayList<>();
    for (DonacionesApi.Donacion d : asignadas) {
      String estado = normalizado(d.estadoActual());
      String cuanto = cantidad(d.cantidad(), categoria(d));
      if (ENTREGADA.equals(estado)) {
        DonacionesApi.QuienDono quien = d.quienDono();
        donaron.add(
            new Donacion(
                quien == null || quien.nombreVisible() == null || quien.nombreVisible().isBlank()
                    ? null
                    : quien.nombreVisible().strip(),
                null,
                quien != null && "JURIDICA".equals(quien.tipo()) ? "empresa" : null,
                cuanto,
                desde(d, ENTREGADA).format(DIA)));
        continue;
      }
      boolean conProblema = FALLIDA.equals(estado) || VENCIDA.equals(estado);
      boolean enLaPuerta = PUERTA.equals(estado);
      String tono;
      if (conProblema) {
        tono = "con-problema";
      } else if (enLaPuerta) {
        tono = "puerta";
      } else {
        tono = TRASLADO.equals(estado) ? "en-camino" : "pendiente";
      }
      entregas.add(
          new Entrega(
              cuanto,
              tono,
              RecibidasRest.paso(estado, desde(d, estado)),
              enLaPuerta ? d.id().toString() : null));
    }
    String nombre = sub != null && sub.nombre() != null ? sub.nombre() : n.descripcion();
    Categoria categoria = sub == null ? null : sub.categoria();
    String rubro = categoria == null || categoria.nombre() == null ? "Otras" : categoria.nombre();
    return new Necesidad(
        "necesidad-" + n.id(),
        n.id().toString(),
        n.version(),
        nombre,
        categoria == null || categoria.nombre() == null ? null : icono(categoria.nombre()),
        rubro,
        unidad,
        plazo(hasta, repite),
        faltan(hoy, hasta),
        repite ? REPITE.getOrDefault(n.periodicidad(), "Se repite") : "Una sola vez",
        actual == null ? null : actual.recibido(),
        actual != null && actual.objetivo() != null ? actual.objetivo() : pedida,
        cambio(n.ultimoCambio(), pedida, unidad, hoy, false),
        cambio(n.ultimoCambio(), pedida, unidad, hoy, true),
        n.descripcion(),
        puerta,
        salieron + noSalieron,
        problema,
        entregas,
        donaron,
        periodos.isEmpty() ? null : "Desde " + periodos.getLast().inicio().format(MES_Y_ANIO),
        periodos.stream().map(p -> periodo(p, unidad)).toList(),
        salieron,
        noSalieron,
        problema,
        recibidas);
  }

  private static Periodo periodo(DonacionesApi.Periodo p, String unidad) {
    int recibido = p.recibido() == null ? 0 : p.recibido();
    int objetivo = p.objetivo() == null ? 0 : p.objetivo();
    boolean enCurso = "EN_CURSO".equals(p.resultado());
    String tono =
        enCurso
            ? "curso"
            : ("CUMPLIDO".equals(p.resultado()) ? "todo" : (recibido > 0 ? "parte" : "nada"));
    String estado =
        switch (tono) {
          case "curso" -> "En curso";
          case "todo" -> "Llegó todo";
          case "parte" -> "Llegó una parte";
          default -> "No llegó nada";
        };
    String mes = p.inicio().format(MES);
    return new Periodo(
        Character.toUpperCase(mes.charAt(0)) + mes.substring(1),
        p.fin() == null ? null : "del " + p.inicio().format(DIA) + " al " + p.fin().format(DIA),
        recibido + " de " + objetivo + " " + unidad,
        tono,
        estado,
        recibido,
        objetivo);
  }

  private static String plazo(LocalDate hasta, boolean repite) {
    if (hasta == null) {
      return null;
    }
    if (!repite) {
      return "Hasta el " + hasta.format(DIA);
    }
    String mes = hasta.format(MES);
    return Character.toUpperCase(mes.charAt(0))
        + mes.substring(1)
        + ", hasta el "
        + hasta.getDayOfMonth();
  }

  private static String faltan(LocalDate hoy, LocalDate hasta) {
    if (hasta == null) {
      return null;
    }
    long dias = ChronoUnit.DAYS.between(hoy, hasta);
    return dias <= 0 ? "termina hoy" : (dias == 1 ? "falta 1 día" : "faltan " + dias + " días");
  }

  /** El cartel (o la marca, con {@code marca}) del último cambio de cantidad, o null. */
  private static String cambio(
      DonacionesApi.UltimoCambio ultimo, int ahora, String unidad, LocalDate hoy, boolean marca) {
    if (ultimo == null || ultimo.fecha() == null || ultimo.cantidadAnterior() == null) {
      return null;
    }
    boolean deHoy = ultimo.fecha().equals(hoy);
    if (marca) {
      return deHoy ? "Cambiada hoy" : "Cambiada el " + ultimo.fecha().format(DIA);
    }
    return (deHoy ? "Hoy" : "El " + ultimo.fecha().format(DIA))
        + " pasó de "
        + ultimo.cantidadAnterior()
        + " a "
        + ahora
        + " "
        + unidad
        + ".";
  }

  private static Terminada terminada(DonacionesApi.Necesidad n, Subcategoria sub) {
    String nombre = sub != null && sub.nombre() != null ? sub.nombre() : n.descripcion();
    Categoria conUnidad = new Categoria(null, null, n.unidad(), null, null, null);
    int pedida = n.cantidadNecesitada() == null ? 0 : n.cantidadNecesitada();
    return new Terminada(
        nombre + " · " + cantidad(pedida, conUnidad), TERMINO.getOrDefault(n.estado(), ""));
  }

  /** Lo que se puede pedir, por categoría: "Arroz (kg)". */
  private static List<Grupo> catalogo(List<Subcategoria> subcategorias) {
    Map<String, List<Opcion>> grupos = new TreeMap<>();
    subcategorias.stream()
        .filter(s -> s.id() != null && s.nombre() != null)
        .forEach(
            s -> {
              Categoria categoria = s.categoria();
              String grupo =
                  categoria == null || categoria.nombre() == null ? "Otras" : categoria.nombre();
              String unidadDeLa =
                  s.unidad() != null ? s.unidad() : (categoria == null ? null : categoria.unidad());
              String unidad = unidad(new Categoria(null, null, unidadDeLa, null, null, null));
              grupos
                  .computeIfAbsent(grupo, g -> new ArrayList<>())
                  .add(new Opcion(s.id().toString(), s.nombre() + " (" + unidad + ")", false));
            });
    return grupos.entrySet().stream().map(e -> new Grupo(e.getKey(), e.getValue())).toList();
  }

  /** La unidad sola, la de Comun ("2 kg" sin el número); sin unidad, unidades. */
  private static String unidad(Categoria conUnidad) {
    String sola = cantidad(2, conUnidad).substring(1).strip();
    return sola.isEmpty() ? "unidades" : sola;
  }

  @Override
  public void publicar(
      UUID entidad,
      UUID subcategoria,
      int cantidad,
      String periodicidad,
      LocalDate hasta,
      String motivo) {
    api.publicarNecesidad(
        new DonacionesApi.Necesidad(
            null,
            periodicidad == null ? "EXTRAORDINARIA" : RECURRENTE,
            entidad,
            subcategoria,
            cantidad,
            motivo,
            null,
            null,
            null,
            null,
            null,
            periodicidad == null ? hasta : null,
            periodicidad,
            null,
            null));
  }

  @Override
  public void cambiar(UUID necesidad, long version, int cantidad, String motivo) {
    DonacionesApi.Necesidad n = Objects.requireNonNull(api.necesidad(necesidad));
    api.cambiarNecesidad(
        necesidad,
        new DonacionesApi.Necesidad(
            n.id(),
            n.tipo(),
            n.idEntidad(),
            n.idSubcategoria(),
            cantidad,
            motivo,
            n.estaSatisfecha(),
            n.fechaInicio(),
            n.fechaFin(),
            n.estado(),
            version,
            n.fechaLimite(),
            n.periodicidad(),
            n.unidad(),
            n.ultimoCambio()));
  }

  @Override
  public void darDeBaja(UUID necesidad) {
    api.darDeBajaNecesidad(necesidad);
  }
}
