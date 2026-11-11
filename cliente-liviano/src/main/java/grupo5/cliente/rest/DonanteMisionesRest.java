package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;

import grupo5.cliente.puertos.DonanteMisionesPuerto;
import grupo5.cliente.rest.IncentivosApi.Ascenso;
import grupo5.cliente.rest.IncentivosApi.PerfilDeDonante;
import grupo5.cliente.vistas.DonanteMisionesVista;
import grupo5.cliente.vistas.DonanteMisionesVista.Categoria;
import grupo5.cliente.vistas.DonanteMisionesVista.Historia;
import grupo5.cliente.vistas.DonanteMisionesVista.Hito;
import grupo5.cliente.vistas.DonanteMisionesVista.Mision;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Misiones e insignias con los datos de Incentivos (perfil, misiones y ascensos); que la cuenta sea
 * una persona lo dice Donaciones (las empresas no tienen misiones, RF-07). Incentivos da el avance
 * ({@code 12 de 50}) sin su unidad, y no da qué donación completó cada misión: la huerta cuenta lo
 * que hay, sin la historia de cada planta.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.misiones", havingValue = "rest")
class DonanteMisionesRest implements DonanteMisionesPuerto {

  private static final String NOTA =
      "Todavía no mostramos con qué donación completaste cada misión.";
  private static final String HUMANA = "HUMANA";
  private static final String COMPLETA = "completa";
  private static final String CURSO = "curso";
  private static final String TU_CATEGORIA = "tu categoría";
  private static final String SE_ABRE_DESPUES = "se abre después";
  private static final String TE_SUMASTE = "Te sumaste a DonaTrack";
  private static final DateTimeFormatter ACTUALIZADO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Comun.ES_AR);

  private final IncentivosApi incentivos;
  private final DonacionesApi donaciones;
  private final Clock reloj;

  DonanteMisionesRest(IncentivosApi incentivos, DonacionesApi donaciones, Clock reloj) {
    this.incentivos = incentivos;
    this.donaciones = donaciones;
    this.reloj = reloj.withZone(ARGENTINA);
  }

  @Override
  public DonanteMisionesVista misiones(UUID donante) {
    DonacionesApi.Persona persona = donaciones.donante(donante).persona();
    if (persona == null || !HUMANA.equals(persona.tipo())) {
      return new DonanteMisionesVista(
          false, false, null, null, null, 0, "", List.of(), List.of(), null, null);
    }
    PerfilDeDonante perfil = incentivos.donante(donante);
    List<Mision> misiones = misiones(incentivos.misiones(donante));
    int completadas = (int) misiones.stream().filter(m -> COMPLETA.equals(m.estado())).count();
    String categoria = capitalizada(perfil.categoria());
    return new DonanteMisionesVista(
        true,
        completadas == 0,
        saludo(perfil.nombre()),
        categoria,
        desde(perfil, categoria, incentivos.ascensos(donante)),
        completadas,
        ACTUALIZADO.format(LocalDateTime.now(reloj)),
        misiones,
        categorias(misiones),
        bienvenida(perfil.fechaRegistro()),
        NOTA);
  }

  /**
   * Incentivos evalúa solo la primera sin completar ({@code curso}); las que le siguen en su
   * categoría esperan y las de las categorías que vienen no se abren ({@code cerrada}).
   */
  private static List<Mision> misiones(List<IncentivosApi.Mision> recibidas) {
    int enCurso = -1;
    for (int i = 0; i < recibidas.size() && enCurso < 0; i++) {
      if (!Boolean.TRUE.equals(recibidas.get(i).completada())) {
        enCurso = i;
      }
    }
    String categoriaEnCurso = enCurso < 0 ? null : capitalizada(recibidas.get(enCurso).categoria());
    List<Mision> misiones = new ArrayList<>();
    for (int i = 0; i < recibidas.size(); i++) {
      IncentivosApi.Mision m = recibidas.get(i);
      String categoria = capitalizada(m.categoria());
      if (Boolean.TRUE.equals(m.completada())) {
        String fecha = m.fechaCompletada() == null ? null : DIA_Y_ANIO.format(m.fechaCompletada());
        misiones.add(mision(categoria, m, COMPLETA, fecha, null));
      } else if (i == enCurso) {
        int progreso = Objects.requireNonNullElse(m.progresoActual(), 0);
        int objetivo = Objects.requireNonNullElse(m.objetivo(), 0);
        misiones.add(
            new Mision(
                categoria,
                nombre(m),
                pide(m),
                CURSO,
                null,
                progreso,
                objetivo,
                progreso + " de " + objetivo,
                null,
                null,
                null));
      } else if (categoria.equals(categoriaEnCurso)) {
        misiones.add(mision(categoria, m, "espera", null, nombre(recibidas.get(i - 1))));
      } else {
        misiones.add(mision(categoria, m, "cerrada", null, null));
      }
    }
    return misiones;
  }

  /**
   * El nombre que ve la persona es el de su insignia ("Racha Inicial"): el de la misión ("Racha")
   * es interno y se repite entre categorías.
   */
  private static String nombre(IncentivosApi.Mision m) {
    return m.insignia() != null && m.insignia().nombre() != null
        ? m.insignia().nombre()
        : m.nombre();
  }

  /** Qué pide, con las palabras de la insignia ("3 meses consecutivos donando"). */
  private static String pide(IncentivosApi.Mision m) {
    return m.insignia() != null && m.insignia().descripcion() != null
        ? m.insignia().descripcion()
        : m.descripcion();
  }

  private static Mision mision(
      String categoria, IncentivosApi.Mision m, String estado, String fecha, String espera) {
    return new Mision(
        categoria, nombre(m), pide(m), estado, fecha, null, null, null, null, espera, null);
  }

  /**
   * Las categorías en el orden de las misiones: las completas, la actual y las que se abren
   * después.
   */
  private static List<Categoria> categorias(List<Mision> misiones) {
    List<String> nombres = misiones.stream().map(Mision::categoria).distinct().toList();
    String actual =
        misiones.stream()
            .filter(m -> CURSO.equals(m.estado()))
            .map(Mision::categoria)
            .findFirst()
            .orElse(null);
    List<Categoria> categorias = new ArrayList<>();
    for (int i = 0; i < nombres.size(); i++) {
      String nombre = nombres.get(i);
      boolean completa =
          misiones.stream()
              .filter(m -> m.categoria().equals(nombre))
              .allMatch(m -> COMPLETA.equals(m.estado()));
      if (completa) {
        categorias.add(new Categoria(nombre, COMPLETA, null, null));
      } else if (nombre.equals(actual)) {
        categorias.add(
            new Categoria(
                nombre, TU_CATEGORIA, i + 1 < nombres.size() ? nombres.get(i + 1) : null, null));
      } else {
        categorias.add(
            new Categoria(nombre, SE_ABRE_DESPUES, null, i == 0 ? null : nombres.get(i - 1)));
      }
    }
    return categorias;
  }

  /** Desde cuándo es de su categoría: el último ascenso a ella o, si nunca ascendió, su alta. */
  private static String desde(PerfilDeDonante perfil, String categoria, List<Ascenso> ascensos) {
    LocalDate fecha =
        ascensos.stream()
            .filter(a -> a.fecha() != null && categoria.equalsIgnoreCase(a.categoriaNueva()))
            .map(Ascenso::fecha)
            .max(LocalDate::compareTo)
            .orElse(perfil.fechaRegistro());
    return fecha == null ? null : DIA_Y_ANIO.format(fecha);
  }

  /** La primera planta llega con la cuenta: es lo único que Incentivos sabe de ella. */
  private static Historia bienvenida(LocalDate registro) {
    List<Hito> hitos =
        registro == null
            ? List.of()
            : List.of(new Hito(TE_SUMASTE, DIA_Y_ANIO.format(registro), true));
    return new Historia(
        TE_SUMASTE + ". Esta planta llega con tu cuenta.", hitos, null, null, null, null);
  }

  /** El primer nombre, o null si no lo hay. */
  private static String saludo(String nombre) {
    if (nombre == null || nombre.isBlank()) {
      return null;
    }
    return nombre.strip().split("\\s+")[0];
  }

  /** "SOSTENEDOR" y "sostenedor" quedan "Sostenedor". */
  private static String capitalizada(String valor) {
    if (valor == null || valor.isEmpty()) {
      return "";
    }
    return valor.substring(0, 1).toUpperCase(Locale.ROOT)
        + valor.substring(1).toLowerCase(Locale.ROOT);
  }
}
