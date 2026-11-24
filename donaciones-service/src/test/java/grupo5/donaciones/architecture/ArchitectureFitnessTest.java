package grupo5.donaciones.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureFitnessTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setUp() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("grupo5.donaciones");
  }

  @Test
  void entidadesDeDominio_noDebenDependerDeControladoresNiInfraestructura() {
    noClasses()
        .that()
        .resideInAPackage("grupo5.donaciones.models.entities..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("grupo5.donaciones.controllers..", "grupo5.donaciones.infrastructure..")
        .check(importedClasses);
  }

  @Test
  void controladores_debenResidirEnPaqueteControllers() {
    classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .resideInAPackage("grupo5.donaciones.controllers..")
        .check(importedClasses);
  }

  @Test
  void controladores_noDebenInyectarRepositoriosDirectamente() {
    noClasses()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("grupo5.donaciones.models.repositories..")
        .check(importedClasses);
  }

  @Test
  void testsUnitarios_noDebenAnotarseConSpringBootTest() {
    JavaClasses testClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.OnlyIncludeTests())
            .importPackages("grupo5.donaciones");

    noClasses()
        .that()
        .haveSimpleNameEndingWith("Test")
        .and()
        .doNotHaveSimpleName("DonacionesServiceApplicationTest")
        .should()
        .beAnnotatedWith(SpringBootTest.class)
        .check(testClasses);
  }

  /**
   * Con réplicas, una sola corre cada tarea programada (E7-S01d-1). La excepción es el reintento
   * del outbox en memoria: cada réplica tiene su propio store y no hay trabajo compartido que
   * proteger.
   */
  @Test
  void tareasProgramadas_debenTenerSchedulerLock() {
    methods()
        .that()
        .areAnnotatedWith(Scheduled.class)
        .and()
        .areDeclaredInClassesThat()
        .doNotHaveFullyQualifiedName("grupo5.donaciones.infrastructure.outbox.OutboxRetryScheduler")
        .should()
        .beAnnotatedWith(SchedulerLock.class)
        .check(importedClasses);
  }
}
