package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonantesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.EntidadesBeneficiariasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.PersonasPersistenciaMapper;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Paso 4: roles Donante y EntidadBeneficiaria, con el id de su persona (#892). */
@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  PersonasPersistenciaMapper.class,
  DireccionesPersistenciaMapper.class,
  PersonaRepositoryJpaAdapter.class,
  DonantesPersistenciaMapper.class,
  DonanteRepositoryJpaAdapter.class,
  EntidadesBeneficiariasPersistenciaMapper.class,
  EntidadBeneficiariaRepositoryJpaAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class RolesRepositoriesJpaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private IPersonasRepository personas;
  @Autowired private IDonantesRepository donantes;
  @Autowired private IEntidadesBeneficiariasRepository entidades;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    // Con RESTRICT, los roles se borran antes que las personas
    donantes.deleteAll();
    entidades.deleteAll();
    personas.deleteAll();
    jdbc.update("DELETE FROM donaciones.direccion");
  }

  @Test
  void guardarYLeer_donante_conservaElIdDeSuPersonaYQueEstaActivo() {
    Humana juan = PersonaMother.juanPerez();
    personas.save(juan);

    donantes.save(new Donante(juan.getId()));
    Donante leido = donantes.findById(juan.getId()).orElseThrow();

    assertEquals(juan.getId(), leido.personaId());
    assertTrue(leido.estaActivo());
  }

  @Test
  void guardar_donanteDadoDeBaja_persisteLaBajaYSePuedeReactivar() {
    Humana juan = PersonaMother.juanPerez();
    personas.save(juan);
    Donante donante = new Donante(juan.getId());
    donantes.save(donante);

    donante.darDeBaja();
    donantes.save(donante);
    Donante deBaja = donantes.findById(juan.getId()).orElseThrow();
    assertFalse(deBaja.estaActivo());

    deBaja.reactivar();
    donantes.save(deBaja);
    assertTrue(donantes.findById(juan.getId()).orElseThrow().estaActivo());
    assertEquals(1, donantes.count());
  }

  @Test
  void guardarYLeer_entidadBeneficiaria_conservaElIdDeSuJuridicaYSuEstado() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    personas.save(ong);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(ong.getId());
    entidad.darDeBaja();

    entidades.save(entidad);
    EntidadBeneficiaria leida = entidades.findById(ong.getId()).orElseThrow();

    assertEquals(ong.getId(), leida.juridicaId());
    assertFalse(leida.estaActivo());
  }

  @Test
  void guardar_donanteSinPersona_fallaPorClaveForanea() {
    Donante huerfano = new Donante(UUID.randomUUID());

    assertThrows(DataIntegrityViolationException.class, () -> donantes.save(huerfano));
  }

  @Test
  void guardar_entidadSobreUnaHumana_fallaPorClaveForanea() {
    Humana juan = PersonaMother.juanPerez();
    personas.save(juan);
    EntidadBeneficiaria sobreHumana = new EntidadBeneficiaria(juan.getId());

    assertThrows(DataIntegrityViolationException.class, () -> entidades.save(sobreHumana));
  }

  @Test
  void borrar_personaConRolDeDonante_loRechazaLaBase() {
    Humana juan = PersonaMother.juanPerez();
    personas.save(juan);
    donantes.save(new Donante(juan.getId()));

    assertThrows(DataIntegrityViolationException.class, () -> personas.delete(juan));
    assertTrue(personas.findById(juan.getId()).isPresent());
  }

  @Test
  void borrar_juridicaQueEsEntidad_loRechazaLaBase() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    personas.save(ong);
    entidades.save(new EntidadBeneficiaria(ong.getId()));
    UUID id = ong.getId();

    // Por SQL: borrar por JPA una jurídica con representantes falla antes, en Hibernate
    // (hallazgo anotado en la bitácora). Acá interesa la restricción de la base.
    assertThrows(
        DataIntegrityViolationException.class,
        () -> jdbc.update("DELETE FROM donaciones.persona WHERE id = ?", id));
    assertTrue(entidades.findById(ong.getId()).isPresent());
  }
}
