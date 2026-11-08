package grupo5.cliente.cuentas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import grupo5.cliente.cuentas.SolicitudesDeEntidad.Pendiente;
import grupo5.cliente.puertos.DirectorioDeCuentas;
import grupo5.cliente.puertos.DirectorioDeCuentas.SolicitudDeEntidad;
import grupo5.cliente.puertos.DonacionesComoUsuario;
import grupo5.cliente.puertos.DonacionesComoUsuario.Juridica;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitudesDeEntidadTest {

  private static final UUID JURIDICA = UUID.randomUUID();

  private final DonacionesComoUsuario donaciones = mock(DonacionesComoUsuario.class);
  private final DirectorioDeCuentas cuentas = mock(DirectorioDeCuentas.class);
  private final SolicitudesDeEntidad solicitudes = new SolicitudesDeEntidad(donaciones, cuentas);

  SolicitudesDeEntidadTest() {
    var pendiente = new SolicitudDeEntidad("cuenta-1", JURIDICA, "rosa@ejemplo.test");
    when(cuentas.solicitudesDeEntidad()).thenReturn(List.of(pendiente));
  }

  @Test
  void lasPendientesLlevanLaRazonSocialDeSuJuridicaYElCorreoDeLaCuenta() {
    when(donaciones.juridicas()).thenReturn(List.of(new Juridica(JURIDICA, "Merendero Oeste")));

    assertThat(solicitudes.pendientes())
        .containsExactly(new Pendiente("cuenta-1", "Merendero Oeste", "rosa@ejemplo.test"));
  }

  @Test
  void sinJuridicaRegistradaLaPendienteDiceQueNoTieneRazonSocial() {
    assertThat(solicitudes.pendientes().get(0).razonSocial()).contains("sin razón social");
  }

  @Test
  void aprobarDaDeAltaLaEntidadYHabilitaLaCuentaQuitandoLaSolicitud() {
    solicitudes.aprobar("cuenta-1");

    var orden = inOrder(donaciones, cuentas);
    orden.verify(donaciones).registrarEntidad(JURIDICA);
    orden.verify(cuentas).asignarRol("cuenta-1", "ENTIDAD");
    orden.verify(cuentas).asignarPlantilla("cuenta-1", "RESPONSABLE_ENTIDAD");
    orden.verify(cuentas).quitarAtributo("cuenta-1", "solicitudEntidad");
  }

  @Test
  void unaCuentaSinSolicitudNoSeAprueba() {
    assertThatThrownBy(() -> solicitudes.aprobar("otra"))
        .isInstanceOf(NoSuchElementException.class);

    verifyNoInteractions(donaciones);
  }
}
