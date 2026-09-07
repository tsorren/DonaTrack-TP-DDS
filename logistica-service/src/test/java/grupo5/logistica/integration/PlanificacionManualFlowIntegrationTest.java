package grupo5.logistica.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.logistica.LogisticaServiceApplication;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.PlanificacionSolicitada;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.services.ComunicadorEventosLogistica;
import grupo5.logistica.services.IPlanificacionService;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = LogisticaServiceApplication.class,
    properties = {
      "logistica.planificacion.manual-enabled=true",
      "logistica.planificacion.cron.expression=0 0 0 1 1 *"
    })
class PlanificacionManualFlowIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private IPlanificacionService planificacionService;

  @MockitoBean private IServicioExternoPlanificacion planificadorExterno;
  @MockitoBean private ComunicadorEventosLogistica comunicadorEventos;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    objectMapper = new ObjectMapper();

    doAnswer(
            invocation -> {
              SolicitudPlanificacion seguimiento = invocation.getArgument(0);
              PlanificacionSolicitada solicitud = invocation.getArgument(1);

              CallbackPlanificacionRequestDTO callback =
                  new CallbackPlanificacionRequestDTO(
                      seguimiento.getId(),
                      List.of(
                          new RutaPlanificadaDTO(
                              solicitud.camionesDisponibles().getFirst().getId(),
                              solicitud.choferesDisponibles().getFirst().getId(),
                              solicitud.fecha(),
                              solicitud.entregas().stream().map(Entrega::getId).toList())),
                      "OK",
                      null);

              planificacionService.procesarCallback(callback);
              return null;
            })
        .when(planificadorExterno)
        .solicitarPlanificacion(any(), any());
  }

  @Test
  void ejecucionManual_deberiaCrearRutaDePuntaAPunta() throws Exception {
    String camionId =
        crear(
            "/api/camiones",
            """
            {"patente":"AB123CD","capacidadVolumen":100,"altura":3,"capacidadKG":5000}
            """);

    String choferId =
        crear(
            "/api/choferes",
            """
            {"nombre":"Juan","apellido":"Prueba","licencia":"LIC-TEST-001","telefonoContacto":"1122334455"}
            """);

    String entregaId =
        crear(
            "/api/entregas",
            """
            {
              "idDonacion":"11111111-1111-1111-1111-111111111111",
              "idBeneficiaria":"22222222-2222-2222-2222-222222222222",
              "destino":{
                "calle":"Av. Siempre Viva","altura":742,"piso":null,"departamento":null,
                "codigoPostal":"1000","localidad":"CABA","provincia":"Buenos Aires","pais":"Argentina"
              },
              "pesoTotalKG":10,
              "volumenTotalM3":1
            }
            """);

    mockMvc
        .perform(post("/api/logistica/planificaciones/ejecuciones"))
        .andExpect(status().isAccepted());

    mockMvc
        .perform(get("/api/rutas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].camionId").value(camionId))
        .andExpect(jsonPath("$[0].choferId").value(choferId))
        .andExpect(jsonPath("$[0].entregaIds", hasSize(1)))
        .andExpect(jsonPath("$[0].entregaIds[0]").value(entregaId));

    verify(planificadorExterno).solicitarPlanificacion(any(), any());
    verify(comunicadorEventos).comunicarRutaAsignada(any(), any());
  }

  private String crear(String endpoint, String body) throws Exception {
    MvcResult result =
        mockMvc
            .perform(post(endpoint).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn();

    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }
}
