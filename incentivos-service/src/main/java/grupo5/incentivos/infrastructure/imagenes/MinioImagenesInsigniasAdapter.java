package grupo5.incentivos.infrastructure.imagenes;

import grupo5.incentivos.models.storage.IImagenesInsignias;

/**
 * Resuelve las referencias {@code /insignias/<archivo>} a la URL pública del objeto en MinIO:
 * {@code <public-base-url>/<bucket>/<archivo>}. No usa la red: la URL pública es determinística.
 */
public class MinioImagenesInsigniasAdapter implements IImagenesInsignias {

  static final String PREFIJO_REFERENCIA = "/insignias/";

  private final String urlBaseDelBucket;

  public MinioImagenesInsigniasAdapter(String publicBaseUrl, String bucket) {
    String base =
        publicBaseUrl.endsWith("/")
            ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
            : publicBaseUrl;
    this.urlBaseDelBucket = base + "/" + bucket + "/";
  }

  @Override
  public String urlPublica(String referencia) {
    if (referencia == null || !referencia.startsWith(PREFIJO_REFERENCIA)) {
      return referencia;
    }
    return urlBaseDelBucket + referencia.substring(PREFIJO_REFERENCIA.length());
  }

  @Override
  public String referencia(String url) {
    if (url == null || !url.startsWith(urlBaseDelBucket)) {
      return url;
    }
    return PREFIJO_REFERENCIA + url.substring(urlBaseDelBucket.length());
  }
}
