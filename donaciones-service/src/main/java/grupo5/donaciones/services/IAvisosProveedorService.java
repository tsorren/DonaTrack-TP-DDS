package grupo5.donaciones.services;

import grupo5.donaciones.dto.logistica.AvisoProveedorRequestDTO;

/**
 * Recibe lo que un proveedor de logística HTTP informa sobre las donaciones que se le asignaron.
 */
public interface IAvisosProveedorService {

  /**
   * Aplica el aviso sobre las donaciones, solo si todas están asignadas al proveedor que avisa.
   *
   * @throws grupo5.common.exceptions.RecursoNoEncontradoException si alguna donación no está
   *     asignada a ese proveedor (no se revela si existe o pertenece a otro)
   * @throws grupo5.common.exceptions.ValidationException si faltan campos del tipo de aviso
   */
  void registrarAviso(String proveedorId, AvisoProveedorRequestDTO aviso);
}
