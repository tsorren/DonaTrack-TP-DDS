package grupo5.logistica.models.entities.camiones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Camion implements AggregateRoot {
  private final UUID id;
  private UUID rutaId;
  private final String patente;
  private final Float capacidadVolumen;
  private final Float altura;
  private final Float capacidadKG;
  private EstadoCamion estado;

  public Camion(String patente, Float capacidadVolumen, Float capacidadKG, Float altura) {
    validarDatos(patente, capacidadVolumen, capacidadKG, altura);
    this.id = UUID.randomUUID();
    this.rutaId = null;
    this.patente = patente;
    this.capacidadVolumen = capacidadVolumen;
    this.capacidadKG = capacidadKG;
    this.altura = altura;
    this.estado = EstadoCamion.DISPONIBLE;
  }

  private static void validarDatos(
      String patente, Float capacidadVolumen, Float capacidadKG, Float altura) {
    if (patente == null || capacidadVolumen == null || capacidadKG == null || altura == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (patente.trim().isEmpty() || capacidadVolumen <= 0 || capacidadKG <= 0 || altura <= 0) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  public void asignarARuta(UUID rutaId) {
    if (rutaId == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (estado != EstadoCamion.DISPONIBLE) {
      throw new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA);
    }

    this.estado = EstadoCamion.EN_RUTA;
    this.rutaId = rutaId;
  }

  public void completarRuta() {
    if (estado != EstadoCamion.EN_RUTA) {
      throw new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA);
    }

    this.estado = EstadoCamion.DISPONIBLE;
    this.rutaId = null;
  }

  public void habilitar() {
    if (this.estado != EstadoCamion.DESHABILITADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA);
    }

    this.estado = EstadoCamion.DISPONIBLE;
  }

  public void deshabilitar() {
    if (this.estado != EstadoCamion.DISPONIBLE) {
      throw new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA);
    }

    this.estado = EstadoCamion.DESHABILITADO;
  }

  public boolean estaDisponibleParaAsignar() {
    return this.estado == EstadoCamion.DISPONIBLE;
  }

  @Override
  public UUID getId() {
    return this.id;
  }
}
