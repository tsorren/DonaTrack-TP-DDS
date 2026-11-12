package grupo5.cliente.puertos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Lo que el sitio lee y hace en Donaciones con el token de quien tiene la sesión. */
public interface DonacionesComoUsuario {

  List<NecesidadDeEntidad> necesidadesDe(UUID entidadId);

  /** Las personas jurídicas registradas, con su razón social; las lee quien administra. */
  List<Juridica> juridicas();

  /** Da de alta la entidad beneficiaria de una persona jurídica; la hace quien administra. */
  void registrarEntidad(UUID juridicaId);

  record Juridica(UUID id, String razonSocial) {}

  record NecesidadDeEntidad(
      UUID id,
      String tipo,
      int cantidadNecesitada,
      String descripcion,
      boolean estaSatisfecha,
      LocalDate fechaInicio,
      LocalDate fechaFin) {}
}
