package grupo5.donaciones.services;

import grupo5.donaciones.dto.fotos.SolicitudSubidaFotoInputDTO;
import grupo5.donaciones.dto.fotos.SubidaFotoOutputDTO;

/**
 * Fotos de bienes en el almacenamiento de objetos. En {@code fotoUrl} se guarda una
 * <b>referencia</b> ({@code /fotos-bienes/<uuid>.<ext>}); la URL para verla se firma al leer, así
 * nunca se persiste una URL que vence. Una URL externa (cargada antes de este cambio, o de otro
 * origen) pasa sin cambios en ambos sentidos.
 */
public interface IFotosBienesService {

  /** Genera la clave de la foto y una URL temporal para subirla. */
  SubidaFotoOutputDTO solicitarSubida(SolicitudSubidaFotoInputDTO solicitud);

  /**
   * Si {@code fotoUrl} es una referencia, verifica que la foto se haya subido y respete tipo y
   * tamaño. {@code null}, vacío o una URL externa no se validan.
   */
  void validarFoto(String fotoUrl);

  /** Referencia -> URL temporal de descarga. Cualquier otro valor vuelve sin cambios. */
  String urlParaMostrar(String fotoUrl);
}
