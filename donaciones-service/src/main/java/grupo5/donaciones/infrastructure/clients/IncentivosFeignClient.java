package grupo5.donaciones.infrastructure.clients;

import grupo5.donaciones.dto.comunicaciones.DonacionExitosaRequest;
import grupo5.donaciones.dto.comunicaciones.DonanteRegistradoDTO;
import grupo5.donaciones.dto.comunicaciones.NuevaDonacionRequest;
import grupo5.donaciones.dto.comunicaciones.RegistrarDonanteRequest;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "incentivos-service",
    url = "${donatrack.incentivos.url}/api/incentivos",
    configuration = FeignRetryConfig.class)
public interface IncentivosFeignClient {

  @PostMapping("/donaciones")
  void procesarDonacion(@RequestBody NuevaDonacionRequest request);

  @PostMapping("/donaciones/exitosa")
  void procesarDonacionExitosa(@RequestBody DonacionExitosaRequest request);

  @PostMapping("/donantes/{donanteId}")
  DonanteRegistradoDTO registrarDonante(
      @PathVariable UUID donanteId, @RequestBody RegistrarDonanteRequest request);

  @DeleteMapping("/donantes/{donanteId}")
  void darDeBaja(@PathVariable UUID donanteId);
}
