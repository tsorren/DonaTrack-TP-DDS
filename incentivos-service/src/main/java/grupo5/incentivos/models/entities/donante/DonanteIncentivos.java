package grupo5.incentivos.models.entities.donante;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.metricas.Metricas;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.factory.MisionFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;

@Getter
public class DonanteIncentivos implements AggregateRoot {

  private final UUID id;
  private final UUID idPersona;
  private String nombre;
  private CategoriaDonante categoria;
  private List<CambioCategoria> historialCategorias;
  private List<Mision> misiones;
  private List<Insignia> insignias;
  private Metricas metricas;

  private final List<Object> eventosDominio = new ArrayList<>();

  public DonanteIncentivos(UUID idDonante, UUID idPersona, String nombre, List<Mision> misiones) {
    if (idPersona == null) {
      throw new ValidationException(ErrorCatalog.DONANTE_INCENTIVOS_ID_NULO);
    }

    if (idDonante == null) {
      throw new ValidationException(ErrorCatalog.DONANTE_INCENTIVOS_ID_NULO);
    }
    this.id = idDonante;
    this.idPersona = idPersona;
    this.nombre = nombre;
    this.categoria = CategoriaDonante.COLABORADOR;
    this.historialCategorias = new ArrayList<>();
    this.misiones = misiones != null ? new ArrayList<>(misiones) : new ArrayList<>();
    this.insignias = new ArrayList<>();
    this.metricas = new Metricas();
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

  private void registrarEvento(Object evento) {
    this.eventosDominio.add(evento);
  }

  public List<Object> pullEventosDominio() {
    List<Object> copia = List.copyOf(this.eventosDominio);
    this.eventosDominio.clear();
    return copia;
  }

  public void otorgarInsignia(Insignia insignia) {
    if (insignia == null) {
      throw new ValidationException(ErrorCatalog.INSIGNIA_NULA);
    }
    Insignia nuevaInsignia =
        new Insignia(
            insignia.nombre(),
            insignia.descripcion(),
            insignia.imagenUrl(),
            insignia.visible(),
            LocalDate.now(ZoneId.systemDefault()));
    this.insignias.add(nuevaInsignia);
  }

  public void configurarVisibilidadInsignia(String nombre, boolean visible) {
    for (int i = 0; i < this.insignias.size(); i++) {
      Insignia current = this.insignias.get(i);
      if (current.nombre().equals(nombre)) {
        this.insignias.set(
            i,
            new Insignia(
                current.nombre(),
                current.descripcion(),
                current.imagenUrl(),
                visible,
                current.fechaObtenida()));
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
        .findFirst()
        .orElse(null);
  }

  public long misionesCompletadasEnMes(int anio, int mes) {
    return this.misiones.stream()
        .filter(m -> m.isCompletada() && m.fueCompletadaEnMes(anio, mes))
        .count();
  }
}
