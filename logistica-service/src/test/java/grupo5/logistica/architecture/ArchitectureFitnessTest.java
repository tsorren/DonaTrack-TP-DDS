package grupo5.logistica.architecture;

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
            .importPackages("grupo5.logistica");
  }

  @Test
  void entidadesDeDominio_noDebenDependerDeControladoresNiDtoNiInfraestructura() {
    noClasses()
        .that()
        .resideInAPackage("grupo5.logistica.models.entities..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "grupo5.logistica.controllers..",
            "grupo5.logistica.dto..",
            "grupo5.logistica.infrastructure..")
        .check(importedClasses);
  }

  @Test
  void controladores_debenResidirEnPaqueteControllers() {
    classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .resideInAPackage("grupo5.logistica.controllers..")
        .check(importedClasses);
  }

  @Test
  void controladores_noDebenInyectarRepositoriosDirectamente() {
    noClasses()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("grupo5.logistica.models.repositories..")
        .check(importedClasses);
  }

  @Test
  void testsUnitarios_noDebenAnotarseConSpringBootTest() {
    JavaClasses testClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.OnlyIncludeTests())
            .importPackages("grupo5.logistica");

    noClasses()
        .that()
        .haveSimpleNameEndingWith("Test")
        .should()
        .beAnnotatedWith(SpringBootTest.class)
        .check(testClasses);
  }
}
