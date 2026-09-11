package grupo5.donaciones.fixtures;

import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

public final class BienMother {

  public static final LocalDate VENCIMIENTO_FUTURO = LocalDate.of(2027, Month.DECEMBER, 31);

  private BienMother() {}

  public static Bien alimentoPerecedero(String descripcion) {
    return new Bien(descripcion, "http://fotos/alimento.png", VENCIMIENTO_FUTURO, null, 1.0, 0.002);
  }

  public static Bien prendaRopa(String descripcion) {
    return new Bien(descripcion, "http://fotos/ropa.png", null, Estado.USADO, 0.8, 0.005);
  }

  public static Bien mueble(String descripcion) {
    return new Bien(descripcion, "http://fotos/mueble.png", null, Estado.NUEVO, 10.0, 0.5);
  }

  public static Bien simple(String descripcion) {
    return new Bien(descripcion, "http://fotos/bien.png", null, null, 1.0, 0.01);
  }

  public static BienNormalizado aceptado(Bien bien, UUID subcategoriaId) {
    boolean conVencimiento = bien.fechaVencimiento() != null;
    boolean conEstado = bien.estado() != null;
    return new BienNormalizado(
        bien, subcategoriaId, 1.0, EstadoNormalizacion.ACEPTADO, conVencimiento, conEstado);
  }

  public static BienNormalizado pendienteRevision(
      Bien bien, UUID subcategoriaId, double confianza) {
    boolean conVencimiento = bien.fechaVencimiento() != null;
    boolean conEstado = bien.estado() != null;
    return new BienNormalizado(
        bien,
        subcategoriaId,
        confianza,
        EstadoNormalizacion.PENDIENTE_REVISION,
        conVencimiento,
        conEstado);
  }
}
