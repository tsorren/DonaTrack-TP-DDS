package grupo5.cliente.rest;

import grupo5.cliente.puertos.MisionesPuerto;
import grupo5.cliente.vistas.MisionesVista;
import grupo5.cliente.vistas.MisionesVista.Categoria;
import grupo5.cliente.vistas.MisionesVista.Insignia;
import grupo5.cliente.vistas.MisionesVista.Mision;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador REST que consume el backend de Incentivos para armar la vista de misiones e insignias
 * (RF-07).
 */
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.misiones", havingValue = "rest")
class MisionesRest implements MisionesPuerto {

  private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final DateTimeFormatter DIA_Y_ANIO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_AR);
  private static final DateTimeFormatter DIA_Y_HORA =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", ES_AR);

  private static final String COLABORADOR = "Colaborador";
  private static final String SOSTENEDOR = "Sostenedor";
  private static final String TRANSFORMADOR = "Transformador";
  private static final List<String> ORDEN_CATEGORIAS =
      List.of(COLABORADOR, SOSTENEDOR, TRANSFORMADOR);

  private final IncentivosApi incentivos;
  private final DonacionesApi donaciones;
  private final Clock reloj;

  MisionesRest(IncentivosApi incentivos, DonacionesApi donaciones, Clock reloj) {
    this.incentivos = incentivos;
    this.donaciones = donaciones;
    this.reloj = reloj;
  }

  @Override
  public MisionesVista misiones(UUID donanteId) {
    if (!esPersonaHumana(donanteId)) {
      return MisionesVista.sinPermiso();
    }

    IncentivosApi.PerfilDonante perfil = incentivos.perfilDonante(donanteId);
    List<IncentivosApi.Mision> misionesRaw = incentivos.misiones(donanteId);
    if (misionesRaw == null) {
      misionesRaw = List.of();
    }

    List<IncentivosApi.Ascenso> ascensos = List.of();
    try {
      ascensos = incentivos.ascensos(donanteId);
    } catch (RestClientException ignorada) {
      // Si el donante no tiene ascensos o el endpoint da error, usamos la fecha de registro
    }

    String categoriaActual =
        perfil != null && perfil.categoria() != null
            ? normalizarCategoria(perfil.categoria())
            : COLABORADOR;

    int indiceActual = Math.max(0, ORDEN_CATEGORIAS.indexOf(categoriaActual));

    List<IncentivosApi.Mision> ordenadas =
        new ArrayList<>(
            misionesRaw.stream()
                .sorted(
                    Comparator.comparingInt(
                        (IncentivosApi.Mision m) ->
                            ORDEN_CATEGORIAS.indexOf(normalizarCategoria(m.categoria()))))
                .toList());

    List<Mision> listaMisiones = new ArrayList<>();
    String misionAnterior = null;
    String misionEnCurso = null;

    for (IncentivosApi.Mision m : ordenadas) {
      String cat = normalizarCategoria(m.categoria());
      int indiceCat = ORDEN_CATEGORIAS.indexOf(cat);

      String estado;
      String fecha = null;
      String espera = null;
      int progreso = m.progresoActual();
      int objetivo = m.objetivo();
      String cuenta = null;

      if (m.completada()) {
        estado = "completa";
        if (m.fechaCompletada() != null) {
          fecha = m.fechaCompletada().format(DIA_Y_ANIO);
        }
      } else if (indiceCat < indiceActual) {
        estado = "completa";
      } else if (indiceCat > indiceActual) {
        estado = "cerrada";
      } else {
        // En la categoría actual
        if (misionEnCurso == null) {
          estado = "curso";
          misionEnCurso = m.nombre();
          cuenta = formatearCuenta(progreso, objetivo, m.descripcion());
        } else {
          estado = "espera";
          espera = misionAnterior != null ? misionAnterior : misionEnCurso;
        }
      }

      misionAnterior = m.nombre();
      Insignia insignia = insignia(m.insignia(), fecha);
      listaMisiones.add(
          new Mision(
              cat,
              m.nombre(),
              m.descripcion(),
              estado,
              fecha,
              progreso,
              objetivo,
              cuenta,
              null,
              espera,
              insignia));
    }

    int completadas =
        (int) listaMisiones.stream().filter(m -> "completa".equals(m.estado())).count();
    boolean todasCompletadas = completadas >= 6;
    boolean nueva = completadas == 0;

    List<Categoria> categorias = new ArrayList<>();
    for (int i = 0; i < ORDEN_CATEGORIAS.size(); i++) {
      String nombreCat = ORDEN_CATEGORIAS.get(i);
      String estadoCat;
      String siguiente = null;
      String anterior = null;

      if (i < indiceActual) {
        estadoCat = "completa";
      } else if (i == indiceActual) {
        estadoCat = todasCompletadas ? "completa" : "tu categoría";
        if (i < ORDEN_CATEGORIAS.size() - 1 && !todasCompletadas) {
          siguiente = ORDEN_CATEGORIAS.get(i + 1);
        }
      } else {
        estadoCat = "se abre después";
        anterior = ORDEN_CATEGORIAS.get(i - 1);
      }
      categorias.add(new Categoria(nombreCat, estadoCat, siguiente, anterior));
    }

    LocalDate fechaDesde =
        ascensos != null && !ascensos.isEmpty()
            ? ascensos.get(ascensos.size() - 1).fecha()
            : (perfil != null && perfil.fechaRegistro() != null
                ? perfil.fechaRegistro()
                : LocalDate.now(reloj.withZone(ARGENTINA)));

    String momentoActual = LocalDateTime.now(reloj.withZone(ARGENTINA)).format(DIA_Y_HORA);
    String nombre = perfil != null && perfil.nombre() != null ? perfil.nombre().trim() : "";

    String bajada;
    if (nueva) {
      bajada =
          "Gracias por sumarte"
              + (nombre.isEmpty() ? "" : ", " + primerNombre(nombre))
              + ". Sos donante "
              + categoriaActual
              + ". Todavía no completaste ninguna misión. Al "
              + momentoActual
              + ".";
    } else if (todasCompletadas) {
      bajada =
          "Gracias por donar"
              + (nombre.isEmpty() ? "" : ", " + primerNombre(nombre))
              + ". Sos donante "
              + categoriaActual
              + " y completaste las 6 misiones. Al "
              + momentoActual
              + ".";
    } else {
      bajada =
          "Gracias por donar"
              + (nombre.isEmpty() ? "" : ", " + primerNombre(nombre))
              + ". Sos donante "
              + categoriaActual
              + " desde el "
              + (fechaDesde != null ? fechaDesde.format(DIA_Y_ANIO) : momentoActual)
              + ". Completaste "
              + completadas
              + " de las 6 misiones. Al "
              + momentoActual
              + ".";
    }

    List<Mision> obtenidas =
        listaMisiones.stream().filter(m -> "completa".equals(m.estado())).toList();

    return new MisionesVista(true, nueva, bajada, listaMisiones, categorias, obtenidas);
  }

  private boolean esPersonaHumana(UUID donanteId) {
    DonacionesApi.DonanteDetalle d = donaciones.donante(donanteId);
    return d != null && d.persona() != null && "HUMANA".equalsIgnoreCase(d.persona().tipo());
  }

  private static String primerNombre(String nombreCompleto) {
    String[] partes = nombreCompleto.split("\\s+");
    return partes.length > 0 ? partes[0] : nombreCompleto;
  }

  private static String normalizarCategoria(String categoria) {
    if (categoria == null) {
      return COLABORADOR;
    }
    String mayus = categoria.toUpperCase(Locale.ROOT);
    return switch (mayus) {
      case "SOSTENEDOR" -> SOSTENEDOR;
      case "TRANSFORMADOR" -> TRANSFORMADOR;
      default -> COLABORADOR;
    };
  }

  private static String formatearCuenta(int progreso, int objetivo, String descripcion) {
    String desc = descripcion != null ? descripcion.toLowerCase(Locale.ROOT) : "";
    String unidad = "";
    if (desc.contains("mes") || desc.contains("meses")) {
      unidad = objetivo == 1 ? "mes" : "meses";
    } else if (desc.contains("categoría") || desc.contains("categorias")) {
      unidad = objetivo == 1 ? "categoría" : "categorías";
    } else if (desc.contains("bien") || desc.contains("bienes")) {
      unidad = objetivo == 1 ? "bien" : "bienes";
    } else if (desc.contains("donación") || desc.contains("donaciones")) {
      unidad = objetivo == 1 ? "donación" : "donaciones";
    }
    return unidad.isEmpty()
        ? progreso + " de " + objetivo
        : progreso + " de " + objetivo + " " + unidad;
  }

  private static Insignia insignia(IncentivosApi.Insignia i, String fechaCompletada) {
    if (i == null) {
      return null;
    }
    String fecha =
        i.fechaObtenida() != null ? i.fechaObtenida().format(DIA_Y_ANIO) : fechaCompletada;
    return new Insignia(i.nombre(), i.descripcion(), i.imagenUrl(), i.visible(), fecha);
  }
}
