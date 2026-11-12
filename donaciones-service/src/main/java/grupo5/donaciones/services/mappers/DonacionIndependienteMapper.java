package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.donacionesIndependientes.BienNormalizadoDTO;
import grupo5.donaciones.dto.donacionesIndependientes.BienResumenDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDIResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CategoriaResumenDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.ItemDonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.QuienDonoDTO;
import grupo5.donaciones.dto.donacionesIndependientes.SubcategoriaResumenDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.CambioEstado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.IFotosBienesService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class DonacionIndependienteMapper {

  private final ISubcategoriasRepository subcategoriasRepository;
  private final ICategoriasRepository categoriasRepository;
  private final IFotosBienesService fotosBienes;
  private final IDonacionesRepository donaciones;
  private final IDonantesRepository donantes;
  private final IPersonasRepository personas;

  public DonacionIndependienteMapper(
      ISubcategoriasRepository subcategoriasRepository,
      ICategoriasRepository categoriasRepository,
      IFotosBienesService fotosBienes,
      IDonacionesRepository donaciones,
      IDonantesRepository donantes,
      IPersonasRepository personas) {
    this.subcategoriasRepository = subcategoriasRepository;
    this.categoriasRepository = categoriasRepository;
    this.fotosBienes = fotosBienes;
    this.donaciones = donaciones;
    this.donantes = donantes;
    this.personas = personas;
  }

  public DonacionIndependienteResponseDTO toDTO(DonacionIndependiente donacion) {
    if (donacion == null) {
      return null;
    }
    Lecturas lecturas =
        new Lecturas(
            id -> donaciones.findById(id).orElse(null),
            id -> donantes.findById(id).orElse(null),
            id -> personas.findById(id).orElse(null),
            id -> subcategoriasRepository.findById(id).orElse(null),
            id -> categoriasRepository.findById(id).orElse(null));
    return toDTO(donacion, lecturas);
  }

  /**
   * Una página: las donaciones originales, sus donantes y personas, y las subcategorías y
   * categorías de los bienes se leen por lote, no una vez por fila (R11-01).
   */
  public List<DonacionIndependienteResponseDTO> toDTOs(List<DonacionIndependiente> pagina) {
    Map<UUID, Donacion> originales =
        porId(
            donaciones.findAllById(
                distintos(pagina.stream().map(DonacionIndependiente::getDonacionOriginalId))),
            Donacion::getId);
    Set<UUID> donanteIds = distintos(originales.values().stream().map(Donacion::getDonanteId));
    Map<UUID, Donante> donantesPorId = porId(donantes.findAllById(donanteIds), Donante::getId);
    Map<UUID, Persona> personasPorId = porId(personas.findAllById(donanteIds), Persona::getId);
    Map<UUID, Subcategoria> subcategorias =
        porId(
            subcategoriasRepository.findAllById(
                distintos(
                    pagina.stream()
                        .flatMap(d -> d.getItems() == null ? Stream.empty() : d.getItems().stream())
                        .map(
                            i ->
                                i == null || i.bien() == null ? null : i.bien().subcategoriaId()))),
            Subcategoria::getId);
    Map<UUID, Categoria> categorias =
        porId(
            categoriasRepository.findAllById(
                distintos(subcategorias.values().stream().map(Subcategoria::getCategoriaId))),
            Categoria::getId);
    Lecturas lecturas =
        new Lecturas(
            originales::get,
            donantesPorId::get,
            personasPorId::get,
            subcategorias::get,
            categorias::get);
    return pagina.stream().map(d -> toDTO(d, lecturas)).toList();
  }

  private static Set<UUID> distintos(Stream<UUID> ids) {
    return ids.filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private static <T> Map<UUID, T> porId(List<T> elementos, Function<T, UUID> id) {
    return elementos.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a));
  }

  /** De dónde sale cada lectura: una por id (un detalle) o un mapa ya cargado (una página). */
  private record Lecturas(
      Function<UUID, Donacion> donacion,
      Function<UUID, Donante> donante,
      Function<UUID, Persona> persona,
      Function<UUID, Subcategoria> subcategoria,
      Function<UUID, Categoria> categoria) {}

  private DonacionIndependienteResponseDTO toDTO(
      DonacionIndependiente donacion, Lecturas lecturas) {
    List<ItemDonacionIndependienteResponseDTO> itemsMapped =
        mapItems(donacion.getItems(), lecturas);
    List<CambioEstadoDIResponseDTO> historialMapped = mapHistorial(donacion.getHistorial());

    return new DonacionIndependienteResponseDTO(
        donacion.getId(),
        donacion.getDonacionOriginalId(),
        donacion.getDescripcion(),
        donacion.getEstadoActual() != null
            ? donacion.getEstadoActual().getClass().getSimpleName()
            : null,
        donacion.getFechaRegistro(),
        historialMapped,
        itemsMapped,
        donacion.getCantidad(),
        donacion.getEntidadBeneficiariaId(),
        donacion.getNecesidadId(),
        donacion.isCancelacionPedida(),
        quienDono(donacion.getDonacionOriginalId(), lecturas));
  }

  private static QuienDonoDTO quienDono(UUID donacionOriginalId, Lecturas lecturas) {
    if (donacionOriginalId == null) {
      return null;
    }
    Donacion donacion = lecturas.donacion().apply(donacionOriginalId);
    if (donacion == null) {
      return null;
    }
    UUID donanteId = donacion.getDonanteId();
    Persona persona = lecturas.persona().apply(donanteId);
    Donante donante = lecturas.donante().apply(donanteId);
    if (persona == null || donante == null) {
      return null;
    }
    String tipo = persona.getTipoPersona() == TipoPersona.JURIDICA ? "EMPRESA" : "PERSONA";
    boolean visible = donacion.muestraNombreDe(donante) && !persona.estaAnonimizada();
    return new QuienDonoDTO(tipo, visible ? persona.getNombreCompleto() : null, donanteId);
  }

  private List<ItemDonacionIndependienteResponseDTO> mapItems(
      List<ItemDonacionIndependiente> items, Lecturas lecturas) {
    if (items == null) {
      return List.of();
    }
    return items.stream().map(item -> mapItem(item, lecturas)).toList();
  }

  private ItemDonacionIndependienteResponseDTO mapItem(
      ItemDonacionIndependiente item, Lecturas lecturas) {
    if (item == null) {
      return null;
    }
    BienNormalizadoDTO bienDTO = mapBienNormalizado(item.bien(), lecturas);
    return new ItemDonacionIndependienteResponseDTO(bienDTO, item.cantidad());
  }

  private BienNormalizadoDTO mapBienNormalizado(
      BienNormalizado bienNormalizado, Lecturas lecturas) {
    if (bienNormalizado == null) {
      return null;
    }
    BienResumenDTO bienResumen = mapBienResumen(bienNormalizado.bienOriginal());

    SubcategoriaResumenDTO subcategoriaResumen = null;
    CategoriaResumenDTO categoriaResumen = null;
    if (bienNormalizado.subcategoriaId() != null) {
      Subcategoria sub = lecturas.subcategoria().apply(bienNormalizado.subcategoriaId());
      if (sub != null) {
        subcategoriaResumen = mapSubcategoriaResumen(sub);
        categoriaResumen = mapCategoriaResumen(sub, lecturas);
      }
    }
    return new BienNormalizadoDTO(bienResumen, subcategoriaResumen, categoriaResumen);
  }

  private static List<CambioEstadoDIResponseDTO> mapHistorial(List<CambioEstado> historial) {
    if (historial == null) {
      return List.of();
    }
    return historial.stream().map(DonacionIndependienteMapper::mapCambioEstado).toList();
  }

  private static CambioEstadoDIResponseDTO mapCambioEstado(CambioEstado c) {
    if (c == null) {
      return null;
    }
    String estadoAnterior =
        c.getEstadoAnterior() != null ? c.getEstadoAnterior().getClass().getSimpleName() : null;
    String estadoNuevo =
        c.getEstadoNuevo() != null ? c.getEstadoNuevo().getClass().getSimpleName() : null;
    return new CambioEstadoDIResponseDTO(
        estadoAnterior, estadoNuevo, c.getTimestamp(), c.getJustificacion(), c.getActor());
  }

  /** La foto se devuelve como URL temporal de descarga: en el bien se guarda la referencia. */
  private BienResumenDTO mapBienResumen(Bien bienOriginal) {
    if (bienOriginal == null) {
      return null;
    }
    return new BienResumenDTO(
        bienOriginal.descripcion(),
        fotosBienes.urlParaMostrar(bienOriginal.fotoUrl()),
        bienOriginal.fechaVencimiento(),
        bienOriginal.estado());
  }

  private static SubcategoriaResumenDTO mapSubcategoriaResumen(Subcategoria sub) {
    if (sub == null) {
      return null;
    }
    return new SubcategoriaResumenDTO(sub.getId(), sub.getNombre());
  }

  private static CategoriaResumenDTO mapCategoriaResumen(Subcategoria sub, Lecturas lecturas) {
    if (sub == null || sub.getCategoriaId() == null) {
      return null;
    }
    Categoria cat = lecturas.categoria().apply(sub.getCategoriaId());
    if (cat == null) {
      return null;
    }
    return new CategoriaResumenDTO(
        cat.getId(),
        cat.getNombre(),
        cat.getTipoUnidad() != null ? cat.getTipoUnidad().name() : "UNIDADES");
  }
}
