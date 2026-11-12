package grupo5.logistica.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.FotoRecepcionSubidaDTO;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.AlmacenFotosRecepcion;
import grupo5.logistica.services.impl.LimpiadorFotoRecepcion.Formato;
import grupo5.logistica.services.impl.LimpiadorFotoRecepcion.FotoLimpia;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Subida y lectura de las fotos de recepción (ADR-E6-08, HB-10). La subida valida y limpia todas
 * las fotos antes de guardar la primera, y no toca la entrega: la entidad confirma después con
 * {@code PATCH /api/entregas/{id}/estado} y las URLs estables que devuelve la subida.
 */
@Service
public class FotosRecepcionService {

  /** Vida de la URL firmada a la que redirige la URL estable (RNF-PRIV-03). */
  static final Duration VIGENCIA_URL_FIRMADA = Duration.ofMinutes(5);

  private final IEntregasRepository entregasRepository;
  private final AlmacenFotosRecepcion almacen;
  private final String urlPublicaBase;

  public FotosRecepcionService(
      IEntregasRepository entregasRepository,
      AlmacenFotosRecepcion almacen,
      @Value("${logistica.fotos.url-publica-base}") String urlPublicaBase) {
    this.entregasRepository = entregasRepository;
    this.almacen = almacen;
    this.urlPublicaBase =
        urlPublicaBase.endsWith("/")
            ? urlPublicaBase.substring(0, urlPublicaBase.length() - 1)
            : urlPublicaBase;
  }

  public List<FotoRecepcionSubidaDTO> subir(UUID idEntrega, List<byte[]> fotos) {
    if (fotos == null || fotos.isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    Entrega entrega =
        entregasRepository
            .findById(idEntrega)
            .orElseThrow(() -> new RecursoNoEncontradoException(idEntrega));
    if (entrega.getEstadoActual() == EstadoEntrega.ENTREGADA) {
      throw new BusinessStateException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    List<FotoLimpia> limpias = fotos.stream().map(LimpiadorFotoRecepcion::limpiar).toList();
    List<FotoRecepcionSubidaDTO> subidas = new ArrayList<>();
    for (FotoLimpia foto : limpias) {
      UUID fotoId = UUID.randomUUID();
      almacen.guardar(
          clave(idEntrega, fotoId, foto.formato()), foto.contenido(), foto.formato().tipoContenido);
      subidas.add(
          new FotoRecepcionSubidaDTO(
              fotoId, urlPublicaBase + "/api/entregas/" + idEntrega + "/fotos/" + fotoId));
    }
    return subidas;
  }

  /** URL firmada de 5 minutos de la foto, para la redirección de la URL estable. */
  public String urlFirmada(UUID idEntrega, UUID fotoId) {
    for (Formato formato : Formato.values()) {
      String clave = clave(idEntrega, fotoId, formato);
      if (almacen.existe(clave)) {
        return almacen.urlFirmada(clave, VIGENCIA_URL_FIRMADA);
      }
    }
    throw new RecursoNoEncontradoException(fotoId);
  }

  /** {@code entregas/<idEntrega>/<fotoId>.<ext>}: los dos ids son UUID, sin datos del cliente. */
  static String clave(UUID idEntrega, UUID fotoId, Formato formato) {
    return "entregas/" + idEntrega + "/" + fotoId + "." + formato.extension;
  }
}
