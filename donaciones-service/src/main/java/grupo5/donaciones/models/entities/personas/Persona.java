package grupo5.donaciones.models.entities.personas;

import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public abstract sealed class Persona implements Anonimizable, AggregateRoot
    permits Humana, Juridica {
  private final UUID id;
  private TipoDocumento tipoDocumento;
  private String documento;
  private Direccion direccion;

  @Getter(AccessLevel.NONE)
  private List<MedioDeContacto> mediosDeContacto;

  protected Persona() {
    this.id = UUID.randomUUID();
    this.mediosDeContacto = new ArrayList<>();
    this.mediosDeContacto.add(new Telefono());
  }

  /** Permite fijar el id explícitamente (uso: seeding de entidades con id conocido). */
  protected Persona(UUID id) {
    this.id = id;
    this.mediosDeContacto = new ArrayList<>();
    this.mediosDeContacto.add(new Telefono());
  }

  public List<MedioDeContacto> getMediosDeContacto() {
    return Collections.unmodifiableList(mediosDeContacto);
  }

  public void actualizarDocumento(TipoDocumento tipo, String documento) {
    this.tipoDocumento = tipo;
    this.documento = documento;
  }

  public void actualizarDireccion(Direccion direccion) {
    this.direccion = direccion;
  }

  public abstract TipoPersona getTipoPersona();

  public abstract String getNombreCompleto();

  public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
    if (medioDeContacto != null) {
      mediosDeContacto.add(medioDeContacto);
    }
  }

  public void limpiarMediosDeContacto() {
    this.mediosDeContacto.clear();
  }

  public void quitarMedioDeContacto(MedioDeContacto medioDeContacto) {
    mediosDeContacto.remove(medioDeContacto);
  }

  public void definirMedioDeContactoPredeterminado(MedioDeContacto medioDeContacto) {
    mediosDeContacto.stream()
        .filter(m -> m.getEsPredeterminado() != null && m.getEsPredeterminado())
        .findFirst()
        .ifPresent(m -> m.setEsPredeterminado(false));

    medioDeContacto.setEsPredeterminado(true);
  }

  /**
   * Determina si esta persona es duplicada de {@code otra}, comparando documento y medios de
   * contacto. Reemplaza al Strategy de Criterios (RF Oleada 1 - Persona): la comparación par a par
   * es una regla de dominio y vive en la entidad; recorrer el repositorio en busca de candidatos
   * sigue siendo responsabilidad de la capa de aplicación (ver ValidadorPersonaDuplicada).
   */
  public Boolean esDuplicadaDe(Persona otra) {
    if (otra == null) {
      return false;
    }
    return coincideEnDocumento(otra) || compartenMedioDeContacto(otra);
  }

  private boolean coincideEnDocumento(Persona otra) {
    return this.documento != null
        && !this.documento.isBlank()
        && this.documento.equals(otra.documento);
  }

  private boolean compartenMedioDeContacto(Persona otra) {
    if (this.mediosDeContacto.isEmpty() || otra.mediosDeContacto.isEmpty()) {
      return false;
    }
    for (MedioDeContacto medioPropio : this.mediosDeContacto) {
      for (MedioDeContacto medioAjeno : otra.mediosDeContacto) {
        if (coincidenMedios(medioPropio, medioAjeno)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean coincidenMedios(MedioDeContacto medio1, MedioDeContacto medio2) {
    // Si ambos son Correos
    if (medio1 instanceof Correo correo1 && medio2 instanceof Correo correo2) {
      String val1 = correo1.getDireccionCorreo();
      String val2 = correo2.getDireccionCorreo();
      if (val1 == null || val2 == null) return false;

      return val1.trim().equalsIgnoreCase(val2.trim());
    }

    // Si ambos son Telefonos (al usar instanceof Telefono, también incluye automáticamente a
    // WhatsApp)
    if (medio1 instanceof Telefono tel1 && medio2 instanceof Telefono tel2) {
      String val1 = tel1.obtenerNumeroCompleto();
      String val2 = tel2.obtenerNumeroCompleto();
      if (val1 == null || val2 == null) return false;

      String limpio1 = val1.replaceAll("\\D", "");
      String limpio2 = val2.replaceAll("\\D", "");

      return !limpio1.isEmpty()
          && !limpio2.isEmpty()
          && (limpio1.endsWith(limpio2) || limpio2.endsWith(limpio1));
    }

    return false;
  }
}
