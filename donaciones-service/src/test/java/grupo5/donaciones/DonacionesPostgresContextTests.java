package grupo5.donaciones;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.config.AdminConstantes;
import grupo5.donaciones.infrastructure.persistencia.adapters.CategoriaRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.adapters.PersonaRepositoryJpaAdapter;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Arranca la aplicación completa con el perfil {@code postgres} (el de producción) contra un
 * Postgres real: Flyway, {@code ddl-auto=validate}, el cableado entre todos los beans y los
 * seeders. Los tests de cada adapter prueban cada pieza por separado; este detecta un error de
 * cableado entre ellas antes de que aparezca en un entorno levantado.
 */
@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers
@DisabledIfDockerUnavailable
class DonacionesPostgresContextTests {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  // application-postgres.properties exige SPRING_DATASOURCE_PASSWORD, sin valor por defecto
  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private IPersonasRepository personas;
  @Autowired private ICategoriasRepository categorias;

  @Test
  void contextLoads_conLosRepositoriosJpaYLosSeedersEscribiendoEnPostgres() {
    assertInstanceOf(PersonaRepositoryJpaAdapter.class, personas);
    assertInstanceOf(CategoriaRepositoryJpaAdapter.class, categorias);
    // AdminSeeder y CatalogDataInitializer corrieron contra la base real
    assertTrue(personas.findByDocumento(AdminConstantes.DOCUMENTO_ADMIN).isPresent());
    assertFalse(categorias.findAll().isEmpty());
  }
}
