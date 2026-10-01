package grupo5.incentivos.models.entities.misiones;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

@Getter
public class MisionHabilDonador extends Mision {

  public MisionHabilDonador(CategoriaDonante categoria, Integer cantidadBienesObjetivo) {
    super(
        "Habil Donador",
        "Realiza una donacion con al menos " + cantidadBienesObjetivo + " bienes",
        categoria,
        cantidadBienesObjetivo);
  }

  private MisionHabilDonador(
      UUID id,
      Integer numeroMision,
      String nombre,
      String descripcion,
      CategoriaDonante categoria,
      Integer objetivo,
      Integer progresoActual,
      boolean completada,
      LocalDate fechaCompletada,
      Insignia insignia) {
    super(
        id,
        numeroMision,
        nombre,
        descripcion,
        categoria,
        objetivo,
        progresoActual,
        completada,
        fechaCompletada,
        insignia);
  }

  public static MisionHabilDonador reconstituir(
      UUID id,
      Integer numeroMision,
      String nombre,
      String descripcion,
      CategoriaDonante categoria,
      Integer objetivo,
      Integer progresoActual,
      boolean completada,
      LocalDate fechaCompletada,
      Insignia insignia) {
    return new MisionHabilDonador(
        id,
        numeroMision,
        nombre,
        descripcion,
        categoria,
        objetivo,
        progresoActual,
        completada,
        fechaCompletada,
        insignia);
  }

  @Override
  protected Integer calcularNuevoProgreso(DonanteIncentivos donante, EventoDonacion evento) {
    Integer cantidadActual = evento.getCantidadBienes() != null ? evento.getCantidadBienes() : 0;

    return Math.max(this.getProgresoActual(), cantidadActual);
  }
}
