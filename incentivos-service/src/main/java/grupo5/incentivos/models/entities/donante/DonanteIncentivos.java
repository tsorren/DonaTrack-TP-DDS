package grupo5.incentivos.models.entities.donante;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.incentivos.models.entities.donante.insignias.Insignia;
import grupo5.incentivos.models.entities.donante.metricas.Metricas;
import grupo5.incentivos.models.entities.donante.misiones.Mision;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    this(idDonante, idPersona, nombre, new ArrayList<>());
  }

  public void registrarDonacion(EventoDonacion evento) {
    metricas.registrarDonacion(evento);

    this.misiones.stream()
        .filter(m -> m.getCategoria() == this.categoria && !m.isCompletada())
        .findFirst()
        .ifPresent(m -> m.evaluarProgreso(this, evento));
  }

  public void registrarDonacionExitosa(UUID organizacionId) {
    metricas.registrarDonacionExitosa(organizacionId);

    this.misiones.stream()
        .filter(m -> m.getCategoria() == this.categoria && !m.isCompletada())
        .findFirst()
        .ifPresent(m -> m.evaluarProgresoExitoso(this));
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

  public boolean intentarAscenso() {
    boolean todasCompletadasEnCategoria =
        this.misiones.stream()
            .filter(m -> m.getCategoria() == this.categoria)
            .allMatch(Mision::isCompletada);

    if (todasCompletadasEnCategoria && this.categoria != CategoriaDonante.TRANSFORMADOR) {
      CategoriaDonante anterior = this.categoria;
      this.categoria = siguienteCategoria();
      this.historialCategorias.add(new CambioCategoria(anterior, this.categoria));
      return true;
    }
    return false;
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
