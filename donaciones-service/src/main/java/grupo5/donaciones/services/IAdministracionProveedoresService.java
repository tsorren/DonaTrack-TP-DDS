package grupo5.donaciones.services;

import grupo5.donaciones.dto.logistica.ProveedorLogisticaDTO;
import java.util.List;

/** Consulta y cambio en caliente del proveedor de logística preferido. */
public interface IAdministracionProveedoresService {

  List<ProveedorLogisticaDTO> listarProveedores();

  /**
   * @return los proveedores con el nuevo preferido
   * @throws grupo5.common.exceptions.ValidationException si el proveedor no está configurado
   */
  List<ProveedorLogisticaDTO> cambiarProveedorPreferido(String proveedorId);
}
