package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.dto.entidadBeneficiaria.FiltroOrganizaciones;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioEntidades;
import grupo5.donaciones.models.repositories.listados.TextoBuscado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Entidades en memoria. La búsqueda por nombre cruza con las jurídicas de las personas; la de
 * categoría, con las necesidades y las subcategorías; la de "ya doné", con las donaciones
 * independientes y las donaciones (R11-12, las mismas reglas que el adaptador JPA).
 */
@Repository
@Profile("!postgres")
public class EntidadesBeneficiariasRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<EntidadBeneficiaria>
    implements IEntidadesBeneficiariasRepository {

  private final IPersonasRepository personas;
  private final INecesidadesRepository necesidades;
  private final ISubcategoriasRepository subcategorias;
  private final IDonacionesIndependientesRepository independientes;
  private final IDonacionesRepository donaciones;

  /** Sin búsqueda: para los tests que solo guardan y leen. */
  public EntidadesBeneficiariasRepositoryEnMemoria() {
    this(null);
  }

  /** Solo con la búsqueda por nombre: los filtros de categoría y "ya doné" no encuentran nada. */
  public EntidadesBeneficiariasRepositoryEnMemoria(IPersonasRepository personas) {
    this(personas, null, null, null, null);
  }

  @Autowired
  public EntidadesBeneficiariasRepositoryEnMemoria(
      IPersonasRepository personas,
      INecesidadesRepository necesidades,
      ISubcategoriasRepository subcategorias,
      IDonacionesIndependientesRepository independientes,
      IDonacionesRepository donaciones) {
    this.personas = personas;
    this.necesidades = necesidades;
    this.subcategorias = subcategorias;
    this.independientes = independientes;
    this.donaciones = donaciones;
  }

  @Override
  public Tramo<EntidadBeneficiaria> buscar(CriterioEntidades criterio) {
    Map<UUID, Juridica> juridicas =
        personas.findAll().stream()
            .filter(Juridica.class::isInstance)
            .map(Juridica.class::cast)
            .collect(Collectors.toMap(Juridica::getId, j -> j));
    Map<UUID, String> razonSocial =
        juridicas.values().stream()
            .collect(Collectors.toMap(Juridica::getId, Juridica::getRazonSocial));
    FiltroOrganizaciones filtro = criterio.filtro();
    Set<UUID> conNecesidades =
        filtro.conNecesidades() == null ? Set.of() : conNecesidadesVigentes(criterio);
    Set<UUID> deLaCategoria =
        criterio.categoriaId() == null ? Set.of() : pidenDeLaCategoria(criterio);
    Set<UUID> donadas =
        criterio.donadasPor() == null ? Set.of() : asignadasA(criterio.donadasPor());
    List<EntidadBeneficiaria> filtradas =
        storage.values().stream()
            // Las públicas necesitan la jurídica para mostrar su nombre
            .filter(e -> !criterio.soloActivas() || razonSocial.containsKey(e.getId()))
            .filter(e -> !criterio.soloActivas() || e.estaActivo())
            .filter(e -> TextoBuscado.contiene(razonSocial.get(e.getId()), criterio.nombre()))
            .filter(e -> criterio.categoriaId() == null || deLaCategoria.contains(e.getId()))
            .filter(e -> criterio.donadasPor() == null || donadas.contains(e.getId()))
            .filter(e -> filtro.activa() == null || filtro.activa() == e.estaActivo())
            .filter(
                e ->
                    filtro.conNecesidades() == null
                        || filtro.conNecesidades() == conNecesidades.contains(e.getId()))
            .filter(e -> filtro.rubro() == null || delRubro(juridicas.get(e.getId()), filtro))
            .sorted(
                Comparator.comparing(
                        (EntidadBeneficiaria e) ->
                            TextoBuscado.normalizar(razonSocial.get(e.getId())),
                        Comparator.nullsLast(Comparator.<String>naturalOrder()))
                    .thenComparing(EntidadBeneficiaria::getId))
            .toList();
    return Tramo.de(filtradas, criterio.paginado());
  }

  @Override
  public Set<UUID> recibieronDe(UUID donanteId, Collection<UUID> entidadIds) {
    if (donanteId == null || entidadIds == null || entidadIds.isEmpty()) {
      return Set.of();
    }
    return asignadasA(donanteId).stream().filter(entidadIds::contains).collect(Collectors.toSet());
  }

  /** R11-43: el rubro de la jurídica, sin mayúsculas ni tildes; sin rubro, ninguno. */
  private static boolean delRubro(Juridica juridica, FiltroOrganizaciones filtro) {
    return juridica != null
        && juridica.getRubro() != null
        && TextoBuscado.normalizar(juridica.getRubro().trim())
            .equals(TextoBuscado.normalizar(filtro.rubro()));
  }

  /** R11-43: las entidades con alguna necesidad activa y no vencida hoy, de cualquier categoría. */
  private Set<UUID> conNecesidadesVigentes(CriterioEntidades criterio) {
    if (necesidades == null) {
      return Set.of();
    }
    return necesidades.findAll().stream()
        .filter(n -> n.getEntidadId() != null && n.admiteAsignaciones(criterio.hoy()))
        .map(Necesidad::getEntidadId)
        .collect(Collectors.toSet());
  }

  /** Las entidades con una necesidad activa y no vencida de una subcategoría de la categoría. */
  private Set<UUID> pidenDeLaCategoria(CriterioEntidades criterio) {
    if (necesidades == null || subcategorias == null) {
      return Set.of();
    }
    Set<UUID> suyas =
        subcategorias.findAll().stream()
            .filter(s -> criterio.categoriaId().equals(s.getCategoriaId()))
            .map(Subcategoria::getId)
            .collect(Collectors.toSet());
    return necesidades.findAll().stream()
        .filter(n -> n.getEntidadId() != null && suyas.contains(n.getSubcategoriaId()))
        .filter(n -> n.admiteAsignaciones(criterio.hoy()))
        .map(Necesidad::getEntidadId)
        .collect(Collectors.toSet());
  }

  /** Las entidades a las que quedó asignada alguna donación de ese donante, en cualquier estado. */
  private Set<UUID> asignadasA(UUID donanteId) {
    if (independientes == null || donaciones == null) {
      return Set.of();
    }
    Set<UUID> delDonante =
        donaciones.findAll().stream()
            .filter(d -> donanteId.equals(d.getDonanteId()))
            .map(Donacion::getId)
            .collect(Collectors.toSet());
    return independientes.findAll().stream()
        .filter(di -> delDonante.contains(di.getDonacionOriginalId()))
        .map(DonacionIndependiente::getEntidadBeneficiariaId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @Override
  public long contarActivas() {
    return storage.values().stream().filter(EntidadBeneficiaria::estaActivo).count();
  }
}
