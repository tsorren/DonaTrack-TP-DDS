package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.ISolicitudesEntregaRepository;
import grupo5.donaciones.services.impl.VerificadorOrigenEventos;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class VerificadorOrigenEventosTest {

  private static final String TOKEN_EXTERNO = "token-sintetico-externo";
  private static final String TOKEN_DONATRACK = "token-sintetico-donatrack";

  private ISolicitudesEntregaRepository solicitudes;
  private MockEnvironment environment;
  private VerificadorOrigenEventos verificador;

  @BeforeEach
  void setUp() {
    solicitudes = mock(ISolicitudesEntregaRepository.class);
    environment =
        new MockEnvironment()
            .withProperty("donatrack.logistica.proveedor.externo.token-vuelta", TOKEN_EXTERNO)
            .withProperty("donatrack.logistica.proveedor.donatrack.token-vuelta", TOKEN_DONATRACK);
    verificador = new VerificadorOrigenEventos(environment, solicitudes);
  }

  private UUID donacionAsignadaA(String proveedorId) {
    UUID donacionId = UUID.randomUUID();
    SolicitudEntrega solicitud = new SolicitudEntrega(donacionId, LocalDateTime.now());
    solicitud.asignarProveedor(proveedorId);
    when(solicitudes.findMasRecientePorDonacion(donacionId)).thenReturn(Optional.of(solicitud));
    return donacionId;
  }

  @Test
  void proveedorConSuTokenYDonacionPropia_esValido() {
    UUID donacionId = donacionAsignadaA("externo");

    assertTrue(verificador.esOrigenValido("externo", TOKEN_EXTERNO, List.of(donacionId)));
  }

  @Test
  void variasDonaciones_todasDebenSerDelProveedor() {
    UUID propia = donacionAsignadaA("externo");
    UUID otraPropia = donacionAsignadaA("externo");
    UUID ajena = donacionAsignadaA("donatrack");

    assertTrue(verificador.esOrigenValido("externo", TOKEN_EXTERNO, List.of(propia, otraPropia)));
    assertFalse(verificador.esOrigenValido("externo", TOKEN_EXTERNO, List.of(propia, ajena)));
  }

  @Test
  void sinIdentificarse_seRechaza() {
    UUID donacionId = donacionAsignadaA("externo");

    assertFalse(verificador.esOrigenValido(null, TOKEN_EXTERNO, List.of(donacionId)));
    assertFalse(verificador.esOrigenValido("externo", null, List.of(donacionId)));
    assertFalse(verificador.esOrigenValido("externo", "", List.of(donacionId)));
  }

  @Test
  void tokenIncorrectoOTokenDeOtroProveedor_seRechaza() {
    UUID donacionId = donacionAsignadaA("externo");

    assertFalse(verificador.esOrigenValido("externo", "otro-token", List.of(donacionId)));
    assertFalse(verificador.esOrigenValido("externo", TOKEN_DONATRACK, List.of(donacionId)));
  }

  @Test
  void proveedorSinTokenConfigurado_fallaCerrado() {
    UUID donacionId = donacionAsignadaA("sin-token");
    environment.setProperty("donatrack.logistica.proveedor.vacio.token-vuelta", "");
    UUID otraDonacion = donacionAsignadaA("vacio");

    assertFalse(verificador.esOrigenValido("sin-token", "cualquiera", List.of(donacionId)));
    assertFalse(verificador.esOrigenValido("vacio", "", List.of(otraDonacion)));
  }

  @Test
  void idConCaracteresRaros_seRechazaSinConsultarLaConfiguracion() {
    UUID donacionId = donacionAsignadaA("externo");
    environment.setProperty("donatrack.logistica.proveedor.externo.url.token-vuelta", "x");

    assertFalse(verificador.esOrigenValido("externo.url", "x", List.of(donacionId)));
  }

  @Test
  void proveedorAutenticadoPeroSobreUnaDonacionAjena_seRechaza() {
    UUID ajena = donacionAsignadaA("donatrack");

    assertFalse(verificador.esOrigenValido("externo", TOKEN_EXTERNO, List.of(ajena)));
  }

  @Test
  void donacionSinSolicitud_seRechaza() {
    UUID donacionId = UUID.randomUUID();
    when(solicitudes.findMasRecientePorDonacion(donacionId)).thenReturn(Optional.empty());

    assertFalse(verificador.esOrigenValido("externo", TOKEN_EXTERNO, List.of(donacionId)));
  }

  @Test
  void solicitudFallidaConsecuenciaDeEnviosInciertos_siguiendoElMismoProveedor_seAcepta() {
    UUID donacionId = UUID.randomUUID();
    SolicitudEntrega solicitud = new SolicitudEntrega(donacionId, LocalDateTime.now());
    solicitud.asignarProveedor("externo");
    solicitud.marcarFallida();
    when(solicitudes.findMasRecientePorDonacion(donacionId)).thenReturn(Optional.of(solicitud));

    assertTrue(verificador.esOrigenValido("externo", TOKEN_EXTERNO, List.of(donacionId)));
  }
}
