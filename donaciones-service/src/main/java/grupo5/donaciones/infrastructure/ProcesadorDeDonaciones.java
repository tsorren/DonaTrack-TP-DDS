package grupo5.donaciones.infrastructure;

import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemanticoBien;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.events.DonacionNormalizadaEvent;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// TODO: Testear Procesador

@Service
@RequiredArgsConstructor
public class ProcesadorDeDonaciones {

  private static final Logger log = LoggerFactory.getLogger(ProcesadorDeDonaciones.class);

  private final NormalizadorSemanticoBien normalizador;
  private final Segmentador segmentador;
  private final IDonacionesRepository donacionRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  private final IncentivosFeignClient incentivosFeignClient;
  private final IItemDonacionNormalizadoRepository itemNormalizadoRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Async
  public void procesar(Donacion donacion) {
    List<ItemDonacionNormalizado> itemsNormalizados = normalizador.normalizar(donacion);
    for (ItemDonacionNormalizado item : itemsNormalizados) {
      log.info(
          "  Item: {}, Subcategoría asignada: {}, Confianza: {}, Estado: {}",
          item.getBien().getBienOriginal().getDescripcion(),
          item.getBien().getSubcategoria() != null
              ? item.getBien().getSubcategoria().getNombre()
              : "null",
          item.getBien().getConfianza(),
          item.getBien().getEstadoNormalizacion());
    }

    // Persistir todos los items normalizados
    itemNormalizadoRepository.saveAll(itemsNormalizados);

    // Verificar si quedan pendientes de revisión
    boolean tienePendientes =
        itemsNormalizados.stream()
            .anyMatch(
                item ->
                    item.getBien().getEstadoNormalizacion()
                        == EstadoNormalizacion.PENDIENTE_REVISION);

    if (!tienePendientes) {
      log.info("Donación {} normalizada inmediatamente. Publicando evento.", donacion.getId());
      donacion.marcarNormalizada();
      donacionRepository.save(donacion);
      eventPublisher.publishEvent(new DonacionNormalizadaEvent(donacion.getId()));
    } else {
      log.info(
          "Donación {} tiene ítems pendientes de revisión. Queda en estado CARGADA.",
          donacion.getId());
    }
  }
}
