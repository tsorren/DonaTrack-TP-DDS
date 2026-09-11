package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.CambioCategoriaDTO;
import grupo5.incentivos.dto.DonantePerfilDTO;
import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface IDonanteIncentivosController {
  ResponseEntity<DonanteRegistradoDTO> registrarDonante(
      @PathVariable UUID donanteId, @Valid @RequestBody RegistrarDonanteRequest request);

  ResponseEntity<Void> darDeBaja(@PathVariable UUID donanteId);

  ResponseEntity<Void> modificarDonante(
      @PathVariable UUID donanteId, @Valid @RequestBody ModificarDonanteRequest request);

  ResponseEntity<DonantePerfilDTO> obtenerDonante(@PathVariable UUID donanteId);

  ResponseEntity<List<CambioCategoriaDTO>> obtenerAscensos(@PathVariable UUID donanteId);
}
