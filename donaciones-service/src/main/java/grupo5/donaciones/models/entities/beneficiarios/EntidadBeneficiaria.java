package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.UUID;

/**
 * Rol de entidad beneficiaria. Su identidad es la de la {@link Juridica}: {@code id == juridicaId}.
 */
public class EntidadBeneficiaria implements Anonimizable, AggregateRoot {
  private final UUID id;
  private boolean activo;

  public EntidadBeneficiaria(UUID juridicaId) {
    if (juridicaId == null) {
      throw new ValidationException(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA);
    }
    this.id = juridicaId;
    this.activo = true;
  }

  /**
   * Una entidad beneficiaria es una organización sin fines de lucro: tiene que ser jurídica, no
   * puede ser una empresa y necesita dirección (la entrega se hace allí).
   */
  public static Juridica validarApta(Persona persona) {
    if (!(persona instanceof Juridica juridica)) {
      throw new ValidationException(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA);
    }
    validarRequisitos(juridica.getTipo(), juridica.getDireccion() != null);
    return juridica;
  }

  /**
   * Reglas de una entidad beneficiaria expresadas sobre los datos resultantes: así se pueden
   * validar antes de modificar la jurídica (ver {@code PersonasService.actualizarPersona}).
   */
  public static void validarRequisitos(TipoJuridico tipo, boolean tieneDireccion) {
    if (tipo == TipoJuridico.EMPRESA) {
      throw new ValidationException(ErrorCatalog.ENTIDAD_BENEFICIARIA_TIPO_INVALIDO);
    }
    if (!tieneDireccion) {
      throw new ValidationException(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_DIRECCION);
    }
  }

  public UUID juridicaId() {
    return this.id;
  }

  @Override
  public UUID getId() {
    return this.id;
  }

  public boolean estaActivo() {
    return this.activo;
  }

  /** Devuelve {@code true} solo si hubo transición de activa a inactiva. */
  public boolean darDeBaja() {
    boolean estabaActiva = this.activo;
    this.activo = false;
    return estabaActiva;
  }

  /** Devuelve {@code true} solo si hubo transición de inactiva a activa. */
  public boolean reactivar() {
    boolean estabaInactiva = !this.activo;
    this.activo = true;
    return estabaInactiva;
  }

  @Override
  public void anonimizar() {
    // Coordinado a nivel de servicio de aplicación (PersonasService)
  }
}
