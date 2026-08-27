package grupo5.incentivos.models.entities.donante;

import grupo5.common.events.AgregadoConEventos;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.EventoDonanteIncentivos;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import grupo5.incentivos.models.entities.metricas.Metricas;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.factory.MisionFactory;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;

@Getter
public class DonanteIncentivos extends AgregadoConEventos<EventoDonanteIncentivos> {

  private final UUID id;
  private final UUID idPersona;
  private final LocalDate fechaRegistro;
  private String nombre;
  private CategoriaDonante categoria;
  private List<CambioCategoria> historialCategorias;
  private List<Mision> misiones;
  private List<InsigniaGanada> insignias;
  private Metricas metricas;

  public DonanteIncentivos(
      UUID idDonante,
      UUID idPersona,
      String nombre,
      CategoriaDonante categoria,
      LocalDate fechaRegistro,
      List<CambioCategoria> historialCategorias,
      List<Mision> misiones,
      List<InsigniaGanada> insignias,
      Metricas metricas) {
    if (idPersona == null) {
      throw new ValidationException(ErrorCatalog.DONANTE_INCENTIVOS_ID_NULO);
    }
    if (idDonante == null) {
      throw new ValidationException(ErrorCatalog.DONANTE_INCENTIVOS_ID_NULO);
    }
    this.id = idDonante;
    this.idPersona = idPersona;
    this.nombre = nombre;
    this.categoria = categoria != null ? categoria : CategoriaDonante.COLABORADOR;
    this.fechaRegistro =
        fechaRegistro != null ? fechaRegistro : LocalDate.now(ZoneId.systemDefault());
    this.historialCategorias =
        historialCategorias != null ? new ArrayList<>(historialCategorias) : new ArrayList<>();
    this.misiones = misiones != null ? new ArrayList<>(misiones) : new ArrayList<>();
    this.insignias = insignias != null ? new ArrayList<>(insignias) : new ArrayList<>();
    this.metricas = metricas != null ? metricas : new Metricas();
  }

  public DonanteIncentivos(UUID idDonante, UUID idPersona, String nombre, List<Mision> misiones) {
    this(
        idDonante,
        idPersona,
        nombre,
        CategoriaDonante.COLABORADOR,
        LocalDate.now(ZoneId.systemDefault()),
        new ArrayList<>(),
        misiones,
        new ArrayList<>(),
        new Metricas());
  }

  public DonanteIncentivos(UUID idDonante, UUID idPersona, String nombre, LocalDate fechaRegistro) {
    this(
        idDonante,
        idPersona,
        nombre,
        CategoriaDonante.COLABORADOR,
        fechaRegistro,
        new ArrayList<>(),
        MisionFactory.crearMisionesEstandar(),
        new ArrayList<>(),
        new Metricas());
  }

  public DonanteIncentivos(UUID idDonante, UUID idPersona, String nombre) {
    this(idDonante, idPersona, nombre, MisionFactory.crearMisionesEstandar());
  }

  public void cambiarNombre(String nombre) {
    this.nombre = nombre;
  }

  public void registrarDonacion(EventoDonacion evento) {
    metricas.registrarDonacion(evento);
    evaluarMisionActiva(mision -> mision.evaluarProgreso(this, evento));
  }

  public void registrarDonacionExitosa(UUID organizacionId) {
    metricas.registrarDonacionExitosa(organizacionId);
    evaluarMisionActiva(mision -> mision.evaluarProgresoExitoso(this));
  }

  private void evaluarMisionActiva(Consumer<Mision> evaluador) {
    Mision activa = getMisionActiva();
    if (activa == null) {
      return;
    }
    evaluador.accept(activa);
    if (activa.isCompletada()) {
      completarMision(activa);
    }
  }

  private void completarMision(Mision mision) {
    registrarEvento(
        new MisionCompletada(
            this.id, this.idPersona, this.nombre, mision.getNombre(), mision.getInsignia()));
    ascender();
  }

