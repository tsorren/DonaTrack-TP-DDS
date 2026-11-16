package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.donantes.DonanteEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonantesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonanteRepository;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.privacidad.Anonimizable;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.listados.CriterioDonantes;
import grupo5.donaciones.models.repositories.listados.Tramo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class DonanteRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Donante, DonanteEntity, SpringDataDonanteRepository>
    implements IDonantesRepository {

  private static final String DESDE =
      "FROM donaciones.donante d JOIN donaciones.persona p ON p.id = d.id" + SqlPersona.UNIONES;

  // Donó alguna vez algo de la categoría: algún ítem normalizado de sus donaciones
  private static final String DE_LA_CATEGORIA =
      "EXISTS (SELECT 1 FROM donaciones.donacion dn"
          + " JOIN donaciones.item_donacion_normalizado n ON n.id_donacion = dn.id"
          + " JOIN donaciones.subcategoria s ON s.id = n.id_subcategoria"
          + " WHERE dn.id_donante = d.id AND s.id_categoria = :categoriaId)";

  @PersistenceContext private EntityManager em;

  public DonanteRepositoryJpaAdapter(
      SpringDataDonanteRepository springDataRepo, DonantesPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Tramo<Donante> buscar(CriterioDonantes criterio) {
    ConsultaListado consulta =
        SqlPersona.filtrar(
                new ConsultaListado(DESDE), criterio.q(), criterio.tipo(), criterio.tipoJuridico())
            .siHay(criterio.activo(), "d.activo = :activo", "activo")
            .siHay(criterio.categoriaId(), DE_LA_CATEGORIA, "categoriaId")
            .siHay(
                criterio.altaDesde() == null ? null : criterio.altaDesde().atStartOfDay(),
                "d.fecha_alta >= :altaDesde",
                "altaDesde")
            .siHay(
                criterio.altaHasta() == null
                    ? null
                    : criterio.altaHasta().plusDays(1).atStartOfDay(),
                "d.fecha_alta < :altaHasta",
                "altaHasta");
    if (criterio.canal() != null) {
      // Para contactar: activos, no anonimizados y con ese medio
      consulta
          .donde("d.activo")
          .donde(
              "coalesce(h.nombre, j.razon_social) <> :anonimo",
              "anonimo",
              Anonimizable.VALOR_STRING)
          .donde(conMedio(criterio.canal()));
    }
    List<UUID> ids = consulta.ids(em, "d.id", orden(criterio), criterio.paginado());
    List<Donante> pagina = ConsultaListado.enOrden(ids, findAllById(ids), Donante::getId);
    return new Tramo<>(pagina, consulta.total(em));
  }

  private static String orden(CriterioDonantes criterio) {
    return switch (criterio.orden()) {
      case NUEVOS -> "d.fecha_alta DESC, d.id";
      case ANTIGUOS -> "d.fecha_alta ASC, d.id";
      case NOMBRE -> SqlPersona.NOMBRE_VISIBLE + ", d.id";
    };
  }

  /** La condición de tener un medio del canal; un canal desconocido no deja pasar a nadie. */
  private static String conMedio(String canal) {
    String medio =
        switch (canal.toUpperCase(Locale.ROOT)) {
          case "CORREO" -> "JOIN donaciones.correo c ON c.id = m.id WHERE";
          case "TELEFONO" ->
              "JOIN donaciones.telefono t ON t.id = m.id WHERE t.tipo_telefono = 'ESTANDAR' AND";
          case "WHATSAPP" ->
              "JOIN donaciones.telefono t ON t.id = m.id WHERE t.tipo_telefono = 'WHATSAPP' AND";
          default -> null;
        };
    if (medio == null) {
      return "1 = 0";
    }
    return "EXISTS (SELECT 1 FROM donaciones.medio_de_contacto m "
        + medio
        + " m.id_persona = p.id)";
  }

  @Override
  public List<Donante> findAllById(Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return springDataRepo.findAllById(ids).stream().map(toDomain).toList();
  }

  @Override
  public long contarActivos() {
    return springDataRepo.countByActivoTrue();
  }

  @Override
  public long contarActivosConDonaciones() {
    Object total =
        em.createNativeQuery(
                "SELECT count(*) FROM donaciones.donante d WHERE d.activo"
                    + " AND EXISTS (SELECT 1 FROM donaciones.donacion x WHERE x.id_donante = d.id)")
            .getSingleResult();
    return ((Number) total).longValue();
  }
}
