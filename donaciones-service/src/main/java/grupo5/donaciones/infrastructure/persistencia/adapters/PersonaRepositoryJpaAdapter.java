package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.HumanaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.JuridicaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.PersonaEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.PersonasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataPersonaRepository;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioPersonas;
import grupo5.donaciones.models.repositories.listados.Tramo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class PersonaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Persona, PersonaEntity, SpringDataPersonaRepository>
    implements IPersonasRepository {

  private final PersonasPersistenciaMapper mapper;

  @PersistenceContext private EntityManager em;

  public PersonaRepositoryJpaAdapter(
      SpringDataPersonaRepository springDataRepo, PersonasPersistenciaMapper mapper) {
    super(springDataRepo, domain -> null, mapper::toDomain);
    this.mapper = mapper;
  }

  // Guardar una persona toca varias tablas (dirección, persona, medios, representantes): va en una
  // sola transacción.
  @Override
  @Transactional
  public Persona save(Persona persona) {
    PersonaEntity guardada =
        switch (persona) {
          case Humana humana -> guardarHumana(humana);
          case Juridica juridica -> guardarJuridica(juridica);
        };
    return mapper.toDomain(guardada);
  }

  @Override
  @Transactional
  public List<Persona> saveAll(List<Persona> personas) {
    return personas.stream().map(this::save).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Tramo<Persona> buscar(CriterioPersonas criterio) {
    ConsultaListado consulta =
        SqlPersona.filtrar(
                new ConsultaListado("FROM donaciones.persona p" + SqlPersona.UNIONES),
                criterio.q(),
                criterio.tipo(),
                criterio.tipoJuridico())
            .siHay(criterio.ids(), "p.id IN (:ids)", "ids");
    List<UUID> ids =
        consulta.ids(em, "p.id", SqlPersona.NOMBRE_VISIBLE + ", p.id", criterio.paginado());
    List<Persona> pagina = ConsultaListado.enOrden(ids, findAllById(ids), Persona::getId);
    return new Tramo<>(pagina, consulta.total(em));
  }

  /** Una consulta para todas; los medios y representantes llegan de a lotes (batch size). */
  @Override
  @Transactional(readOnly = true)
  public List<Persona> findAllById(Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return springDataRepo.findAllById(ids).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<Persona> findByDocumento(String documento) {
    if (documento == null) {
      return Optional.empty();
    }
    return springDataRepo.findFirstByNroDocumento(documento).map(mapper::toDomain);
  }

  @Override
  public List<Persona> findAllByNumeroDocumento(String numero) {
    if (numero == null || numero.isBlank()) {
      return List.of();
    }
    return springDataRepo.findByNroDocumento(numero.trim()).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Persona> findByCorreo(String direccionCorreo) {
    if (direccionCorreo == null || direccionCorreo.isBlank()) {
      return List.of();
    }
    return springDataRepo.findByCorreo(direccionCorreo).stream().map(mapper::toDomain).toList();
  }

  private HumanaEntity guardarHumana(Humana humana) {
    // Se parte de la entidad existente para conservar humana.representante_de
    HumanaEntity existente =
        springDataRepo.findById(humana.getId()).map(HumanaEntity.class::cast).orElse(null);
    return (HumanaEntity) springDataRepo.save(mapper.toEntity(humana, existente));
  }

  private JuridicaEntity guardarJuridica(Juridica juridica) {
    validarQueNingunRepresentanteEsteEnOtraJuridica(juridica);

    JuridicaEntity existente =
        springDataRepo.findById(juridica.getId()).map(JuridicaEntity.class::cast).orElse(null);
    JuridicaEntity entidad =
        (JuridicaEntity) springDataRepo.save(mapper.toEntity(juridica, existente));
    // La FK humana.representante_de apunta a esta jurídica: tiene que existir antes
    springDataRepo.flush();

    Set<UUID> vigentes =
        juridica.getRepresentantes().stream().map(Humana::getId).collect(Collectors.toSet());
    // Los que dejaron de representarla se desligan, no se borran
    new ArrayList<>(entidad.getRepresentantes())
        .stream()
            .filter(previo -> !vigentes.contains(previo.getId()))
            .forEach(previo -> previo.setRepresentanteDe(null));

    List<HumanaEntity> representantes = new ArrayList<>();
    for (Humana representante : juridica.getRepresentantes()) {
      HumanaEntity humana = guardarHumana(representante);
      humana.setRepresentanteDe(entidad);
      representantes.add(humana);
    }
    entidad.getRepresentantes().clear();
    entidad.getRepresentantes().addAll(representantes);
    return entidad;
  }

  private void validarQueNingunRepresentanteEsteEnOtraJuridica(Juridica juridica) {
    boolean conflicto =
        juridica.getRepresentantes().stream()
            .anyMatch(
                r ->
                    springDataRepo.contarRepresentacionesEnOtraJuridica(r.getId(), juridica.getId())
                        > 0);
    if (conflicto) {
      throw new BusinessStateException(ErrorCatalog.JURIDICA_REPRESENTANTE_EN_OTRA_JURIDICA);
    }
  }
}
