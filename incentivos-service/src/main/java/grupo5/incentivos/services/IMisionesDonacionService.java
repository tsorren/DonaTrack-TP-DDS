package grupo5.incentivos.services;

import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface IMisionesDonacionService {

  void procesarDonacion(NuevaDonacionRequest request);

  void procesarDonacionExitosa(DonacionExitosaRequest request);

  List<MisionDTO> obtenerMisiones(UUID donanteId);

  void verificarRachasVencidas(YearMonth mesActual);
}
