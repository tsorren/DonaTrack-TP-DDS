package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeleccionPorPreferenciaConFallbackTest {

  @Test
  void ordenar_deberiaPonerPrimeroAlPreferidoYDespuesElRestoEnOrdenDeConfiguracion() {
    var estrategia =
        new SeleccionPorPreferenciaConFallback(List.of("donatrack", "externo", "otra"), "externo");

    assertEquals(List.of("externo", "donatrack", "otra"), estrategia.ordenar(null));
  }

  @Test
  void deberiaNormalizarEspaciosVaciosYDuplicados() {
    var estrategia =
        new SeleccionPorPreferenciaConFallback(
            List.of(" donatrack ", "", "externo", "donatrack"), " donatrack");

    assertEquals(List.of("donatrack", "externo"), estrategia.ordenar(null));
  }

  @Test
  void deberiaFallarAlConfigurar_CuandoElPreferidoNoEstaEnLaLista() {
    List<String> proveedores = List.of("donatrack");

    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SeleccionPorPreferenciaConFallback(proveedores, "externo"));
    assertTrue(e.getMessage().contains("externo"));
  }

  @Test
  void deberiaFallarAlConfigurar_CuandoNoHayProveedores() {
    List<String> proveedores = List.of(" ");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SeleccionPorPreferenciaConFallback(proveedores, "donatrack"));
  }

  @Test
  void
      cambiarProveedorPreferido_deberiaAplicarseALosSiguientesPedidosYMandarAlAnteriorAlFallback() {
    var estrategia =
        new SeleccionPorPreferenciaConFallback(
            List.of("donatrack", "externo", "otra"), "donatrack");

    estrategia.cambiarProveedorPreferido(" externo ");

    assertEquals("externo", estrategia.proveedorPreferido());
    assertEquals(List.of("externo", "donatrack", "otra"), estrategia.ordenar(null));
    assertEquals(List.of("donatrack", "externo", "otra"), estrategia.proveedoresConfigurados());
  }

  @Test
  void cambiarProveedorPreferido_deberiaRechazarUnProveedorNoConfigurado() {
    var estrategia =
        new SeleccionPorPreferenciaConFallback(List.of("donatrack", "externo"), "donatrack");

    assertThrows(ValidationException.class, () -> estrategia.cambiarProveedorPreferido("fantasma"));
    assertThrows(ValidationException.class, () -> estrategia.cambiarProveedorPreferido(" "));
    assertThrows(ValidationException.class, () -> estrategia.cambiarProveedorPreferido(null));

    assertEquals("donatrack", estrategia.proveedorPreferido());
    assertEquals(List.of("donatrack", "externo"), estrategia.ordenar(null));
  }
}
