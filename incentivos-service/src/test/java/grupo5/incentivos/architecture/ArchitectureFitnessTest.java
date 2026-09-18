package grupo5.incentivos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureFitnessTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setUp() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("grupo5.incentivos");
  }

  @Test
  void entidadesDeDominio_noDebenDependerDeControladoresNiDtoNiInfraestructura() {
    noClasses()
        .that()
        .resideInAPackage("grupo5.incentivos.models.entities..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "grupo5.incentivos.controllers..",
            "grupo5.incentivos.dto..",
            "grupo5.incentivos.infrastructure..")
        .check(importedClasses);
  }

  @Test
  void controladores_debenResidirEnPaqueteControllers() {
    classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .resideInAPackage("grupo5.incentivos.controllers..")
        .check(importedClasses);
  }

  @Test
  void controladores_noDebenInyectarRepositoriosDirectamente() {
    noClasses()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("grupo5.incentivos.models.repositories..")
        .check(importedClasses);
  }

  @Test
  void testsUnitarios_noDebenAnotarseConSpringBootTest() {
    JavaClasses testClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.OnlyIncludeTests())
            .importPackages("grupo5.incentivos");

    noClasses()
        .that()
        .haveSimpleNameEndingWith("Test")
        .and()
        .doNotHaveSimpleName("IncentivosServiceApplicationTest")
        .should()
        .beAnnotatedWith(SpringBootTest.class)
        .check(testClasses);
  }
}
