package grupo5.logistica.services.mappers;

import grupo5.logistica.dto.rutas.RutaConEntregasResponseDTO;
import grupo5.logistica.dto.rutas.RutaResponseDTO;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RutaMapper {

  private final EntregaMapper entregaMapper;

  public RutaMapper(EntregaMapper entregaMapper) {
    this.entregaMapper = entregaMapper;
  }

  public RutaResponseDTO toResponseDTO(Ruta ruta) {
    if (ruta == null) {
      return null;
    }

    return new RutaResponseDTO(
        ruta.getId(),
        ruta.getFecha(),
        ruta.getEntregaIds(),
        ruta.getChoferId(),
        ruta.getCamionId(),
        ruta.getEstado(),
        ruta.getHoraInicioReal(),
        ruta.getHoraFinReal());
  }

  public RutaConEntregasResponseDTO toResponseDTOConEntregas(Ruta ruta, List<Entrega> entregas) {
    if (ruta == null) {
      return null;
    }

    return new RutaConEntregasResponseDTO(
        ruta.getId(),
        ruta.getFecha(),
        entregas.stream().map(entregaMapper::toResponseDTO).toList(),
        ruta.getChoferId(),
        ruta.getCamionId(),
        ruta.getEstado(),
        ruta.getHoraInicioReal(),
        ruta.getHoraFinReal());
  }
}
