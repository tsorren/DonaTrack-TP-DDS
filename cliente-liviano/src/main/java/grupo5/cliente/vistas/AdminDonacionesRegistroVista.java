package grupo5.cliente.vistas;

import java.util.List;

/**
 * Registrar donación del admin (RF-15), el paso "Quién dona": {@code donantes}, entre los que se
 * busca el elegido por su clave (fuera de la maqueta, solo el elegido, pedido por su id); {@code
 * encontrados}, los que coinciden con la búsqueda, como máximo 10 (R11-01); {@code coinciden},
 * cuántos coinciden en total.
 */
public record AdminDonacionesRegistroVista(
    List<Donante> donantes, List<Donante> encontrados, long coinciden) {

  public AdminDonacionesRegistroVista {
    donantes = List.copyOf(donantes);
    encontrados = List.copyOf(encontrados);
  }

  /** Un donante: {@code clave} va en la dirección y en el formulario; el documento, ya escrito. */
  public record Donante(String clave, String nombre, String documento) {}
}
