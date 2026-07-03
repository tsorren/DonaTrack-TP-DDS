package grupo5.logistica.services.mappers;

import grupo5.logistica.dto.rutas.RutaConEntregasResponseDTO;
import grupo5.logistica.dto.rutas.RutaResponseDTO;
import grupo5.logistica.infrastructure.GeneradorDeUrlSeguimiento;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RutaMapper {

  private final EntregaMapper entregaMapper;
  private final GeneradorDeUrlSeguimiento generadorDeUrlSeguimiento;

  public RutaMapper(EntregaMapper entregaMapper, GeneradorDeUrlSeguimiento generadorDeUrlSeguimiento) {
    this.entregaMapper = entregaMapper;
    this.generadorDeUrlSeguimiento = generadorDeUrlSeguimiento;
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
            ruta.getHoraFinReal(),
            calcularUrlSeguimiento(ruta));
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
            ruta.getHoraFinReal(),
            calcularUrlSeguimiento(ruta));
  }

  /**
   * Calcula la URL de seguimiento en tiempo real bajo demanda, en vez de leerla de un campo
   * persistido. Solo tiene sentido una vez que la ruta arrancó ({@code EN_TRASLADO} o
   * {@code COMPLETADA}); mientras está {@code PENDIENTE} no hay nada que seguir todavía. Al no
   * persistirla, siempre refleja la configuración de tracking vigente en el momento de la
   * consulta, en vez de quedar cacheada con un valor que puede desactualizarse si cambia la base
   * URL del front de seguimiento entre ambientes.
   */
  private String calcularUrlSeguimiento(Ruta ruta) {
    if (ruta.getEstado() == EstadoRuta.PENDIENTE) {
      return null;
    }
    return generadorDeUrlSeguimiento.generarUrl(ruta.getId());
  }
}