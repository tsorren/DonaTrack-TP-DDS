package grupo5.donaciones.graphql.relaciones;

import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.graphql.donaciones.DonacionGraphQl;
import grupo5.donaciones.graphql.necesidades.NecesidadGraphQl;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import grupo5.donaciones.services.INecesidadesService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * {@code Donacion.necesidad} y {@code Donacion.entidad} (SPEC-11, G2; contrato de GraphQL, §4.5),
 * cargadas por lotes con una llamada al servicio por página. Las dos muestran el elemento aunque ya
 * no esté vigente o activo, y quedan nulas si la donación no cubre una necesidad o no está
 * asignada.
 *
 * <p>Solo se llega desde {@code Query.donaciones}, que ya pidió rol; la regla {@code CON_SESION}
 * repite el piso por si el método se reusa desde otro tipo (§4.6).
 */
@Controller
public class RelacionesDeDonacion {

  private final INecesidadesService necesidades;
  private final IEntidadBeneficiariaService entidades;

  public RelacionesDeDonacion(
      INecesidadesService necesidades, IEntidadBeneficiariaService entidades) {
    this.necesidades = necesidades;
    this.entidades = entidades;
  }

  @BatchMapping(typeName = "Donacion", field = "necesidad")
  @PreAuthorize(Acceso.CON_SESION)
  public Map<DonacionGraphQl, NecesidadGraphQl> necesidad(List<DonacionGraphQl> donaciones) {
    return CargaPorId.unir(
        donaciones,
        DonacionGraphQl::necesidadId,
        necesidades::buscarPublicasPorIds,
        NecesidadPublicaDTO::id,
        NecesidadGraphQl::de);
  }

  @BatchMapping(typeName = "Donacion", field = "entidad")
  @PreAuthorize(Acceso.CON_SESION)
  public Map<DonacionGraphQl, EntidadPublicaDTO> entidad(List<DonacionGraphQl> donaciones) {
    return CargaPorId.unir(
        donaciones,
        DonacionGraphQl::entidadId,
        entidades::buscarPublicasPorIds,
        EntidadPublicaDTO::id,
        Function.identity());
  }
}
