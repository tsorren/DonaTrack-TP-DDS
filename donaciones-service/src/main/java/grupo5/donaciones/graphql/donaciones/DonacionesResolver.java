package grupo5.donaciones.graphql.donaciones;

import grupo5.donaciones.graphql.comun.PaginaGraphQl;
import grupo5.donaciones.graphql.comun.PedidoDePagina;
import grupo5.donaciones.graphql.comun.TopeDePagina;
import grupo5.donaciones.seguridad.Acceso;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.ParametrosListado;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * {@code Query.donaciones} (SPEC-11, G2): la misma lectura y la misma regla que {@code GET
 * /donaciones-independientes} (D15). Es la única puerta del esquema al tipo {@code Donacion}.
 */
@Controller
public class DonacionesResolver {

  private final IDonacionesIndependientesService service;
  private final TopeDePagina tope;

  public DonacionesResolver(IDonacionesIndependientesService service, TopeDePagina tope) {
    this.service = service;
    this.tope = tope;
  }

  /**
   * Una página de donaciones independientes. El parámetro se llama {@code filtro} porque la regla
   * lee {@code #filtro.donanteId()} y {@code #filtro.entidadId()}; el esquema lo declara no nulo,
   * con {@code {}} por defecto.
   */
  @QueryMapping
  @PreAuthorize(Acceso.LISTAR_DONACIONES_INDEPENDIENTES)
  public PaginaGraphQl<DonacionGraphQl> donaciones(
      @Argument FiltroDonaciones filtro,
      @Argument Integer pagina,
      @Argument Integer tamanio,
      @Argument String orden) {
    PedidoDePagina pedido = tope.validar(pagina, tamanio);
    ParametrosListado listado =
        new ParametrosListado(
            pedido.pagina(), pedido.tamanio(), ParametrosListado.texto(filtro.texto()), orden);
    return PaginaGraphQl.de(
        service.obtenerConFiltros(filtro.deServicio(), listado),
        pedido,
        MapeoDeDonaciones::donacion);
  }
}
