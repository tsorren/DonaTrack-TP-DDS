package grupo5.common.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CommonArchitectureTest {

  private static JavaClasses commonClasses;

  @BeforeAll
  static void setUp() {
    commonClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("grupo5.common");
  }

  @Test
  void commonLib_noDebeDependerDePaquetesDeMicroservicios() {
    noClasses()
        .that()
        .resideInAPackage("grupo5.common..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "grupo5.donaciones..",
            "grupo5.incentivos..",
            "grupo5.logistica..",
            "grupo5.notificaciones..")
        .check(commonClasses);
  }
}
