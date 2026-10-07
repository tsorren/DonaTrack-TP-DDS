package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.logistica.ProveedorLogisticaDTO;
import grupo5.donaciones.infrastructure.logistica.ProveedoresLogistica;
import grupo5.donaciones.services.IAdministracionProveedoresService;
import grupo5.donaciones.services.logistica.IPreferenciaProveedor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class AdministracionProveedoresService implements IAdministracionProveedoresService {

  private static final Logger log = LoggerFactory.getLogger(AdministracionProveedoresService.class);

  private final IPreferenciaProveedor preferencia;
  private final ProveedoresLogistica proveedores;
  private final Environment environment;

  public AdministracionProveedoresService(
      IPreferenciaProveedor preferencia,
      ProveedoresLogistica proveedores,
      Environment environment) {
    this.preferencia = preferencia;
    this.proveedores = proveedores;
    this.environment = environment;
  }

  @Override
  public List<ProveedorLogisticaDTO> listarProveedores() {
    String preferido = preferencia.proveedorPreferido();
    return preferencia.proveedoresConfigurados().stream()
        .map(
            id ->
                new ProveedorLogisticaDTO(
                    id, transporteDe(id), proveedores.buscar(id).isPresent(), id.equals(preferido)))
        .toList();
  }

  @Override
  public List<ProveedorLogisticaDTO> cambiarProveedorPreferido(String proveedorId) {
    String anterior = preferencia.proveedorPreferido();
    preferencia.cambiarProveedorPreferido(proveedorId);
    log.info(
        "[BROKER-LOGISTICA] Proveedor preferido cambiado de {} a {}",
        anterior,
        preferencia.proveedorPreferido());
    return listarProveedores();
  }

  private String transporteDe(String proveedorId) {
    return environment
        .getProperty("donatrack.logistica.proveedor." + proveedorId + ".transporte", "")
        .trim()
        .toLowerCase();
  }
}
