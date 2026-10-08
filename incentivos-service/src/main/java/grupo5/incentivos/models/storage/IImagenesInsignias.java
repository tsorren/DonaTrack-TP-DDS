package grupo5.incentivos.models.storage;

/**
 * Puerto que traduce la referencia de imagen de una insignia (hoy {@code /insignias/<archivo>.png},
 * definida por el dominio) a la URL pública con la que se persiste y se expone.
 *
 * <p>Es idempotente: una URL que ya es absoluta (o cualquier valor que no sea una referencia de
 * insignia) vuelve sin cambios.
 */
@FunctionalInterface
public interface IImagenesInsignias {

  /** Sin storage de imágenes (modo en memoria, tests): la referencia se usa tal cual. */
  IImagenesInsignias IDENTIDAD = referencia -> referencia;

  String urlPublica(String referencia);
}
