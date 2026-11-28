package grupo5.donaciones.graphql.relaciones;

import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.graphql.necesidades.NecesidadGraphQl;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * {@code Necesidad.entidad} (SPEC-11, G2; contrato de GraphQL, §4.5): la vista pública de la
 * entidad que pide cada necesidad, cargada por lotes con {@link
 * IEntidadBeneficiariaService#buscarActivasPorIds}, una llamada por página. Solo las activas, como
 * {@code GET /api/entidades/publicas}: la de una entidad dada de baja queda nula. Pública, como la
 * raíz {@code necesidades}: une dos vistas públicas (§4.6).
 */
@Controller
public class RelacionesDeNecesidad {

  private final IEntidadBeneficiariaService entidades;

  public RelacionesDeNecesidad(IEntidadBeneficiariaService entidades) {
    this.entidades = entidades;
  }

  @BatchMapping(typeName = "Necesidad", field = "entidad")
  @PreAuthorize(Acceso.PUBLICO)
  public Map<NecesidadGraphQl, EntidadPublicaDTO> entidad(List<NecesidadGraphQl> necesidades) {
    return CargaPorId.unir(
        necesidades,
        NecesidadGraphQl::entidadId,
        entidades::buscarActivasPorIds,
        EntidadPublicaDTO::id,
        Function.identity());
  }
}
