package grupo5.cliente.puertos;

import grupo5.cliente.vistas.EntidadEntregaVista;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/** Confirmar recepción (RF-12): las entregas que esperan la confirmación y el acta. */
public interface EntidadConfirmarPuerto {

  /**
   * La entrega {@code pedida} (la clave del acta, {@code ?entrega=}) si espera la confirmación y,
   * como mucho, otra que también espera, para ofrecerla después; sin {@code pedida}, las que la
   * maqueta muestre. No es una lista: el acta no pagina (R11-43).
   */
  List<EntidadEntregaVista> entregas(UUID entidad, String pedida);

  /** El día de hoy, para la firma del acta ("15 de octubre de 2026"). */
  String hoy();

  /**
   * Confirma la entrega {@code clave} con sus fotos y el remito; con {@code motivo} ({@code
   * incompleta}, {@code danada} u {@code otro}), además, el problema, con cuánto llegó y el
   * detalle.
   */
  void confirmar(UUID entidad, String clave, Acta acta);

  record Acta(
      List<MultipartFile> fotos,
      String remito,
      String motivo,
      Double cantidadRecibida,
      String detalle) {}
}
