package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioEntidades;
import grupo5.donaciones.models.repositories.listados.TextoBuscado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/** Entidades en memoria. La búsqueda por nombre cruza con las jurídicas de las personas. */
@Repository
@Profile("!postgres")
public class EntidadesBeneficiariasRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<EntidadBeneficiaria>
    implements IEntidadesBeneficiariasRepository {

  private final IPersonasRepository personas;

  /** Sin búsqueda: para los tests que solo guardan y leen. */
  public EntidadesBeneficiariasRepositoryEnMemoria() {
    this(null);
  }

  @Autowired
  public EntidadesBeneficiariasRepositoryEnMemoria(IPersonasRepository personas) {
    this.personas = personas;
  }

  @Override
  public Tramo<EntidadBeneficiaria> buscar(CriterioEntidades criterio) {
    Map<UUID, String> razonSocial =
        personas.findAll().stream()
            .filter(Juridica.class::isInstance)
            .map(Juridica.class::cast)
            .collect(Collectors.toMap(Juridica::getId, Juridica::getRazonSocial));
    List<EntidadBeneficiaria> filtradas =
        storage.values().stream()
            // Las públicas necesitan la jurídica para mostrar su nombre
            .filter(e -> !criterio.soloActivas() || razonSocial.containsKey(e.getId()))
            .filter(e -> !criterio.soloActivas() || e.estaActivo())
            .filter(e -> TextoBuscado.contiene(razonSocial.get(e.getId()), criterio.nombre()))
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
  public long contarActivas() {
    return storage.values().stream().filter(EntidadBeneficiaria::estaActivo).count();
  }
}
