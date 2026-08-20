package grupo5.donaciones.infrastructure;

import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemanticoBien;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.events.DonacionNormalizadaEvent;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
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
    // INICIO LOGICA DE NEGOCIO
    List<ItemDonacionNormalizado> itemsNormalizados = normalizador.normalizar(donacion);
    // FIN LOGICA DE NEGOCIO
    logItemsNormalizados(itemsNormalizados);

    // Persistir todos los items normalizados
    itemNormalizadoRepository.saveAll(itemsNormalizados);

    // Verificar y finalizar
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

    // INICIO LOGICA DE NEGOCIO
    boolean tienePendientes =
        itemsNormalizados.stream()
            .anyMatch(
                item ->
                    item.getBien() != null
                        && item.getBien().estadoNormalizacion()
                            == EstadoNormalizacion.PENDIENTE_REVISION);

    if (!tienePendientes) {
      log.info("Donación {} normalizada inmediatamente. Publicando evento.", donacion.getId());
      donacion.marcarNormalizada();

      // FIN LOGICA DE NEGOCIO
      donacionRepository.save(donacion);
      eventPublisher.publishEvent(new DonacionNormalizadaEvent(donacion.getId()));
    } else {
      log.info(
          "Donación {} tiene ítems pendientes de revisión. Queda en estado CARGADA.",
          donacion.getId());
    }
  }
}
