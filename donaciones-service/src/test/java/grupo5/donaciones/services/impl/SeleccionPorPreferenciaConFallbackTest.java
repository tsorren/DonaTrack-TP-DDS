package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.*;

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
}
