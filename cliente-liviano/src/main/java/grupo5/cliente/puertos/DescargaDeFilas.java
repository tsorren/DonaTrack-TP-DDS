package grupo5.cliente.puertos;

import java.io.IOException;
import java.io.Writer;

/**
 * La descarga de las filas con error de una importación (R11-20). {@code archivo} es el nombre con
 * que se subió (null si Donaciones no lo guardó), para el nombre del archivo que baja; {@code
 * contenido} escribe el CSV mientras pide las filas de a tandas: ni el cliente ni el servicio
 * tienen la lista entera.
 */
public record DescargaDeFilas(String archivo, Contenido contenido) {

  /** Escribe el CSV en {@code salida}, tanda por tanda. */
  @FunctionalInterface
  public interface Contenido {
    void escribir(Writer salida) throws IOException;
  }
}
