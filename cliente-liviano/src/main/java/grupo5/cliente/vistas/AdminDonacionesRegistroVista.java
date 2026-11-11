package grupo5.cliente.vistas;

import java.util.List;

/**
 * Registrar donación del admin (RF-15), el paso "Quién dona": los donantes que se pueden elegir y
 * los que coinciden con la búsqueda.
 */
public record AdminDonacionesRegistroVista(List<Donante> donantes, List<Donante> encontrados) {

  public AdminDonacionesRegistroVista {
    donantes = List.copyOf(donantes);
    encontrados = List.copyOf(encontrados);
  }

  /** Un donante: {@code clave} va en la dirección y en el formulario; el documento, ya escrito. */
  public record Donante(String clave, String nombre, String documento) {}
}
