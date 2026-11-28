package grupo5.donaciones.graphql.entidades;

import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.graphql.comun.PaginaGraphQl;
import grupo5.donaciones.graphql.comun.PedidoDePagina;
import grupo5.donaciones.graphql.comun.TopeDePagina;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import java.util.function.Function;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * {@code Query.entidades} (SPEC-11, G2): las entidades beneficiarias activas con su vista pública,
 * sobre {@link IEntidadBeneficiariaService#buscarPublicas}. Pública, sin token, como {@code GET
 * /api/entidades/publicas} (D08).
 *
 * <p>El tipo {@code Entidad} del esquema tiene los mismos campos que {@link EntidadPublicaDTO}, así
 * que el DTO se devuelve tal cual: no trae documento, dirección, contactos ni representantes (D26).
 */
@Controller
public class EntidadesResolver {

  private final IEntidadBeneficiariaService service;
  private final TopeDePagina tope;

  public EntidadesResolver(IEntidadBeneficiariaService service, TopeDePagina tope) {
    this.service = service;
    this.tope = tope;
  }

  @QueryMapping
  @PreAuthorize(Acceso.PUBLICO)
  public PaginaGraphQl<EntidadPublicaDTO> entidades(
      @Argument FiltroEntidades filtro, @Argument Integer pagina, @Argument Integer tamanio) {
    PedidoDePagina pedido = tope.validar(pagina, tamanio);
    FiltroEntidades criterio = filtro == null ? new FiltroEntidades(null, null) : filtro;
    return PaginaGraphQl.de(
        service.buscarPublicas(
            criterio.nombre(), criterio.categoria(), pedido.pagina(), pedido.tamanio()),
        pedido,
        Function.identity());
  }
}
