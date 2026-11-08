package grupo5.cliente.puertos;

import java.util.Map;
import java.util.Optional;

/** Las cuentas de las personas: de dónde sale su correo y dónde se guarda a qué persona operan. */
public interface DirectorioDeCuentas {

  String correoDe(String cuentaId);

  /** Escribe los atributos en la cuenta y, si hay plantilla, se la asigna. */
  void vincular(String cuentaId, Map<String, String> atributos, Optional<String> plantilla);
}
