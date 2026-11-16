package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.MotivoReporte;
import grupo5.logistica.models.entities.entregas.ReporteRecepcion;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** HB-17: remito y reporte al confirmar una entrega (DTI-29). */
class RemitoYReporteDeRecepcionTest {

  private static final List<String> FOTOS = List.of("https://storage.donatrack.org/f-1.jpg");

  private static Entrega enTraslado() {
    Localidad lanus = new Localidad("Lanus", new Provincia("Buenos Aires", new Pais("Argentina")));
    Entrega entrega =
        new Entrega(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new Direccion("Calle Falsa", 123, null, null, "1824", lanus),
            10f,
            1f);
    entrega.iniciarRuta("Chofer");
    return entrega;
  }

  @Test
  void confirmaConRemitoYReporteYElEventoLlevaSoloElReporte() {
    Entrega entrega = enTraslado();
    ReporteRecepcion reporte = new ReporteRecepcion(MotivoReporte.INCOMPLETA, null, 12.5);

    entrega.confirmarEntrega("Comedor", FOTOS, "  R-0001-123  ", reporte);

    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstadoActual());
    assertEquals("R-0001-123", entrega.getNumeroRemito());
    assertEquals(reporte, entrega.getReporteRecepcion());
    EntregaConfirmada evento =
        assertInstanceOf(EntregaConfirmada.class, entrega.getDomainEvents().getFirst());
    assertEquals(reporte, evento.getReporte());
  }

  @Test
  void confirmarSinRemitoNiReporteSigueAndando() {
    Entrega entrega = enTraslado();

    entrega.confirmarEntrega("Comedor", FOTOS);

    assertNull(entrega.getNumeroRemito());
    assertNull(entrega.getReporteRecepcion());
    assertNull(((EntregaConfirmada) entrega.getDomainEvents().getFirst()).getReporte());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "12345678901234567890123456789012345678901"})
  void rechazaRemitoVacioOMasLargoQue40YNoCambiaElEstado(String remito) {
    Entrega entrega = enTraslado();

    assertThrows(
        ValidationException.class, () -> entrega.confirmarEntrega("Comedor", FOTOS, remito, null));
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstadoActual());
    assertTrue(entrega.getDomainEvents().isEmpty());
  }

  @Test
  void aceptaRemitoDe40Caracteres() {
    Entrega entrega = enTraslado();
    String remito = "1234567890123456789012345678901234567890";

    entrega.confirmarEntrega("Comedor", FOTOS, remito, null);

    assertEquals(remito, entrega.getNumeroRemito());
  }

  @Test
  void reporteValidoNormalizaElDetalle() {
    assertEquals(
        "Faltan dos cajas",
        new ReporteRecepcion(MotivoReporte.DANADA, "  Faltan dos cajas ", null).detalle());
    assertNull(new ReporteRecepcion(MotivoReporte.DANADA, "   ", null).detalle());
    String detalleMaximo = "x".repeat(500);
    assertEquals(
        detalleMaximo, new ReporteRecepcion(MotivoReporte.OTRO, detalleMaximo, null).detalle());
  }

  @Test
  void reporteRechazaDatosInvalidos() {
    String detalleLargo = "x".repeat(501);
    assertThrows(ValidationException.class, () -> new ReporteRecepcion(null, "detalle", null));
    assertThrows(
        ValidationException.class, () -> new ReporteRecepcion(MotivoReporte.OTRO, " ", null));
    assertThrows(
        ValidationException.class,
        () -> new ReporteRecepcion(MotivoReporte.DANADA, detalleLargo, null));
    assertThrows(
        ValidationException.class,
        () -> new ReporteRecepcion(MotivoReporte.INCOMPLETA, null, null));
    assertThrows(
        ValidationException.class, () -> new ReporteRecepcion(MotivoReporte.INCOMPLETA, null, 0.0));
    assertThrows(
        ValidationException.class, () -> new ReporteRecepcion(MotivoReporte.DANADA, null, -1.0));
    assertThrows(
        ValidationException.class,
        () -> new ReporteRecepcion(MotivoReporte.DANADA, null, Double.NaN));
  }

  @Test
  void reconstituirRecepcionHidrataSinValidar() {
    Entrega entrega = enTraslado();
    ReporteRecepcion reporte = new ReporteRecepcion(MotivoReporte.DANADA, null, null);

    entrega.reconstituirRecepcion("R-9", reporte);

    assertEquals("R-9", entrega.getNumeroRemito());
    assertEquals(reporte, entrega.getReporteRecepcion());
  }
}
