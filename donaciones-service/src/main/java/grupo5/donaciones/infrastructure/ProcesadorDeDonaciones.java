package grupo5.donaciones.infrastructure;

import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemanticoBien;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.itemsNormalizados.EvaluadorNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
  private final ISubcategoriasRepository subcategoriasRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Async
  public void procesar(Donacion donacion) {
    List<ItemDonacionNormalizado> itemsNormalizados = normalizador.normalizar(donacion);
    logItemsNormalizados(itemsNormalizados);

    itemNormalizadoRepository.saveAll(itemsNormalizados);

    finalizarNormalizacion(donacion, itemsNormalizados);
  }

  private void logItemsNormalizados(List<ItemDonacionNormalizado> itemsNormalizados) {
    for (ItemDonacionNormalizado item : itemsNormalizados) {
      String subcatNombre = "null";
      if (item.getBien() != null && item.getBien().subcategoriaId() != null) {
        Subcategoria subcat =
            subcategoriasRepository.findById(item.getBien().subcategoriaId()).orElse(null);
        if (subcat != null) {
          subcatNombre = subcat.getNombre();
        }
      }

      log.info(
          "  Item: {}, Subcategoría asignada: {}, Confianza: {}, Estado: {}",
          item.getBien() != null && item.getBien().bienOriginal() != null
              ? item.getBien().bienOriginal().descripcion()
              : "null",
          subcatNombre,
          item.getBien() != null ? item.getBien().confianza() : "null",
          item.getBien() != null ? item.getBien().estadoNormalizacion() : "null");
    }
  }

  private void finalizarNormalizacion(
      Donacion donacion, List<ItemDonacionNormalizado> itemsNormalizados) {
    if (EvaluadorNormalizacion.estanTodosNormalizados(itemsNormalizados)) {
      log.info("Donación {} normalizada inmediatamente. Publicando eventos.", donacion.getId());
      donacion.marcarNormalizada();
      donacionRepository.save(donacion);
      donacion.getDomainEvents().forEach(eventPublisher::publishEvent);
      donacion.clearDomainEvents();
    } else {
      log.info(
          "Donación {} tiene ítems pendientes de revisión. Queda en estado CARGADA.",
          donacion.getId());
    }
  }
}
