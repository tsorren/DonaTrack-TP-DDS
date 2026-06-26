package grupo5.incentivos.services;

import grupo5.incentivos.dto.*;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.List;
import java.util.UUID;

public interface IIncentivosService {
  DonanteRegistradoDTO registrarDonante(RegistrarDonanteRequest request);

  void modificarDonante(UUID donanteId, ModificarDonanteRequest request);

  void procesarDonacion(NuevaDonacionRequest request);

  void procesarDonacionExitosa(DonacionExitosaRequest request);

  void procesarInactividad();

  DonanteIncentivos obtenerDonante(UUID donanteId);

  MetricasDonanteDTO obtenerMetricas(UUID donanteId);

  List<MisionDTO> obtenerMisiones(UUID donanteId);

  List<InsigniaDTO> obtenerInsignias(UUID donanteId);

  void configurarVisibilidadInsignia(UUID donanteId, String nombreInsignia, boolean visible);

  void darDeBaja(UUID donanteId);

  ResumenSistemaDTO obtenerResumenSistema();

  List<DonanteIncentivos> listarTodos();
}
