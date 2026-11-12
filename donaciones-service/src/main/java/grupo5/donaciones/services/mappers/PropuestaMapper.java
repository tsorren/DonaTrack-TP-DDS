package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.propuestas.GrupoFragmentacionDTO;
import grupo5.donaciones.dto.propuestas.NecesidadResumenDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.dto.propuestas.SugerenciaDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.listados.PropuestaEnListado;
import grupo5.donaciones.models.repositories.listados.StockEnPropuestas;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Arma la respuesta de las propuestas. Una página se arma con lecturas por lote (necesidades,
 * entidades, unidades y donaciones), no con una lectura por fila (R11-24, A7).
 */
@Component
public class PropuestaMapper {

  private final INecesidadesRepository necesidadRepository;
  private final IDonacionesIndependientesRepository donacionRepository;
  private final DonacionIndependienteMapper diMapper;
  private final IPersonasRepository personas;
  private final ISubcategoriasRepository subcategorias;
  private final ICategoriasRepository categorias;

  public PropuestaMapper(
      INecesidadesRepository necesidadRepository,
      IDonacionesIndependientesRepository donacionRepository,
      DonacionIndependienteMapper diMapper,
      IPersonasRepository personas,
      ISubcategoriasRepository subcategorias,
      ICategoriasRepository categorias) {
    this.necesidadRepository = necesidadRepository;
    this.donacionRepository = donacionRepository;
    this.diMapper = diMapper;
    this.personas = personas;
    this.subcategorias = subcategorias;
    this.categorias = categorias;
  }

  /**
   * Una propuesta sin el cálculo contra las otras pendientes (la que se acaba de generar): sin
   * situación y con lo que queda de cada donación según su estado.
   */
  public PropuestaDTO toDTO(Propuesta propuesta) {
    if (propuesta == null) {
      return null;
    }
    return toDTOs(List.of(sinCalculo(propuesta))).getFirst();
  }

  /** Las que se acaban de generar, armadas por lote. */
  public List<PropuestaDTO> toDTOsSinCalculo(List<Propuesta> propuestas) {
    return toDTOs(propuestas.stream().map(PropuestaMapper::sinCalculo).toList());
  }

  private static PropuestaEnListado sinCalculo(Propuesta propuesta) {
    return new PropuestaEnListado(propuesta, null, 0, Map.of());
  }

  /** Una página del listado, con su situación y lo que piden las otras pendientes. */
  public List<PropuestaDTO> toDTOs(List<PropuestaEnListado> pagina) {
    if (pagina.isEmpty()) {
      return List.of();
    }
    Lecturas lecturas = new Lecturas(pagina.stream().map(PropuestaEnListado::propuesta).toList());
    return pagina.stream().map(p -> toDTO(p, lecturas)).toList();
  }

  private PropuestaDTO toDTO(PropuestaEnListado enListado, Lecturas lecturas) {
    Propuesta propuesta = enListado.propuesta();
    Necesidad necesidad = lecturas.necesidades.get(propuesta.getNecesidadQueSatisfaceId());
    NecesidadResumenDTO necesidadDTO = necesidad == null ? null : lecturas.resumen(necesidad);

    // Una misma donación en dos fragmentaciones se informa una vez, con la suma
    Map<UUID, Integer> agrupado = new LinkedHashMap<>();
    for (PosibleFragmentacion f : propuesta.getPosiblesFragmentaciones()) {
      agrupado.merge(f.getDonacionOriginalId(), f.getCantidadNecesaria(), Integer::sum);
    }
    boolean pendiente = propuesta.getEstado() == EstadoPropuesta.PENDIENTE;
    List<GrupoFragmentacionDTO> fragmentaciones =
        agrupado.entrySet().stream()
            .map(
                e -> {
                  int cantidad = e.getValue() == null ? 0 : e.getValue();
                  StockEnPropuestas stock = enListado.porDonacion().get(e.getKey());
                  int disponible =
                      stock != null ? stock.disponible() : lecturas.disponible(e.getKey());
                  int pedidaEnOtras =
                      stock == null
                          ? 0
                          : Math.max(0, stock.pedidaEnPendientes() - (pendiente ? cantidad : 0));
                  int otrasPropuestas =
                      stock == null
                          ? 0
                          : Math.max(0, stock.propuestasPendientes() - (pendiente ? 1 : 0));
                  return new GrupoFragmentacionDTO(
                      lecturas.dtos.get(e.getKey()),
                      e.getValue(),
                      disponible,
                      pedidaEnOtras,
                      otrasPropuestas);
                })
            .toList();

    List<SugerenciaDTO> sugeridaPor =
        propuesta.getSugerencias().stream()
            .map(
                s ->
                    new SugerenciaDTO(
                        s.algoritmo(), s.puesto(), s.deCuantas(), s.eligioLasDonaciones()))
            .toList();

    return new PropuestaDTO(
        propuesta.getId(),
        propuesta.getEstado(),
        propuesta.getFechaCreacion(),
        necesidadDTO,
        fragmentaciones,
        propuesta.getDecididaPor(),
        propuesta.getFechaDecision(),
        sugeridaPor,
        enListado.situacion(),
        enListado.otrasPendientesMismaNecesidad(),
        propuesta.getMotivoDescarte());
  }

