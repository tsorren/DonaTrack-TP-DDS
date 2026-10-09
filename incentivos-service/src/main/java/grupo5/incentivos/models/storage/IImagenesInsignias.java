package grupo5.incentivos.models.storage;

/**
 * Puerto que traduce entre la referencia de imagen de una insignia (hoy {@code
 * /insignias/<archivo>.png}, definida por el dominio) y la URL pública con la que se expone.
 *
 * <p>En la base se persiste la <b>referencia</b> ({@link #referencia}); la URL pública se resuelve
 * al leer ({@link #urlPublica}). Así cambiar {@code public-base-url} corrige también los datos ya
 * guardados. Ambas operaciones son idempotentes y devuelven sin cambios cualquier valor que no
 * reconozcan.
 */
public interface IImagenesInsignias {

  /** Sin storage de imágenes (modo en memoria, tests): la referencia se usa tal cual. */
  IImagenesInsignias IDENTIDAD =
      new IImagenesInsignias() {
        @Override
        public String urlPublica(String referencia) {
          return referencia;
        }

        @Override
        public String referencia(String url) {
          return url;
        }
      };

  /** Referencia -> URL pública (lectura). */
  String urlPublica(String referencia);

  /** URL pública -> referencia (escritura). Una referencia ya relativa vuelve igual. */
  String referencia(String url);
}
