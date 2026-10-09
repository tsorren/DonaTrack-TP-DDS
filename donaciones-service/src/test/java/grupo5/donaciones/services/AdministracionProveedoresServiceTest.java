package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.logistica.ProveedorLogisticaDTO;
import grupo5.donaciones.infrastructure.logistica.ProveedoresLogistica;
import grupo5.donaciones.services.impl.AdministracionProveedoresService;
import grupo5.donaciones.services.impl.SeleccionPorPreferenciaConFallback;
import grupo5.donaciones.services.logistica.IProveedorLogistica;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

class AdministracionProveedoresServiceTest {

  private SeleccionPorPreferenciaConFallback preferencia;
  private AdministracionProveedoresService service;

  @BeforeEach
  void setUp() {
    preferencia =
        new SeleccionPorPreferenciaConFallback(
            List.of("donatrack", "externo", "sin-adapter"), "donatrack");
    IProveedorLogistica donatrack = Mockito.mock(IProveedorLogistica.class);
    Mockito.when(donatrack.id()).thenReturn("donatrack");
    IProveedorLogistica externo = Mockito.mock(IProveedorLogistica.class);
    Mockito.when(externo.id()).thenReturn("externo");
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("donatrack.logistica.proveedor.donatrack.transporte", "amqp")
            .withProperty("donatrack.logistica.proveedor.externo.transporte", " HTTP ");
    service =
        new AdministracionProveedoresService(
            preferencia, new ProveedoresLogistica(List.of(donatrack, externo)), environment);
  }

  @Test
  void listarProveedores_deberiaMostrarTransporteDisponibilidadYPreferido() {
    List<ProveedorLogisticaDTO> lista = service.listarProveedores();

    assertEquals(
        List.of(
            new ProveedorLogisticaDTO("donatrack", "amqp", true, true),
            new ProveedorLogisticaDTO("externo", "http", true, false),
            new ProveedorLogisticaDTO("sin-adapter", "", false, false)),
        lista);
  }

  @Test
  void cambiarProveedorPreferido_deberiaActualizarLaEstrategiaYDevolverLaListaNueva() {
    List<ProveedorLogisticaDTO> lista = service.cambiarProveedorPreferido("externo");

    assertEquals("externo", preferencia.proveedorPreferido());
    assertTrue(
        lista.stream().filter(p -> p.id().equals("externo")).findFirst().orElseThrow().preferido());
    assertFalse(
        lista.stream()
            .filter(p -> p.id().equals("donatrack"))
            .findFirst()
            .orElseThrow()
            .preferido());
  }

  @Test
  void cambiarProveedorPreferido_conUnProveedorNoConfigurado_noCambiaNada() {
    assertThrows(ValidationException.class, () -> service.cambiarProveedorPreferido("fantasma"));

    assertEquals("donatrack", preferencia.proveedorPreferido());
  }
}
