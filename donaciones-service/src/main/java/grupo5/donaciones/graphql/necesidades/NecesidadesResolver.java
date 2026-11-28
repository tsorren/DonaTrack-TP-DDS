package grupo5.donaciones.graphql.necesidades;

import grupo5.donaciones.graphql.comun.PaginaGraphQl;
import grupo5.donaciones.graphql.comun.PedidoDePagina;
import grupo5.donaciones.graphql.comun.TopeDePagina;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.services.INecesidadesService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * La consulta raíz {@code necesidades} (SPEC-11, G2): las necesidades vigentes, pública como {@code
 * GET /api/necesidades/publicas} (contrato de GraphQL, §4.6). Valida la página contra el tope y lee
 * por {@link INecesidadesService#buscarPublicas}, que pagina en el repositorio.
 */
@Controller
public class NecesidadesResolver {

  private final INecesidadesService necesidades;
  private final TopeDePagina tope;

  public NecesidadesResolver(INecesidadesService necesidades, TopeDePagina tope) {
    this.necesidades = necesidades;
    this.tope = tope;
  }

  @QueryMapping
  @PreAuthorize(Acceso.PUBLICO)
  public PaginaGraphQl<NecesidadGraphQl> necesidades(
      @Argument FiltroNecesidadesGraphQl filtro,
      @Argument Integer pagina,
      @Argument Integer tamanio) {
    PedidoDePagina pedido = tope.validar(pagina, tamanio);
    return PaginaGraphQl.de(
        necesidades.buscarPublicas(
            filtro.entidad(),
            filtro.categoria(),
            filtro.subcategoria(),
            filtro.tipo(),
            pedido.pagina(),
            pedido.tamanio()),
        pedido,
        NecesidadGraphQl::de);
  }
}
