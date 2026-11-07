package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.config.AlmacenamientoProperties;
import grupo5.donaciones.dto.fotos.SolicitudSubidaFotoInputDTO;
import grupo5.donaciones.dto.fotos.SubidaFotoOutputDTO;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.ports.StoragePort.Metadatos;
import grupo5.donaciones.models.ports.StoragePort.Operacion;
import grupo5.donaciones.services.IFotosBienesService;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class FotosBienesService implements IFotosBienesService {

  static final String PREFIJO_REFERENCIA = "/fotos-bienes/";
  private static final String METODO_SUBIDA = "PUT";

  /** Solo claves generadas por {@link #solicitarSubida}: UUID y una extensión conocida. */
  private static final Pattern CLAVE_FOTO =
      Pattern.compile(
          "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");

  private static final Map<String, String> EXTENSIONES =
      Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");

  private final StoragePort storage;
  private final AlmacenamientoProperties almacenamiento;
  private final Clock clock;

  public FotosBienesService(
      StoragePort storage, AlmacenamientoProperties almacenamiento, Clock clock) {
    this.storage = storage;
    this.almacenamiento = almacenamiento;
    this.clock = clock;
  }

  @Override
  public SubidaFotoOutputDTO solicitarSubida(SolicitudSubidaFotoInputDTO solicitud) {
    String tipo = normalizar(solicitud.tipoContenido());
    if (!esTipoPermitido(tipo)) {
      throw new ValidationException(ErrorCatalog.FOTO_BIEN_TIPO_NO_PERMITIDO);
    }
    String clave = UUID.randomUUID() + EXTENSIONES.get(tipo);
    String url =
        storage.urlTemporal(
            almacenamiento.bucketFotos(), clave, almacenamiento.vigenciaSubida(), Operacion.SUBIR);
    Instant expiraEn = clock.instant().plus(almacenamiento.vigenciaSubida());
    return new SubidaFotoOutputDTO(url, METODO_SUBIDA, tipo, PREFIJO_REFERENCIA + clave, expiraEn);
  }

  @Override
  public void validarFoto(String fotoUrl) {
    if (fotoUrl == null || !fotoUrl.startsWith(PREFIJO_REFERENCIA)) {
      return;
    }
    String clave = fotoUrl.substring(PREFIJO_REFERENCIA.length());
    if (!CLAVE_FOTO.matcher(clave).matches()) {
      throw new ValidationException(ErrorCatalog.FOTO_BIEN_REFERENCIA_INVALIDA);
    }
    Metadatos metadatos =
        storage
            .metadatos(almacenamiento.bucketFotos(), clave)
            .orElseThrow(() -> new ValidationException(ErrorCatalog.FOTO_BIEN_NO_ENCONTRADA));
    if (!esTipoPermitido(normalizar(metadatos.tipoContenido()))) {
      throw new ValidationException(ErrorCatalog.FOTO_BIEN_TIPO_NO_PERMITIDO);
    }
    if (metadatos.tamanio() > almacenamiento.fotoTamanioMaximo().toBytes()) {
      throw new ValidationException(ErrorCatalog.FOTO_BIEN_TAMANIO_EXCEDIDO);
    }
  }

  @Override
  public String urlParaMostrar(String fotoUrl) {
    if (fotoUrl == null || !fotoUrl.startsWith(PREFIJO_REFERENCIA)) {
      return fotoUrl;
    }
    String clave = fotoUrl.substring(PREFIJO_REFERENCIA.length());
    if (!CLAVE_FOTO.matcher(clave).matches()) {
      return fotoUrl;
    }
    return storage.urlTemporal(
        almacenamiento.bucketFotos(),
        clave,
        almacenamiento.vigenciaDescarga(),
        Operacion.DESCARGAR);
  }

  private boolean esTipoPermitido(String tipo) {
    return tipo != null
        && EXTENSIONES.containsKey(tipo)
        && almacenamiento.fotoTiposPermitidos().contains(tipo);
  }

  /** Sin parámetros ({@code image/jpeg; charset=...}) y en minúsculas. */
  private static String normalizar(String tipoContenido) {
    if (tipoContenido == null) {
      return null;
    }
    int puntoYComa = tipoContenido.indexOf(';');
    String tipo = puntoYComa >= 0 ? tipoContenido.substring(0, puntoYComa) : tipoContenido;
    return tipo.trim().toLowerCase(Locale.ROOT);
  }
}