  /** Lo que hace falta para armar una página, leído una vez y por lote. */
  private final class Lecturas {

    private final Map<UUID, Necesidad> necesidades;
    private final Map<UUID, Integer> asignado;
    private final Map<UUID, Unidad> unidades;
    private final Map<UUID, String> nombres;
    private final Map<UUID, DonacionIndependiente> donaciones;
    private final Map<UUID, DonacionIndependienteResponseDTO> dtos;

    Lecturas(List<Propuesta> propuestas) {
      necesidades =
          porId(
              necesidadRepository.findAllById(
                  distintos(propuestas.stream().map(Propuesta::getNecesidadQueSatisfaceId))),
              Necesidad::getId);
      // La necesidad no conoce sus donaciones (DTI-06): lo asignado se suma por lote
      Map<UUID, UUID> periodoPorNecesidad = new HashMap<>();
      necesidades.values().forEach(n -> periodoPorNecesidad.put(n.getId(), n.periodoActualId()));
      asignado = donacionRepository.sumarCantidadAsignada(periodoPorNecesidad);
      unidades = unidadesDe(necesidades.values());
      nombres =
          personas
              .findAllById(distintos(necesidades.values().stream().map(Necesidad::getEntidadId)))
              .stream()
              .filter(Juridica.class::isInstance)
              .map(Juridica.class::cast)
              .filter(j -> j.getRazonSocial() != null)
              .collect(Collectors.toMap(Persona::getId, Juridica::getRazonSocial, (a, b) -> a));
      List<DonacionIndependiente> leidas =
          donacionRepository.findAllById(
              distintos(
                  propuestas.stream()
                      .flatMap(p -> p.getPosiblesFragmentaciones().stream())
                      .map(PosibleFragmentacion::getDonacionOriginalId)));
      donaciones = porId(leidas, DonacionIndependiente::getId);
      dtos = porId(diMapper.toDTOs(leidas), DonacionIndependienteResponseDTO::id);
    }

    NecesidadResumenDTO resumen(Necesidad n) {
      return new NecesidadResumenDTO(
          n.getId(),
          n.getDescripcion(),
          n.getCantidadNecesitada(),
          n.getClass().getSimpleName().replace("Necesidad", "").toUpperCase(),
          n.estaSatisfechaCon(asignado.getOrDefault(n.getId(), 0)) ? "SATISFECHA" : "PENDIENTE",
          n.getEntidadId(),
          nombres.get(n.getEntidadId()),
          unidades.get(n.getSubcategoriaId()));
    }

    /** Sin el cálculo del listado: lo que tiene la donación si sigue en el depósito. */
    int disponible(UUID donacionId) {
      DonacionIndependiente d = donaciones.get(donacionId);
      boolean enDeposito =
          d != null
              && d.getEstadoActual() != null
              && d.getEstadoActual().getTipo() == TipoEstadoDonacion.EN_DEPOSITO;
      return enDeposito ? d.getCantidad() : 0;
    }

    /** La unidad en que se pide cada subcategoría: la suya o la de su categoría. */
    private Map<UUID, Unidad> unidadesDe(Collection<Necesidad> leidas) {
      List<Subcategoria> subs =
          subcategorias.findAllById(distintos(leidas.stream().map(Necesidad::getSubcategoriaId)));
      Map<UUID, Categoria> cats =
          porId(
              categorias.findAllById(distintos(subs.stream().map(Subcategoria::getCategoriaId))),
              Categoria::getId);
      Map<UUID, Unidad> porSubcategoria = new HashMap<>();
      for (Subcategoria s : subs) {
        Categoria c = cats.get(s.getCategoriaId());
        Unidad unidad = s.unidadCon(c == null ? null : c.getTipoUnidad());
        if (unidad != null) {
          porSubcategoria.put(s.getId(), unidad);
        }
      }
      return porSubcategoria;
    }
  }

  private static Set<UUID> distintos(Stream<UUID> ids) {
    return ids.filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private static <T> Map<UUID, T> porId(List<T> elementos, Function<T, UUID> id) {
    return elementos.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a));
  }
}
