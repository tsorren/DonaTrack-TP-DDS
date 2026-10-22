package grupo5.donaciones.models.entities.personas;

import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
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

  /**
   * Reconstituye una persona ya persistida: conserva id y medios tal cual, sin agregar el teléfono
   * vacío que trae una persona nueva.
   */
  protected Persona(
      UUID id,
      TipoDocumento tipoDocumento,
      String documento,
      Direccion direccion,
      List<MedioDeContacto> mediosDeContacto) {
    this.id = id;
    this.tipoDocumento = tipoDocumento;
    this.documento = documento;
    this.direccion = direccion;
    this.mediosDeContacto = new ArrayList<>(mediosDeContacto);
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
   * Determina si esta persona es duplicada de {@code otra}. Reemplaza al Strategy de Criterios (RF
   * Oleada 1 - Persona): la comparación par a par es una regla de dominio y vive en la entidad;
   * buscar los candidatos en el repositorio es responsabilidad de la capa de aplicación (ver
   * ValidadorPersonaDuplicada).
   *
   * <p>Manda el documento (tipo y número). Si no coincide, compartir un correo alcanza solo cuando
   * a alguna de las dos le falta documento: con documentos distintos es un conflicto (ver {@link
   * #entraEnConflictoCon}). El teléfono no cuenta, porque se comparte seguido (oficina, casa).
   */
  public Boolean esDuplicadaDe(Persona otra) {
    if (otra == null) {
      return false;
    }
    if (coincideEnDocumento(otra)) {
      return true;
    }
    return compartenCorreo(otra) && !ambasTienenDocumento(otra);
  }

  /**
   * Comparten un correo pero tienen documentos distintos: no se puede decidir si son la misma
   * persona (por ejemplo, dos jurídicas que usan el mail de la misma representante).
   */
  public boolean entraEnConflictoCon(Persona otra) {
    return otra != null
        && compartenCorreo(otra)
        && ambasTienenDocumento(otra)
        && !coincideEnDocumento(otra);
  }

  /** Direcciones de correo de la persona, tal como fueron cargadas. */
  public List<String> direccionesDeCorreo() {
    return correos().map(Correo::getDireccionCorreo).filter(Objects::nonNull).toList();
  }

  // Si a alguna le falta el tipo (por ejemplo, una fila del CSV sin TipoDoc), alcanza el número
  private boolean coincideEnDocumento(Persona otra) {
    boolean tiposCompatibles =
        this.tipoDocumento == null
            || otra.tipoDocumento == null
            || this.tipoDocumento == otra.tipoDocumento;
    return tieneDocumento()
        && tiposCompatibles
        && this.documento.trim().equals(otra.documento == null ? null : otra.documento.trim());
  }

  private boolean tieneDocumento() {
    return this.documento != null && !this.documento.isBlank();
  }

  private boolean ambasTienenDocumento(Persona otra) {
    return this.tieneDocumento() && otra.tieneDocumento();
  }

  private boolean compartenCorreo(Persona otra) {
    return correos()
        .anyMatch(propio -> otra.correos().anyMatch(ajeno -> coincidenMedios(propio, ajeno)));
  }

  private Stream<Correo> correos() {
    return this.mediosDeContacto.stream().filter(Correo.class::isInstance).map(Correo.class::cast);
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
