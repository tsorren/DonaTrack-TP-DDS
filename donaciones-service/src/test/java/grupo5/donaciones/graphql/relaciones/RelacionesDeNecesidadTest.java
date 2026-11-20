package grupo5.donaciones.graphql.relaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.graphql.comun.BaseSliceGraphQl;
import grupo5.donaciones.graphql.necesidades.NecesidadGraphQl;
import grupo5.donaciones.graphql.necesidades.NecesidadesResolver;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.seguridad.TokensDePrueba;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * {@code Necesidad.entidad} (SPEC-11, G2, lote {@code g2-relaciones}): la entidad que pide cada
 * necesidad, cargada por lotes con una sola lectura al servicio por página, pública como la raíz y
 * con la seguridad encendida, y sin datos de una persona (contrato de GraphQL, §4.5 y §4.6).
 */
@WebMvcTest(controllers = {NecesidadesResolver.class, RelacionesDeNecesidad.class})
class RelacionesDeNecesidadTest extends BaseSliceGraphQl {

  private static final UUID COMEDOR = UUID.fromString("00000000-0000-4000-8000-0000000000b1");
  private static final UUID HOGAR = UUID.fromString("00000000-0000-4000-8000-0000000000b2");
  private static final UUID SIN_VISTA = UUID.fromString("00000000-0000-4000-8000-0000000000b3");
  private static final UUID SUBCATEGORIA = UUID.fromString("00000000-0000-4000-8000-000000000011");

  private static final String CON_ENTIDAD =
      "{ necesidades { total elementos { id entidad { id razonSocial rubro } } } }";

  private static NecesidadPublicaDTO necesidadDe(String id, UUID entidad) {
    return new NecesidadPublicaDTO(
        UUID.fromString(id), entidad, SUBCATEGORIA, "Necesidad sintetica", 5, "RECURRENTE", null);
  }

  @BeforeEach
  void unaPaginaDeTresNecesidadesDeTresEntidades() {
    when(necesidadesService.buscarPublicas(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(
            new Pagina<>(
                List.of(
                    necesidadDe("00000000-0000-4000-8000-0000000000a1", COMEDOR),
                    necesidadDe("00000000-0000-4000-8000-0000000000a2", HOGAR),
                    necesidadDe("00000000-0000-4000-8000-0000000000a3", COMEDOR),
                    necesidadDe("00000000-0000-4000-8000-0000000000a4", SIN_VISTA)),
                4));
    when(entidadBeneficiariaService.buscarPublicasPorIds(any()))
        .thenReturn(
            List.of(
                new EntidadPublicaDTO(COMEDOR, "Comedor de ejemplo", "Comedor"),
                new EntidadPublicaDTO(HOGAR, "Hogar de ejemplo", null)));
  }

  @Test
  void unVisitanteSinTokenRecibeLaEntidadDeCadaNecesidad() throws Exception {
    consultar(CON_ENTIDAD, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.necesidades.elementos[0].entidad.id").value(COMEDOR.toString()))
        .andExpect(
            jsonPath("$.data.necesidades.elementos[0].entidad.razonSocial")
                .value("Comedor de ejemplo"))
        .andExpect(jsonPath("$.data.necesidades.elementos[0].entidad.rubro").value("Comedor"))
        .andExpect(jsonPath("$.data.necesidades.elementos[1].entidad.id").value(HOGAR.toString()))
        .andExpect(jsonPath("$.data.necesidades.elementos[1].entidad.rubro").isEmpty())
        .andExpect(jsonPath("$.data.necesidades.elementos[2].entidad.id").value(COMEDOR.toString()))
        // Una entidad que el servicio no devuelve (sin jurídica) queda nula, sin error
        .andExpect(jsonPath("$.data.necesidades.elementos[3].entidad").isEmpty());
  }

  @Test
  void conToken_tambienResponde() throws Exception {
    consultar(CON_ENTIDAD, TokensDePrueba.DONANTE)
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(
            jsonPath("$.data.necesidades.elementos[0].entidad.id").value(COMEDOR.toString()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void unaSolaLlamadaAlServicioPorPagina_conLosIdsSinRepetir() throws Exception {
    consultar(CON_ENTIDAD, null).andExpect(jsonPath("$.errors").doesNotExist());

    ArgumentCaptor<Collection<UUID>> ids = ArgumentCaptor.forClass(Collection.class);
    verify(entidadBeneficiariaService, times(1)).buscarPublicasPorIds(ids.capture());
    assertThat(ids.getValue()).containsExactlyInAnyOrder(COMEDOR, HOGAR, SIN_VISTA);
  }

  @Test
  void sinPedirLaEntidad_noLlamaAlServicio() throws Exception {
    consultar("{ necesidades { elementos { id } } }", null)
        .andExpect(jsonPath("$.errors").doesNotExist());

    verify(entidadBeneficiariaService, never()).buscarPublicasPorIds(any());
  }

  @Test
  void laEntidadNoExponeDatosDeUnaPersona() throws Exception {
    for (String campo : List.of("documento", "direccion", "mediosDeContacto", "representantes")) {
      consultar("{ necesidades { elementos { entidad { " + campo + " } } } }", null)
          .andExpect(jsonPath("$.data").doesNotExist())
          .andExpect(jsonPath("$.errors[0].extensions.classification").value("ValidationError"));
    }

    verify(necesidadesService, never())
        .buscarPublicas(any(), any(), any(), any(), anyInt(), anyInt());
    verify(entidadBeneficiariaService, never()).buscarPublicasPorIds(any());
  }

  @Test
  void laRelacionEsUnBatchMappingConLaReglaPublica() throws Exception {
    var metodo = RelacionesDeNecesidad.class.getMethod("entidad", List.class);
    BatchMapping mapeo = metodo.getAnnotation(BatchMapping.class);
    PreAuthorize regla = metodo.getAnnotation(PreAuthorize.class);

    assertThat(mapeo).isNotNull();
    assertThat(mapeo.typeName()).isEqualTo("Necesidad");
    assertThat(regla).isNotNull();
    assertThat(regla.value()).isEqualTo(Acceso.PUBLICO);
    assertThat(metodo.getGenericParameterTypes()[0].getTypeName())
        .isEqualTo("java.util.List<" + NecesidadGraphQl.class.getName() + ">");
  }
}
