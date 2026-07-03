package grupo5.logistica.models.entities.camiones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.Objects;
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
    validarPatente(patente);
    validarCapacidad(capacidadVolumen);
    validarCapacidad(capacidadKG);
    validarCapacidad(altura);

    this.id = UUID.randomUUID();
    this.rutaId = null;
    this.patente = patente.trim();
    this.capacidadVolumen = capacidadVolumen;
    this.capacidadKG = capacidadKG;
    this.altura = altura;
    this.estado = EstadoCamion.DISPONIBLE;
  }

  public void asignarARuta(UUID rutaId) {
    if (Objects.isNull(rutaId)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (!estaDisponibleParaAsignar()) {
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
    this.rutaId = null;
  }

  public boolean estaDisponibleParaAsignar() {
    return this.estado == EstadoCamion.DISPONIBLE && Objects.isNull(this.rutaId);
  }

  private static void validarPatente(String patente) {
    if (Objects.isNull(patente)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (patente.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  private static void validarCapacidad(Float valor) {
    if (Objects.isNull(valor)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (valor <= 0) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
