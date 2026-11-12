package grupo5.cliente.puertos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Las cuentas de las personas: de dónde sale su correo y dónde se guarda a qué persona operan. */
public interface DirectorioDeCuentas {

  /** Una cuenta cuya entidad espera la aprobación del equipo; {@code personaId} es su jurídica. */
  record SolicitudDeEntidad(String cuentaId, UUID personaId, String correo) {}

  String correoDe(String cuentaId);

  /** Escribe los atributos en la cuenta y, si hay plantilla, se la asigna. */
  void vincular(String cuentaId, Map<String, String> atributos, Optional<String> plantilla);

  List<SolicitudDeEntidad> solicitudesDeEntidad();

  /** Suma a la cuenta un rol de la plataforma (el área a la que entra). */
  void asignarRol(String cuentaId, String rol);

  void asignarPlantilla(String cuentaId, String plantilla);

  void quitarAtributo(String cuentaId, String atributo);
}
