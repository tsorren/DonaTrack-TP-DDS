package grupo5.donaciones.services.impl;

import grupo5.donaciones.config.ClaveSegura;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.ISolicitudesEntregaRepository;
import grupo5.donaciones.services.logistica.IVerificadorOrigenEventos;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Autentica al proveedor que publicó un evento de vuelta con un token propio ({@code
 * donatrack.logistica.proveedor.<id>.token-vuelta}) y verifica que cada donación le pertenezca.
 *
 * <p>Falla cerrado: un evento sin identidad, de un proveedor sin token configurado, con un token
 * incorrecto o sobre una donación de otro proveedor se rechaza. Los tokens nunca se loguean. Es una
 * protección mínima: el token viaja en un header del mensaje, así que la garantía fuerte (usuario
 * de RabbitMQ por proveedor) sigue siendo la recomendación para un entorno real.
 */
@Service
public class VerificadorOrigenEventos implements IVerificadorOrigenEventos {

  private static final Logger log = LoggerFactory.getLogger(VerificadorOrigenEventos.class);

  private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9_-]+");

  private final Environment environment;
  private final ISolicitudesEntregaRepository solicitudesRepository;

  public VerificadorOrigenEventos(
      Environment environment, ISolicitudesEntregaRepository solicitudesRepository) {
    this.environment = environment;
    this.solicitudesRepository = solicitudesRepository;
  }

  @Override
  public boolean esOrigenValido(String proveedorId, String token, Collection<UUID> donaciones) {
    if (!seAutentica(proveedorId, token)) {
      return false;
    }
    for (UUID donacionId : donaciones) {
      if (!esDelProveedor(donacionId, proveedorId)) {
        log.warn(
            "[ORIGEN-EVENTO] El proveedor {} informó sobre la donación {}, que no tiene asignada:"
                + " se descarta el evento",
            proveedorId,
            donacionId);
        return false;
      }
    }
    return true;
  }

  private boolean seAutentica(String proveedorId, String token) {
    if (proveedorId == null || !ID_VALIDO.matcher(proveedorId).matches()) {
      log.warn("[ORIGEN-EVENTO] Evento sin proveedor identificable: se descarta");
      return false;
    }
    String esperado =
        environment.getProperty("donatrack.logistica.proveedor." + proveedorId + ".token-vuelta");
    if (esperado == null || esperado.isBlank()) {
      log.warn(
          "[ORIGEN-EVENTO] El proveedor {} no tiene token-vuelta configurado: se descarta el"
              + " evento",
          proveedorId);
      return false;
    }
    if (!ClaveSegura.coinciden(token, esperado)) {
      log.warn(
          "[ORIGEN-EVENTO] Token ausente o incorrecto del proveedor {}: se descarta", proveedorId);
      return false;
    }
    return true;
  }

  private boolean esDelProveedor(UUID donacionId, String proveedorId) {
    return solicitudesRepository
        .findMasRecientePorDonacion(donacionId)
        .map(SolicitudEntrega::getProveedorActual)
        .filter(proveedorId::equals)
        .isPresent();
  }
}
