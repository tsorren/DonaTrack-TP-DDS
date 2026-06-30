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
    url = "${donatrack.incentivos.url}${donatrack.routes.incentivos.base}",
    configuration = FeignRetryConfig.class)
public interface IncentivosFeignClient {

  @PostMapping("${donatrack.routes.incentivos.donaciones}")
  void procesarDonacion(@RequestBody NuevaDonacionRequest request);

  @PostMapping("${donatrack.routes.incentivos.donaciones-exitosa}")
  void procesarDonacionExitosa(@RequestBody DonacionExitosaRequest request);

  @PostMapping("${donatrack.routes.incentivos.donantes-id}")
  DonanteRegistradoDTO registrarDonante(
      @PathVariable UUID donanteId, @RequestBody RegistrarDonanteRequest request);

  @DeleteMapping("${donatrack.routes.incentivos.donantes-id}")
  void darDeBaja(@PathVariable UUID donanteId);
}
