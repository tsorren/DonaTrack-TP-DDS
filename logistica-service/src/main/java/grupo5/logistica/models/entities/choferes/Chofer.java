package grupo5.logistica.models.entities.choferes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Chofer implements AggregateRoot {
  private final UUID id;
  private final String nombre;
  private final String apellido;
  private String licencia;
  private String telefonoContacto;
  private EstadoChofer estado;
  private UUID rutaId;

  @Getter(AccessLevel.NONE)
  private final List<CambioEstadoChofer> historialEstado;

  public Chofer(String nombre, String apellido, String licencia, String telefonoContacto) {
    validarDatos(nombre, apellido, licencia, telefonoContacto);
    this.id = UUID.randomUUID();
    this.nombre = nombre;
    this.apellido = apellido;
    this.licencia = licencia;
    this.telefonoContacto = telefonoContacto;
    this.estado = EstadoChofer.DISPONIBLE;
    this.rutaId = null;
    this.historialEstado = new ArrayList<>();
  }

  public void actualizarLicencia(String nuevaLicencia) {
    if (nuevaLicencia == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (nuevaLicencia.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    this.licencia = nuevaLicencia;
  }

  public void actualizarTelefonoContacto(String nuevoTelefono) {
    if (nuevoTelefono == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (nuevoTelefono.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    this.telefonoContacto = nuevoTelefono;
  }

  /** Reserva al chofer para una ruta. Simétrico a {@code Camion#asignarARuta}. */
  public void asignarARuta(UUID rutaId) {
    if (Objects.isNull(rutaId)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    if (!estaDisponibleParaAsignar()) {
      throw new ValidationException(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA);
    }

    EstadoChofer anterior = this.estado;
    this.estado = EstadoChofer.EN_RUTA;
    this.rutaId = rutaId;
    registrarCambioEstado(anterior, EstadoChofer.EN_RUTA);
  }

  public void completarRuta() {
    if (estado != EstadoChofer.EN_RUTA) {
      throw new ValidationException(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA);
    }

    EstadoChofer anterior = this.estado;
    this.estado = EstadoChofer.DISPONIBLE;
    this.rutaId = null;
    registrarCambioEstado(anterior, EstadoChofer.DISPONIBLE);
  }

  public void habilitar() {
    if (this.estado != EstadoChofer.DESHABILITADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA);
    }

    EstadoChofer anterior = this.estado;
    this.estado = EstadoChofer.DISPONIBLE;
    registrarCambioEstado(anterior, EstadoChofer.DISPONIBLE);
  }

  public void deshabilitar() {
    if (this.estado != EstadoChofer.DISPONIBLE) {
      throw new ValidationException(ErrorCatalog.ESTADO_CHOFER_TRANSICION_INVALIDA);
    }

    EstadoChofer anterior = this.estado;
    this.estado = EstadoChofer.DESHABILITADO;
    this.rutaId = null;
    registrarCambioEstado(anterior, EstadoChofer.DESHABILITADO);
  }

  public boolean estaDisponibleParaAsignar() {
    return this.estado == EstadoChofer.DISPONIBLE && Objects.isNull(this.rutaId);
  }

  public List<CambioEstadoChofer> getHistorialEstado() {
    return List.copyOf(this.historialEstado);
  }

  private void registrarCambioEstado(EstadoChofer anterior, EstadoChofer nuevo) {
    this.historialEstado.add(
        new CambioEstadoChofer(anterior, nuevo, LocalDateTime.now(ZoneId.of("UTC"))));
  }

  private static void validarDatos(
      String nombre, String apellido, String licencia, String telefonoContacto) {
    if (nombre == null || apellido == null || licencia == null || telefonoContacto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (nombre.trim().isEmpty()
        || apellido.trim().isEmpty()
        || licencia.trim().isEmpty()
        || telefonoContacto.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
