package grupo5.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class DonacionIndependiente {
  private String descripcion;
  private List<ItemDonacion> items = new ArrayList<>();
  private EstadoDonacion estadoActual;
  private final List<CambioEstado> historial = new ArrayList<>();
  private final LocalDateTime fechaRegistro;

  public DonacionIndependiente(String descripcion) {

    if (descripcion == null || descripcion.trim().isEmpty()) {
      throw new IllegalArgumentException(
              "La descripción de la donación independiente no puede estar vacía.");
    }

    this.descripcion = descripcion;
    this.fechaRegistro = LocalDateTime.now();
    this.estadoActual = new EnDeposito();
  }

  public void agregarItem(ItemDonacion item) {
    if (item == null) {
      throw new IllegalArgumentException("El ítem a agregar no puede ser nulo.");
    }
    this.items.add(item);
  }

  // Lanzar excepcion si el item no esta en la lista
  public void quitarItem(ItemDonacion bien) {
    if (!this.items.contains(bien)) {
      throw new IllegalArgumentException(
              "El ítem que intenta quitar no pertenece a esta donación.");
    }
    this.items.remove(bien);
  }

  public int getCantidad() {
    return this.items.stream().mapToInt(ItemDonacion::getCantidad).sum();
  }

  // Métodos de negocio — delegan al estado actual
  public void registrar() {
    estadoActual.registrar(this);
  }

  public void asignar() {
    estadoActual.asignar(this);
  }

  public void planificarRuta() {
    estadoActual.planificarRuta(this);
  }

  public void iniciarRecorrido() {
    estadoActual.iniciarRecorrido(this);
  }

  public void confirmarEntrega() {
    estadoActual.confirmarEntrega(this);
  }

  public void registrarFalla(String justificacion) {
    estadoActual.registrarFalla(this, justificacion);
  }

  public void retornar() {
    estadoActual.retornar(this);
  }

  public void vencer() {
    estadoActual.vencer(this);
  }

  // Llamado únicamente por los estados concretos
  void cambiarEstado(EstadoDonacion nuevoEstado, String justificacion) {
    CambioEstado cambio = new CambioEstado(this.estadoActual, nuevoEstado, justificacion);
    this.historial.add(cambio);
    this.estadoActual = nuevoEstado;
  }

  public List<CambioEstado> getHistorial() {
    return Collections.unmodifiableList(historial);
  }
}