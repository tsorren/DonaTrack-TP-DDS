package grupo5.logistica.infrastructure;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.planificacion.PlanificadorDeRutas;
import grupo5.logistica.models.entities.rutas.PlanificacionSolicitada;
import grupo5.logistica.models.entities.rutas.RespuestaPlanificacion;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class ProveedorExternoPlanificacionSimuladoTest {

  @Test
  void calculaYEnviaElCallbackSinAsignarLaRutaALaEntrega() {
    PlanificadorDeRutas planificador = mock(PlanificadorDeRutas.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ProveedorExternoPlanificacionSimulado proveedor =
        new ProveedorExternoPlanificacionSimulado(planificador, restTemplate);
    Entrega entrega = crearEntrega();
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");
    UUID solicitudId = UUID.randomUUID();
    PlanificacionSolicitada solicitud =
        new PlanificacionSolicitada(
            solicitudId,
            LocalDate.now(),
            100,
            List.of(List.of(entrega)),
            List.of(camion),
            List.of(chofer));
    RespuestaPlanificacion respuesta =
        new RespuestaPlanificacion(
            UUID.randomUUID(),
            solicitudId,
            LocalDate.now().plusDays(1),
            Map.of(camion, List.of(entrega)),
            Map.of(camion, chofer));
    SolicitudPlanificacion seguimiento =
        new SolicitudPlanificacion(solicitudId, LocalDate.now(), 1, "http://callback");
    when(planificador.procesarSolicitud(solicitud)).thenReturn(respuesta);

    proveedor.solicitarPlanificacion(seguimiento, solicitud);

    assertNull(entrega.getIdRuta());
    verify(restTemplate)
        .postForEntity(
            eq("http://callback"),
            argThat((CallbackPlanificacionRequestDTO dto) -> "OK".equals(dto.estado())),
            eq(Void.class));
  }

  @Test
  void informaElErrorDelPlanificadorMedianteElCallback() {
    PlanificadorDeRutas planificador = mock(PlanificadorDeRutas.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ProveedorExternoPlanificacionSimulado proveedor =
        new ProveedorExternoPlanificacionSimulado(planificador, restTemplate);
    UUID solicitudId = UUID.randomUUID();
    PlanificacionSolicitada solicitud =
        new PlanificacionSolicitada(
            solicitudId, LocalDate.now(), 100, List.of(), List.of(), List.of());
    SolicitudPlanificacion seguimiento =
        new SolicitudPlanificacion(solicitudId, LocalDate.now(), 1, "http://callback");
    when(planificador.procesarSolicitud(solicitud)).thenThrow(new IllegalStateException("fallo"));

    proveedor.solicitarPlanificacion(seguimiento, solicitud);

    verify(restTemplate)
        .postForEntity(
            eq("http://callback"),
            argThat(
                (CallbackPlanificacionRequestDTO dto) ->
                    "ERROR".equals(dto.estado()) && "fallo".equals(dto.motivoError())),
            eq(Void.class));
  }

  private static Entrega crearEntrega() {
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("CABA", provincia);
    Direccion direccion = new Direccion("Calle", 123, null, null, "1000", localidad);
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), direccion, 10f, 2f);
  }
}
