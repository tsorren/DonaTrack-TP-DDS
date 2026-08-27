package grupo5.incentivos.fixtures;

import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class IncentivosFixturesTest {

  private IncentivosFixturesTest() {}

  public static RegistrarDonanteRequest registrarDonante(UUID id) {
    return new RegistrarDonanteRequest(id, UUID.randomUUID(), "Test");
  }

  public static RegistrarDonanteRequest registrarDonante(UUID id, UUID personaId, String nombre) {
    return new RegistrarDonanteRequest(id, personaId, nombre);
  }

  public static ModificarDonanteRequest modificarDonante(String nuevoNombre) {
    return new ModificarDonanteRequest(nuevoNombre);
  }

  public static NuevaDonacionRequest nuevaDonacion(UUID donanteId) {
    return new NuevaDonacionRequest(donanteId, List.of("arroz"), 5, LocalDate.now());
  }

  public static NuevaDonacionRequest nuevaDonacion(UUID donanteId, LocalDate fecha) {
    return new NuevaDonacionRequest(donanteId, List.of("arroz"), 5, fecha);
  }

  public static NuevaDonacionRequest nuevaDonacion(
      UUID donanteId, List<String> categorias, int cantidad, LocalDate fecha) {
    return new NuevaDonacionRequest(donanteId, categorias, cantidad, fecha);
  }

  public static DonacionExitosaRequest donacionExitosa(UUID donanteId) {
    return new DonacionExitosaRequest(donanteId, UUID.randomUUID());
  }

  public static DonacionExitosaRequest donacionExitosa(UUID donanteId, UUID donacionId) {
    return new DonacionExitosaRequest(donanteId, donacionId);
  }
}
