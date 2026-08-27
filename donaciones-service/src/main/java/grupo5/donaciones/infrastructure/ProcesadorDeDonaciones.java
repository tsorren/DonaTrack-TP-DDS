package grupo5.donaciones.infrastructure;

import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.itemsNormalizados.EvaluadorNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.normalizacion.NormalizadorSemanticoBien;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ProcesadorDeDonaciones {

  private static final Logger log = LoggerFactory.getLogger(ProcesadorDeDonaciones.class);

  private final NormalizadorSemanticoBien normalizadorSemantico;
  private final IDonacionesRepository donacionRepository;
  private final IItemDonacionNormalizadoRepository itemNormalizadoRepository;
  private final ISubcategoriasRepository subcategoriasRepository;
  private final ICategoriasRepository categoriasRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final double umbralAceptacion;

  public ProcesadorDeDonaciones(
      IDonacionesRepository donacionRepository,
      IItemDonacionNormalizadoRepository itemNormalizadoRepository,
      ISubcategoriasRepository subcategoriasRepository,
      ICategoriasRepository categoriasRepository,
      ApplicationEventPublisher eventPublisher,
      @Value("${donatrack.normalizacion.umbral-aceptacion:0.6}") double umbralAceptacion) {
    this.normalizadorSemantico = new NormalizadorSemanticoBien();
    this.donacionRepository = donacionRepository;
    this.itemNormalizadoRepository = itemNormalizadoRepository;
    this.subcategoriasRepository = subcategoriasRepository;
    this.categoriasRepository = categoriasRepository;
    this.eventPublisher = eventPublisher;
    this.umbralAceptacion = umbralAceptacion;
  }

  @Async
  public void procesar(Donacion donacion) {
<<<<<<< HEAD
    // INICIO LOGICA DE NEGOCIO
    List<ItemDonacionNormalizado> itemsNormalizados = normalizador.normalizar(donacion);
    // FIN LOGICA DE NEGOCIO
=======
    List<Subcategoria> subcategorias = subcategoriasRepository.findAll();
    Map<UUID, Categoria> categoriasPorId =
        categoriasRepository.findAll().stream()
            .collect(Collectors.toMap(Categoria::getId, c -> c, (a, b) -> a));

    List<ItemDonacionNormalizado> itemsNormalizados =
        normalizadorSemantico.normalizar(
            donacion, subcategorias, categoriasPorId, umbralAceptacion);
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
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
<<<<<<< HEAD

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
=======
    if (EvaluadorNormalizacion.estanTodosNormalizados(itemsNormalizados)) {
      log.info("Donación {} normalizada inmediatamente. Publicando eventos.", donacion.getId());
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
      donacion.marcarNormalizada();

      // FIN LOGICA DE NEGOCIO
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
