package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.ArchivosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataArchivoFilaErrorRepository;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataArchivoRepository;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.models.repositories.listados.CriterioArchivos;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.Tramo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class ArchivoDonantesRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Archivo, ArchivoEntity, SpringDataArchivoRepository>
    implements IArchivoDonantesRepository {

  private final SpringDataArchivoFilaErrorRepository filasConError;
  private final ArchivosPersistenciaMapper mapper;

  @PersistenceContext private EntityManager em;

  public ArchivoDonantesRepositoryJpaAdapter(
      SpringDataArchivoRepository springDataRepo,
      SpringDataArchivoFilaErrorRepository filasConError,
      ArchivosPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.filasConError = filasConError;
    this.mapper = mapper;
  }

  @Override
  public void guardarFilasConError(List<FilaConError> filas) {
    filasConError.saveAll(filas.stream().map(mapper::filaToEntity).toList());
  }

  @Override
  public List<FilaConError> buscarFilasConError(UUID archivoId, int pagina, int tamanio) {
    return filasConError
        .findByArchivoIdOrderByLineaAsc(archivoId, PageRequest.of(pagina, tamanio))
        .stream()
        .map(mapper::filaToDomain)
        .toList();
  }

  /**
   * R11-20: la búsqueda, los filtros, el orden y el tramo, en la base. Las fechas de subida se
   * guardan en la hora de Buenos Aires: el día pedido va de su medianoche a la del día siguiente.
   */
  @Override
  @Transactional(readOnly = true)
  public Tramo<Archivo> buscar(CriterioArchivos criterio) {
    ConsultaListado consulta =
        new ConsultaListado("FROM donaciones.archivo a")
            .siHay(
                criterio.estados().stream().map(Enum::name).toList(),
                "a.estado_archivo IN (:estados)",
                "estados")
            .siHay(
                criterio.desde() == null ? null : criterio.desde().atStartOfDay(),
                "a.subido_en >= :desde",
                "desde")
            .siHay(
                criterio.hasta() == null ? null : criterio.hasta().plusDays(1).atStartOfDay(),
                "a.subido_en < :hasta",
                "hasta");
    if (criterio.q() != null) {
      consulta.donde(
          ConsultaListado.contiene("a.nombre_original", "q"),
          "q",
          ConsultaListado.patron(criterio.q()));
    }
    String orden =
        criterio.orden() == OrdenPorFecha.ANTIGUAS
            ? "a.subido_en ASC NULLS LAST, a.id"
            : "a.subido_en DESC NULLS LAST, a.id";
    List<UUID> ids = consulta.ids(em, "a.id", orden, criterio.paginado());
    List<Archivo> pagina =
        ConsultaListado.enOrden(
            ids,
            springDataRepo.findAllById(ids).stream().map(mapper::toDomain).toList(),
            Archivo::getId);
    return new Tramo<>(pagina, consulta.total(em));
  }
}
