package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioDonantes;
import grupo5.donaciones.models.repositories.listados.OrdenDonantes;
import grupo5.donaciones.models.repositories.listados.PersonaEnListado;
import grupo5.donaciones.models.repositories.listados.TextoBuscado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Donantes en memoria. La búsqueda cruza con las personas y, para "qué dona", con las donaciones,
 * sus ítems normalizados y el catálogo: sin esos repositorios solo se puede guardar y leer por id.
 */
@Repository
@Profile("!postgres")
public class DonantesRepositoryEnMemoria extends CrudRepositoryEnMemoria<Donante>
    implements IDonantesRepository {

  private final IPersonasRepository personas;
  private final IDonacionesRepository donaciones;
  private final IItemDonacionNormalizadoRepository itemsNormalizados;
  private final ISubcategoriasRepository subcategorias;

  /** Sin búsqueda: para los tests que solo guardan y leen. */
  public DonantesRepositoryEnMemoria() {
    this(null, null, null, null);
  }

  @Autowired
  public DonantesRepositoryEnMemoria(
      IPersonasRepository personas,
      IDonacionesRepository donaciones,
      IItemDonacionNormalizadoRepository itemsNormalizados,
      ISubcategoriasRepository subcategorias) {
    this.personas = personas;
    this.donaciones = donaciones;
    this.itemsNormalizados = itemsNormalizados;
    this.subcategorias = subcategorias;
  }

  @Override
  public Tramo<Donante> buscar(CriterioDonantes criterio) {
    Map<UUID, Persona> personaPorId =
        personas.findAll().stream().collect(Collectors.toMap(Persona::getId, Function.identity()));
    Set<UUID> deLaCategoria =
        criterio.categoriaId() == null ? Set.of() : donantesDeLaCategoria(criterio.categoriaId());

    List<Donante> filtrados =
        storage.values().stream()
            .filter(d -> criterio.activo() == null || d.estaActivo() == criterio.activo())
            .filter(d -> criterio.categoriaId() == null || deLaCategoria.contains(d.getId()))
            .filter(
                d -> {
                  Persona persona = personaPorId.get(d.personaId());
                  return persona != null && cumple(persona, d, criterio);
                })
            .sorted(orden(criterio.orden(), personaPorId))
            .toList();
    return Tramo.de(filtrados, criterio.paginado());
  }

  private static boolean cumple(Persona persona, Donante donante, CriterioDonantes criterio) {
    if (criterio.canal() != null
        && (!donante.estaActivo()
            || persona.estaAnonimizada()
            || !PersonaEnListado.tieneCanal(persona, criterio.canal()))) {
      return false;
    }
    return PersonaEnListado.esDelTipo(persona, criterio.tipo(), criterio.tipoJuridico())
        && PersonaEnListado.coincide(persona, criterio.q());
  }

  private static Comparator<Donante> orden(OrdenDonantes orden, Map<UUID, Persona> personaPorId) {
    Comparator<Donante> porAlta = Comparator.comparing(Donante::getFechaAlta);
    Comparator<Donante> principal =
        switch (orden) {
          case NUEVOS -> porAlta.reversed();
          case ANTIGUOS -> porAlta;
          case NOMBRE ->
              Comparator.comparing(
                  d ->
                      TextoBuscado.normalizar(
                          PersonaEnListado.nombreVisible(personaPorId.get(d.personaId()))));
        };
    return principal.thenComparing(Donante::getId);
  }

  /** Los donantes con alguna donación que tiene un ítem de una subcategoría de la categoría. */
  private Set<UUID> donantesDeLaCategoria(UUID categoriaId) {
    Set<UUID> subcategoriasDeLaCategoria =
        subcategorias.findAll().stream()
            .filter(s -> categoriaId.equals(s.getCategoriaId()))
            .map(Subcategoria::getId)
            .collect(Collectors.toSet());
    Set<UUID> donacionesConLaCategoria =
        itemsNormalizados.findAll().stream()
            .filter(i -> subcategoriasDeLaCategoria.contains(i.getBien().subcategoriaId()))
            .map(ItemDonacionNormalizado::getDonacionOriginalId)
            .collect(Collectors.toSet());
    return donaciones.findAll().stream()
        .filter(d -> donacionesConLaCategoria.contains(d.getId()))
        .map(Donacion::getDonanteId)
        .collect(Collectors.toSet());
  }

  @Override
  public long contarActivos() {
    return storage.values().stream().filter(Donante::estaActivo).count();
  }

  @Override
  public long contarActivosConDonaciones() {
    Set<UUID> conDonaciones = donaciones.idsDeDonantes();
    return storage.values().stream()
        .filter(d -> d.estaActivo() && conDonaciones.contains(d.getId()))
        .count();
  }
}
