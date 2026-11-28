package grupo5.donaciones.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import grupo5.donaciones.seguridad.Acceso;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Reglas del paquete {@code graphql} (SPEC-11, §4): los resolvers no tocan repositorios, cada
 * método que resuelve un campo lleva su {@code @PreAuthorize} con una constante de {@link Acceso} y
 * el dominio no conoce GraphQL.
 */
class GraphQlArquitecturaTest {

  private static final String GRAPHQL = "grupo5.donaciones.graphql..";

  /** Los resolvers leen por los servicios de aplicación, nunca por los repositorios. */
  static final ArchRule RESOLVERS_SIN_REPOSITORIOS =
      noClasses()
          .that()
          .resideInAPackage(GRAPHQL)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "grupo5.donaciones.models.repositories..",
              "grupo5.donaciones.infrastructure.persistencia..")
          .because("los resolvers leen por los servicios de aplicación (SPEC-11, §4)");

  /**
   * Todo método con {@code @QueryMapping}, {@code @SchemaMapping} o {@code @BatchMapping} lleva
   * {@code @PreAuthorize} en el método, también cuando es público. Vale para todo el servicio: un
   * resolver fuera del paquete {@code graphql} tampoco puede quedar sin regla. Se admite la regla
   * vacía porque los resolvers llegan con los lotes de cada raíz.
   */
  static final ArchRule REGLA_EN_CADA_RESOLVER =
      methods()
          .that()
          .areMetaAnnotatedWith(SchemaMapping.class)
          .or()
          .areAnnotatedWith(BatchMapping.class)
          .should()
          .beAnnotatedWith(PreAuthorize.class)
          .andShould(usarUnaConstanteDeAcceso())
          .because("la ruta de GraphQL es pública y cada resolver decide su acceso (SPEC-11, §4)")
          .allowEmptyShould(true);

  /** Los resolvers viven en el paquete {@code graphql}, no en los controladores REST. */
  static final ArchRule RESOLVERS_EN_SU_PAQUETE =
      methods()
          .that()
          .areMetaAnnotatedWith(SchemaMapping.class)
          .or()
          .areAnnotatedWith(BatchMapping.class)
          .should()
          .beDeclaredInClassesThat()
          .resideInAPackage(GRAPHQL)
          .allowEmptyShould(true);

  /** El dominio y los servicios de aplicación no conocen GraphQL. */
  static final ArchRule DOMINIO_SIN_GRAPHQL =
      noClasses()
          .that()
          .resideInAnyPackage("grupo5.donaciones.models..", "grupo5.donaciones.services..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(GRAPHQL, "org.springframework.graphql..", "graphql..");

  private static JavaClasses clasesDelServicio;

  @BeforeAll
  static void importar() {
    clasesDelServicio =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("grupo5.donaciones");
  }

  @Test
  void losResolversNoAccedenARepositorios() {
    RESOLVERS_SIN_REPOSITORIOS.check(clasesDelServicio);
  }

  @Test
  void cadaResolverLlevaSuReglaDeAcceso() {
    REGLA_EN_CADA_RESOLVER.check(clasesDelServicio);
  }

  @Test
  void losResolversVivenEnElPaqueteGraphql() {
    RESOLVERS_EN_SU_PAQUETE.check(clasesDelServicio);
  }

  @Test
  void elDominioNoConoceGraphQl() {
    DOMINIO_SIN_GRAPHQL.check(clasesDelServicio);
  }

  // La regla vacía se admite, así que se comprueba que detecta lo que tiene que detectar

  @Test
  void laReglaDeAccesoDetectaUnResolverSinPreAuthorize() {
    JavaClasses clases = new ClassFileImporter().importClasses(ResolverSinRegla.class);

    assertThat(REGLA_EN_CADA_RESOLVER.evaluate(clases).hasViolation()).isTrue();
  }

  @Test
  void laReglaDeAccesoDetectaUnaExpresionQueNoEsDeAcceso() {
    JavaClasses clases = new ClassFileImporter().importClasses(ResolverConExpresionSuelta.class);

    assertThat(REGLA_EN_CADA_RESOLVER.evaluate(clases).hasViolation()).isTrue();
  }

  @Test
  void laReglaDeAccesoAceptaUnResolverConSuConstante() {
    JavaClasses clases = new ClassFileImporter().importClasses(ResolverConRegla.class);

    assertThat(REGLA_EN_CADA_RESOLVER.evaluate(clases).hasViolation()).isFalse();
  }

  private static ArchCondition<JavaMethod> usarUnaConstanteDeAcceso() {
    Set<String> constantes = constantesDeAcceso();
    return new ArchCondition<>("use a constant of Acceso in @PreAuthorize") {
      @Override
      public void check(JavaMethod metodo, ConditionEvents eventos) {
        boolean esConstante =
            metodo
                .tryGetAnnotationOfType(PreAuthorize.class)
                .map(regla -> constantes.contains(regla.value()))
                .orElse(false);
        eventos.add(
            new SimpleConditionEvent(
                metodo,
                esConstante,
                metodo.getFullName() + " no usa una constante de Acceso en @PreAuthorize"));
      }
    };
  }

  private static Set<String> constantesDeAcceso() {
    return Arrays.stream(Acceso.class.getDeclaredFields())
        .filter(campo -> Modifier.isStatic(campo.getModifiers()))
        .filter(campo -> campo.getType() == String.class)
        .map(GraphQlArquitecturaTest::valor)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String valor(Field campo) {
    try {
      return (String) campo.get(null);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  static class ResolverSinRegla {
    @QueryMapping
    String entidades() {
      return "";
    }
  }

  static class ResolverConExpresionSuelta {
    @BatchMapping
    @PreAuthorize("hasRole('DONANTE')")
    String entidad() {
      return "";
    }
  }

  static class ResolverConRegla {
    @SchemaMapping(typeName = "Necesidad")
    @PreAuthorize(Acceso.PUBLICO)
    String entidad() {
      return "";
    }
  }
}
