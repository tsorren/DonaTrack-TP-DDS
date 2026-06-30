package grupo5.logistica.models.entities.camiones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Camion {
  private final UUID id;
  private final String patente;
  private final Float capacidadVolumen;
  private final Float altura;
  private final Float capacidadKG;
  private EstadoCamion estado;

  public Camion(String patente, Float capacidadVolumen, Float capacidadKG, Float altura) {
    this.id = UUID.randomUUID();
    this.patente = patente;
    this.capacidadVolumen = capacidadVolumen;
    this.capacidadKG = capacidadKG;
    this.altura = altura;
    this.estado = EstadoCamion.DISPONIBLE;
  }

  public void mandarAMantenimiento() {
    this.estado = EstadoCamion.EN_MANTENIMIENTO;
  }

  public void salirDeMantenimientoExitoso() {
    if (this.estado != EstadoCamion.EN_MANTENIMIENTO) {
      throw new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA);
    }
    this.estado = EstadoCamion.DISPONIBLE;
  }

  public void marcarComoInactivoPorFalla() {
    if (this.estado != EstadoCamion.EN_MANTENIMIENTO) {
      throw new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA);
    }
    this.estado = EstadoCamion.INACTIVO;
  }
}