  public void ascender() {
    boolean todasCompletadasEnCategoria =
        this.misiones.stream()
            .filter(m -> m.getCategoria() == this.categoria)
            .allMatch(Mision::isCompletada);

    if (todasCompletadasEnCategoria && this.categoria != CategoriaDonante.TRANSFORMADOR) {
      CategoriaDonante anterior = this.categoria;
      this.categoria = siguienteCategoria();
      this.historialCategorias.add(new CambioCategoria(anterior, this.categoria));
      registrarEvento(new AscensoDonante(this.id, this.idPersona, anterior, this.categoria));
    }
  }

  public void otorgarInsignia(Insignia insignia) {
    otorgarInsignia(insignia, LocalDate.now(ZoneId.systemDefault()));
  }

  public void otorgarInsignia(Insignia insignia, LocalDate fechaObtencion) {
    if (insignia == null) {
      throw new ValidationException(ErrorCatalog.INSIGNIA_NULA);
    }
    boolean yaExiste = this.insignias.stream().anyMatch(i -> i.nombre().equals(insignia.nombre()));
    if (yaExiste) {
      return;
    }
    LocalDate fechaFinal =
        fechaObtencion != null ? fechaObtencion : LocalDate.now(ZoneId.systemDefault());
    InsigniaGanada nuevaInsignia =
        new InsigniaGanada(
            insignia.nombre(), insignia.descripcion(), insignia.imagenUrl(), true, fechaFinal);
    this.insignias.add(nuevaInsignia);
  }

  public List<InsigniaGanada> insigniasVisibles() {
    return this.insignias.stream().filter(InsigniaGanada::visible).toList();
  }

  public void configurarVisibilidadInsignia(String nombre, boolean visible) {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.INSIGNIA_SIN_NOMBRE);
    }
    for (int i = 0; i < this.insignias.size(); i++) {
      InsigniaGanada current = this.insignias.get(i);
      if (current.nombre().equals(nombre)) {
        this.insignias.set(i, current.conVisibilidad(visible));
        return;
      }
    }
    throw new BusinessStateException(ErrorCatalog.INSIGNIA_NO_ENCONTRADA);
  }

  private CategoriaDonante siguienteCategoria() {
    return switch (this.categoria) {
      case COLABORADOR -> CategoriaDonante.SOSTENEDOR;
      case SOSTENEDOR -> CategoriaDonante.TRANSFORMADOR;
      case TRANSFORMADOR -> CategoriaDonante.TRANSFORMADOR;
    };
  }

  public Mision getMisionActiva() {
    return this.misiones.stream()
        .filter(m -> m.getCategoria() == this.categoria && !m.isCompletada())
        .min(
            Comparator.comparing(
                Mision::getNumeroMision, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElse(null);
  }

  public long misionesCompletadasEnMes(int anio, int mes) {
    return this.misiones.stream()
        .filter(m -> m.isCompletada() && m.fueCompletadaEnMes(anio, mes))
        .count();
  }

  public void verificarRachas(YearMonth mesActual) {
    this.misiones.stream()
        .filter(m -> !m.isCompletada())
        .forEach(m -> m.verificarVigencia(mesActual));
  }

  public int misionesCompletadas() {
    return (int) this.misiones.stream().filter(Mision::isCompletada).count();
  }

  public boolean tuvoActividadEnMes(YearMonth mes) {
    return this.metricas.donacionesEnMes(mes) > 0;
  }

  public long donacionesEnMes(YearMonth mes) {
    return this.metricas.donacionesEnMes(mes);
  }

  public LocalDate fechaUltimaActividad() {
    return this.metricas.getUltimaDonacion() != null
        ? this.metricas.getUltimaDonacion()
        : this.fechaRegistro;
  }

  public Map<YearMonth, Long> donacionesPorPeriodo() {
    return this.metricas.donacionesPorPeriodo();
  }
}
