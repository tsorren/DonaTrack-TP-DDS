package grupo5.cliente.rest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

/**
 * Lo que el cliente usa de Donaciones (docs/arquitectura/contratos/openapi-donaciones.yaml). Los
 * records toman solo los campos que muestra la pantalla; el resto del JSON se ignora.
 */
interface DonacionesApi {

  @GetExchange("/donaciones-independientes")
  List<Donacion> donaciones(@RequestParam UUID donanteId);

  /** Las asignadas a una entidad beneficiaria. */
  @GetExchange("/donaciones-independientes")
  List<Donacion> asignadas(@RequestParam UUID entidadId);

  /** La donación original, para saber quién donó. */
  @GetExchange("/api/donaciones/{id}")
  Original donacion(@PathVariable UUID id);

  @GetExchange("/api/categorias")
  List<Categoria> categorias();

  @GetExchange("/api/entidades/{id}")
  Entidad entidad(@PathVariable UUID id);

  /** {@code estadoActual} y {@code estadoNuevo} llegan como "EnDeposito" (ver el mapper). */
  record Donacion(
      UUID id,
      String descripcion,
      String estadoActual,
      LocalDateTime fechaRegistro,
      List<Cambio> historial,
      List<Item> items,
      int cantidad,
      UUID entidadBeneficiariaId,
      UUID donacionOriginalId) {}

  record Cambio(String estadoNuevo, LocalDateTime timestamp, String justificacion) {}

  record Item(BienNormalizado bien) {}

  record BienNormalizado(Categoria categoria) {}

  record Categoria(UUID id, String nombre, String unidad) {}

  record Entidad(Juridica juridica) {}

  record Juridica(String razonSocial) {}

  record Original(Donante donante) {}

  record Donante(Persona persona) {}

  /**
   * Una persona jurídica trae {@code razonSocial}; una humana, {@code nombre} y {@code apellido}.
   */
  record Persona(String razonSocial, String nombre, String apellido) {}
}
