package grupo5.donaciones.graphql.donaciones;

import static org.assertj.core.api.Assertions.assertThat;

import grupo5.donaciones.controllers.impl.DonacionesIndependientesController;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.services.ParametrosListado;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Paridad entre {@code GET /donaciones-independientes} y {@code Query.donaciones} (SPEC-11, D15):
 * los dos métodos llevan la misma constante de {@link Acceso}, y el parámetro {@code filtro} de los
 * dos tiene lo que la regla lee ({@code donanteId()} y {@code entidadId()}).
 */
class ParidadConRestTest {

  private static Method listadoRest() throws NoSuchMethodException {
    return DonacionesIndependientesController.class.getMethod(
        "obtenerTodas", FiltroDonacionesIndependientes.class, ParametrosListado.class);
  }

  private static Method consultaGraphQl() {
    return Arrays.stream(DonacionesResolver.class.getDeclaredMethods())
        .filter(metodo -> metodo.isAnnotationPresent(QueryMapping.class))
        .filter(metodo -> metodo.getName().equals("donaciones"))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void elListadoRestYLaConsultaGraphQlUsanLaMismaConstanteDeAcceso() throws Exception {
    String rest = listadoRest().getAnnotation(PreAuthorize.class).value();
    String graphQl = consultaGraphQl().getAnnotation(PreAuthorize.class).value();

    assertThat(rest).isEqualTo(Acceso.LISTAR_DONACIONES_INDEPENDIENTES);
    assertThat(graphQl).isEqualTo(Acceso.LISTAR_DONACIONES_INDEPENDIENTES);
  }

  @Test
  void laReglaEsLaQueTeniaElListadoRest() {
    assertThat(Acceso.LISTAR_DONACIONES_INDEPENDIENTES)
        .isEqualTo(
            "hasRole('ADMIN') or hasAuthority('VER_DONACIONES') and ("
                + "hasRole('DONANTE') and principal.puedeOperarSobre(#filtro.donanteId()) or "
                + "hasRole('ENTIDAD') and principal.puedeOperarSobre(#filtro.entidadId()))");
  }

  @Test
  void losDosMetodosLlamanFiltroAlParametroQueLeeLaRegla() throws Exception {
    assertThat(nombres(listadoRest())).contains("filtro");
    assertThat(nombres(consultaGraphQl())).contains("filtro");
    Class<?> filtroGraphQl =
        Arrays.stream(consultaGraphQl().getParameters())
            .filter(parametro -> parametro.getName().equals("filtro"))
            .findFirst()
            .orElseThrow()
            .getType();
    assertThat(filtroGraphQl.getMethod("donanteId").getReturnType())
        .isEqualTo(FiltroDonacionesIndependientes.class.getMethod("donanteId").getReturnType());
    assertThat(filtroGraphQl.getMethod("entidadId").getReturnType())
        .isEqualTo(FiltroDonacionesIndependientes.class.getMethod("entidadId").getReturnType());
  }

  @Test
  void elFiltroDeGraphQlTieneTodosLosFiltrosDeRestConSuTipoMasElTexto() throws Exception {
    for (RecordComponent componente : FiltroDonacionesIndependientes.class.getRecordComponents()) {
      assertThat(FiltroDonaciones.class.getMethod(componente.getName()).getGenericReturnType())
          .as(componente.getName())
          .isEqualTo(componente.getGenericType());
    }
    assertThat(FiltroDonaciones.class.getMethod("texto").getReturnType()).isEqualTo(String.class);
  }

  private static Set<String> nombres(Method metodo) {
    return Arrays.stream(metodo.getParameters())
        .map(java.lang.reflect.Parameter::getName)
        .collect(Collectors.toSet());
  }
}
