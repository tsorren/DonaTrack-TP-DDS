package grupo5.incentivos.services;

import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.List;
import java.util.UUID;

public interface IGestionDonanteService {

  DonanteRegistradoDTO registrarDonante(RegistrarDonanteRequest request);

  void modificarDonante(UUID donanteId, ModificarDonanteRequest request);

  DonanteIncentivos obtenerDonante(UUID donanteId);

  void darDeBaja(UUID donanteId);

  List<DonanteIncentivos> listarTodos();
}
