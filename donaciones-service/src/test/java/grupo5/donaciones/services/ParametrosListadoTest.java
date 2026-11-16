package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.Paginado;
import org.junit.jupiter.api.Test;

class ParametrosListadoTest {

  @Test
  void sinParametros_esLaPrimeraPaginaDe20_nuncaLaListaCompleta() {
    assertEquals(new Paginado(0, 20), ParametrosListado.primeraPagina().paginado());
  }

  @Test
  void conPageYSize_losUsaYCalculaElDesplazamiento() {
    Paginado paginado = new ParametrosListado(3, 25, null, null).paginado();

    assertEquals(new Paginado(3, 25), paginado);
    assertEquals(75, paginado.desplazamiento());
  }

  @Test
  void aceptaElTamanioMaximo() {
    assertEquals(
        ParametrosListado.TAMANIO_MAXIMO,
        new ParametrosListado(0, ParametrosListado.TAMANIO_MAXIMO, null, null)
            .paginado()
            .tamanio());
  }

  @Test
  void pageOSizeFueraDeRango_da400() {
    for (Integer[] invalido : new Integer[][] {{-1, null}, {null, 0}, {null, 101}}) {
      ParametrosListado listado = new ParametrosListado(invalido[0], invalido[1], null, null);
      ValidationException ex = assertThrows(ValidationException.class, listado::paginado);
      assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
    }
  }

  @Test
  void texto_recortaYTrataElVacioComoAusente() {
    assertEquals("gómez", new ParametrosListado(null, null, "  gómez ", null).texto());
    assertNull(new ParametrosListado(null, null, "   ", null).texto());
    assertNull(ParametrosListado.primeraPagina().texto());
  }

  @Test
  void texto_demasiadoLargo_da400() {
    ParametrosListado listado = new ParametrosListado(null, null, "x".repeat(101), null);

    assertThrows(ValidationException.class, listado::texto);
  }

  @Test
  void orden_sinDistinguirMayusculas_yPorDefectoSiFalta() {
    assertEquals(
        OrdenPorFecha.ANTIGUAS,
        new ParametrosListado(null, null, null, "antiguas")
            .orden(OrdenPorFecha.class, OrdenPorFecha.RECIENTES));
    assertEquals(
        OrdenPorFecha.RECIENTES,
        ParametrosListado.primeraPagina().orden(OrdenPorFecha.class, OrdenPorFecha.RECIENTES));
  }

  @Test
  void orden_desconocido_da400() {
    ParametrosListado listado = new ParametrosListado(null, null, null, "PRECIO");

    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> listado.orden(OrdenPorFecha.class, OrdenPorFecha.RECIENTES));
    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
  }
}
