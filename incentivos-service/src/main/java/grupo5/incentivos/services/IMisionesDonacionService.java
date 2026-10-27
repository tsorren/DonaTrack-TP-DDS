package grupo5.incentivos.services;

import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface IMisionesDonacionService {

  void procesarDonacion(NuevaDonacionRequest request);

  void procesarDonacion(NuevaDonacionRequest request, UUID eventoId);

  void procesarDonacionExitosa(DonacionExitosaRequest request);

  void procesarDonacionExitosa(DonacionExitosaRequest request, UUID eventoId);

  List<MisionDTO> obtenerMisiones(UUID donanteId);

  void verificarRachasVencidas(YearMonth mesActual);
}
