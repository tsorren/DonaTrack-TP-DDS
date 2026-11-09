package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import grupo5.donaciones.models.entities.necesidades.ResultadoPeriodo;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.INecesidadesService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NecesidadesService implements INecesidadesService {

  private final INecesidadesRepository necesidadRepository;
  private final IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private final ISubcategoriasRepository subcategoriaRepository;
  private final ICategoriasRepository categoriasRepository;
  private final AcumuladosDeAsignacion acumulados;
  private final CancelacionDeEntregas cancelacionDeEntregas;

  public NecesidadesService(
      INecesidadesRepository necesidadRepository,
      IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository,
      ISubcategoriasRepository subcategoriaRepository,
      ICategoriasRepository categoriasRepository,
      AcumuladosDeAsignacion acumulados,
      CancelacionDeEntregas cancelacionDeEntregas) {
    this.necesidadRepository = necesidadRepository;
    this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
    this.subcategoriaRepository = subcategoriaRepository;
    this.categoriasRepository = categoriasRepository;
    this.acumulados = acumulados;
    this.cancelacionDeEntregas = cancelacionDeEntregas;
  }

  private NecesidadDTO toDTO(Necesidad necesidad) {
    return toDTO(
        necesidad,
        acumulados.asignadoA(necesidad),
        acumulados.recibidoPor(necesidad),
        unidadesPorSubcategoria());
  }

  private static NecesidadDTO toDTO(
      Necesidad n, int asignado, int recibido, Map<UUID, Unidad> unidades) {
    NecesidadDTO.UltimoCambio ultimoCambio =
        n.muestraUltimoCambio()
            ? new NecesidadDTO.UltimoCambio(n.getCantidadAnterior(), n.getFechaUltimoCambio())
            : null;
    return new NecesidadDTO(
        n.getId(),
        n.getTipoNecesidad().name(),
        n.getEntidadId(),
        n.getSubcategoriaId(),
        n.getCantidadNecesitada(),
        n.getDescripcion(),
        n.estaSatisfechaCon(asignado),
        n.getFechaInicio(),
        n.finDelPeriodoActual(),
        n.estado(recibido, hoy()),
        n.getVersion(),
        n instanceof NecesidadExtraordinaria e ? e.getFechaLimite() : null,
        n instanceof NecesidadRecurrente r ? r.getPeriodicidad() : null,
        unidades.get(n.getSubcategoriaId()),
        ultimoCambio);
  }

  /**
   * La unidad en que se pide cada subcategoría: la suya o la de su categoría.
   *
   * <p>ponytail: lee todo el catálogo en cada llamada (es chico); pasar a una consulta por ids si
   * crece.
   */
  private Map<UUID, Unidad> unidadesPorSubcategoria() {
    Map<UUID, Unidad> unidadPorCategoria = new HashMap<>();
    categoriasRepository
        .findAll()
        .forEach(c -> unidadPorCategoria.put(c.getId(), c.getTipoUnidad()));
    Map<UUID, Unidad> unidades = new HashMap<>();
    for (Subcategoria s : subcategoriaRepository.findAll()) {
      Unidad unidad = s.unidadCon(unidadPorCategoria.get(s.getCategoriaId()));
      if (unidad != null) {
        unidades.put(s.getId(), unidad);
      }
    }
    return unidades;
  }

  private static LocalDate hoy() {
    return LocalDate.now(ZoneId.systemDefault());
  }

  @Override
  public NecesidadDTO guardar(NecesidadDTO dto) {
    Necesidad necesidadDominio = convertirDTOANecesidad(dto);
    necesidadRepository.save(necesidadDominio);

    return toDTO(necesidadDominio);
  }

  @Override
  public NecesidadDTO obtenerPorId(UUID id) {
    return toDTO(buscar(id));
  }

  @Override
  public List<NecesidadDTO> listarConFiltros(
      UUID entidadId, String tipo, List<EstadoNecesidad> estados) {
    List<Necesidad> necesidades =
        necesidadRepository.findAll().stream()
            .filter(n -> entidadId == null || entidadId.equals(n.getEntidadId()))
            .filter(n -> tipo == null || tipo.equalsIgnoreCase(n.getTipoNecesidad().name()))
            .toList();
    Map<UUID, Integer> asignado = acumulados.asignadoA(necesidades);
    Map<UUID, Integer> recibido = acumulados.recibidoPor(necesidades);
    Map<UUID, Unidad> unidades = unidadesPorSubcategoria();
    return necesidades.stream()
        .map(
            n ->
                toDTO(
                    n,
                    asignado.getOrDefault(n.getId(), 0),
                    recibido.getOrDefault(n.getId(), 0),
                    unidades))
        .filter(dto -> estados == null || estados.isEmpty() || estados.contains(dto.estado()))
        .toList();
  }

  @Override
  public List<NecesidadPublicaDTO> listarPublicas(UUID entidadId) {
    LocalDate hoy = hoy();
    List<Necesidad> necesidades =
        necesidadRepository.findAll().stream()
            .filter(n -> n.admiteAsignaciones(hoy))
            .filter(n -> entidadId == null || entidadId.equals(n.getEntidadId()))
            .toList();
    Map<UUID, Integer> asignado = acumulados.asignadoA(necesidades);
    return necesidades.stream()
        .filter(n -> !n.estaSatisfechaCon(asignado.getOrDefault(n.getId(), 0)))
        .map(
            n ->
                new NecesidadPublicaDTO(
                    n.getId(),
                    n.getEntidadId(),
                    n.getSubcategoriaId(),
                    n.getDescripcion(),
                    n.getCantidadNecesitada(),
                    n.getTipoNecesidad().name(),
                    n.finDelPeriodoActual()))
        .toList();
  }

  @Override
  public NecesidadDTO actualizar(UUID id, NecesidadDTO dto) {
    Necesidad necesidad = buscar(id);
    // La versión es opcional mientras dure la migración del contrato (ADR 20261102, punto 2)
    if (dto.version() != null && !dto.version().equals(necesidad.getVersion())) {
      throw new BusinessStateException(ErrorCatalog.NECESIDAD_MODIFICADA_CONCURRENTEMENTE);
    }
    necesidad.actualizar(dto.descripcion(), dto.cantidadNecesitada());
    necesidadRepository.save(necesidad);
    return toDTO(necesidad);
  }

  @Override
  public List<PeriodoNecesidadDTO> listarPeriodos(UUID id) {
    if (!(buscar(id) instanceof NecesidadRecurrente recurrente)) {
      return List.of();
    }
    List<PeriodoNecesidad> periodos = recurrente.getPeriodos();
    PeriodoNecesidad actual = recurrente.obtenerPeriodoActual();
    // ponytail: dos consultas por período; una agrupada si las historias se alargan
    return periodos.stream()
        .map(
            p -> {
              int recibido = acumulados.recibidoEn(id, p.id());
              return new PeriodoNecesidadDTO(
                  p.id(),
                  recurrente.inicioDe(p),
                  p.fechaFin(),
                  p.cantidadObjetivo(),
                  acumulados.asignadoEn(id, p.id()),
                  recibido,
                  resultado(p, recibido, p.equals(actual)));
            })
        .toList();
  }

  private static ResultadoPeriodo resultado(PeriodoNecesidad p, int recibido, boolean enCurso) {
    if (p.estaSatisfechaCon(recibido)) {
      return ResultadoPeriodo.CUMPLIDO;
    }
    return enCurso ? ResultadoPeriodo.EN_CURSO : ResultadoPeriodo.INCOMPLETO;
  }

  @Override
  @Transactional
  public void eliminar(UUID id) {
    Necesidad necesidad = buscar(id);
    necesidad.desactivar();
    necesidadRepository.save(necesidad);
    cancelacionDeEntregas.cancelarLasQueNoSalieron(id);
  }

  private Necesidad buscar(UUID id) {
    return necesidadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  // MAPPER INPUT (dto -> dominio)
  private Necesidad convertirDTOANecesidad(NecesidadDTO dto) {

    subcategoriaRepository
        .findById(dto.idSubcategoria())
        .orElseThrow(() -> new RecursoNoEncontradoException(dto.idSubcategoria()));
    EntidadBeneficiaria entidad =
        entidadesBeneficiariasRepository
            .findById(dto.idEntidad())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.idEntidad()));
    if (!entidad.estaActivo()) {
      throw new BusinessStateException(ErrorCatalog.ENTIDAD_BENEFICIARIA_INACTIVA);
    }
    Necesidad necesidad =
        switch (dto.tipo()) {
          case "RECURRENTE" -> {
            // fechaFin quedó deprecado como entrada: el largo sale de la periodicidad
            if (dto.periodicidad() == null) {
              throw new ValidationException(ErrorCatalog.NECESIDAD_RECURRENTE_SIN_PERIODO);
            }
            yield new NecesidadRecurrente(
                dto.idSubcategoria(),
                dto.cantidadNecesitada(),
                dto.descripcion(),
                dto.periodicidad().periodo(),
                dto.fechaInicio() != null ? dto.fechaInicio() : hoy());
          }
          case "EXTRAORDINARIA" ->
              new NecesidadExtraordinaria(
                  dto.idSubcategoria(),
                  dto.cantidadNecesitada(),
                  dto.descripcion(),
                  dto.fechaLimite());
          default -> throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
        };
    if (dto.idEntidad() != null) {
      necesidad.asociarAEntidad(dto.idEntidad());
    }

    return necesidad;
  }
}
