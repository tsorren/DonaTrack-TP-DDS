package grupo5.tests.integration;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import grupo5.tests.BaseIT;
import grupo5.tests.builders.DonacionTestDataBuilder;
import grupo5.tests.builders.PersonaTestDataBuilder;
import grupo5.tests.dto.PersonaTestDTO;
import grupo5.tests.utils.PollingUtils;
import grupo5.tests.utils.TestIdGenerator;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class CrossServiceCommunicationIT extends BaseIT {

  @Test
  void testPersonaReplicationLifecycle() {
    // 1. Crear persona en donaciones-service
    String dni = TestIdGenerator.randomDni();
    String email = TestIdGenerator.randomEmail("juan");
    PersonaTestDTO persona =
        PersonaTestDataBuilder.humana()
            .conNombre("Juan")
            .conApellido("Perez")
            .conDocumento(dni)
            .conEmail(email)
            .build();

    UUID personaId = donacionesClient.crearPersonaOk(persona);
    assertNotNull(personaId);

    // 2. Verificar replicación en notificaciones-service
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
    notificacionesClient
        .obtenerPersona(personaId)
        .then()
        .statusCode(200)
        .body("id", equalTo(personaId.toString()))
        .body("denominacion", equalTo("Juan Perez"))
        .body("tipoPersona", equalTo("HUMANA"))
        .body("mediosDeContacto[0].direccionCorreo", equalTo(email));

    // 3. Actualizar la persona en donaciones-service
    PersonaTestDTO personaModificada =
        PersonaTestDataBuilder.humana()
            .conNombre("Juan Carlos")
            .conApellido("Perez")
            .conDocumento(dni)
            .conEmail(email)
            .build();
    donacionesClient.actualizarPersona(personaId, personaModificada).then().statusCode(200);

    // 4. Verificar réplica actualizada en notificaciones-service
    PollingUtils.esperarDenominacionPersona(notificacionesClient, personaId, "Juan Carlos Perez");

    // 5. Baja (anonimización) de la persona en donaciones-service
    donacionesClient.eliminarPersona(personaId).then().statusCode(204);

    // 6. Verificar réplica anonimizada en notificaciones-service
    PollingUtils.esperarDenominacionPersona(notificacionesClient, personaId, "ANONIMIZADO");
    notificacionesClient
        .obtenerPersona(personaId)
        .then()
        .statusCode(200)
        .body("mediosDeContacto[0].direccionCorreo", equalTo("ANONIMIZADO"));
  }

  @Test
  void testDonanteRegistrationAndWelcomeNotification() {
    Donante donante = crearDonante("Maria");
    PollingUtils.esperarDonanteEnIncentivos(incentivosClient, donante.donanteId());

    // Métricas iniciales del donante en incentivos-service
    incentivosClient
        .obtenerMetricas(donante.donanteId())
        .then()
        .statusCode(200)
        .body("totalDonacionesExitosas", equalTo(0))
        .body("categoria", equalTo("COLABORADOR"));

    // Notificación de bienvenida en notificaciones-service
    PollingUtils.esperarNotificacionCon(notificacionesClient, donante.personaId(), "Usuario:");
  }

  @Test
  void testDonanteBaja() {
    Donante donante = crearDonante("Pedro");

    // El alta llega a incentivos por RabbitMQ: se espera
    PollingUtils.esperarDonanteEnIncentivos(incentivosClient, donante.donanteId());

    donacionesClient.eliminarDonante(donante.donanteId()).then().statusCode(204);

    PollingUtils.esperarBajaDonanteEnIncentivos(incentivosClient, donante.donanteId());
  }

  @Test
  void testE2EDonationFlowAndSideEffects() {
    Donante donante = crearDonante("Ana");
    UUID entidadId = crearEntidad("Comedor Solidario");

    // Donación con bien único aislado
    String nombreBien = TestIdGenerator.uniqueItemName("arroz flow");
    UUID donacionId =
        donacionesClient.crearDonacionOk(
            DonacionTestDataBuilder.deAlimento(nombreBien, 10)
                .conDonante(donante.donanteId())
                .conDescripcion("Donación de " + nombreBien)
                .build());
    PollingUtils.esperarDonacionSegmentada(donacionesClient, donacionId);

    // Matching, aprobación y entrega por el atajo REST
    entregarPorRest(asignarYAprobar(entidadId, "Necesidad de " + nombreBien, 10));

    // Efectos en incentivos-service y notificaciones-service, un aviso por evento
    PollingUtils.esperarTotalDonacionesExitosas(incentivosClient, donante.donanteId(), 0);
    for (String aviso : List.of("fue asignada a", "salió a entregar", "recibió tu donación")) {
      PollingUtils.esperarNotificacionCon(notificacionesClient, donante.personaId(), aviso);
    }
  }

  @Test
  void testRankingMensualYPosicion() {
    UUID donanteId = crearDonanteEnIncentivos("Raul");

    // 3 donaciones mensuales consecutivas con fechas dinámicas
    enviarDonacionesEnTresMeses(donanteId);
    String periodoActual = YearMonth.now().toString();

    incentivosClient.calcularRanking(periodoActual).then().statusCode(200);

    incentivosClient
        .obtenerUltimoRanking()
        .then()
        .statusCode(200)
        .body("periodo", equalTo(periodoActual));

    incentivosClient
        .obtenerMetricas(donanteId)
        .then()
        .statusCode(200)
        .body("misionesCompletadasTotal", equalTo(1));

    incentivosClient
        .obtenerHistorialRanking()
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1));
  }

  @Test
  void testInsigniaVisibilityFlow() {
    UUID donanteId = crearDonanteEnIncentivos("Laura");

    // 3 eventos consecutivos completan la misión de racha
    enviarDonacionesEnTresMeses(donanteId);

    // La insignia "Racha Inicial" queda visible
    incentivosClient
        .obtenerInsignias(donanteId, null)
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].nombre", equalTo("Racha Inicial"))
        .body("[0].visible", equalTo(true));

    // Visibilidad en false: aparece sin filtro y no con soloVisibles=true
    incentivosClient
        .cambiarVisibilidadInsignia(donanteId, "Racha Inicial", false)
        .then()
        .statusCode(200);
    incentivosClient
        .obtenerInsignias(donanteId, null)
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].visible", equalTo(false));
    incentivosClient
        .obtenerInsignias(donanteId, true)
        .then()
        .statusCode(200)
        .body("size()", equalTo(0));

    // Visibilidad en true otra vez
    incentivosClient
        .cambiarVisibilidadInsignia(donanteId, "Racha Inicial", true)
        .then()
        .statusCode(200);
    incentivosClient
        .obtenerInsignias(donanteId, true)
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].visible", equalTo(true));
  }

  @Test
  void testComplexE2EMultipleDonationsFlow() {
    Donante donante = crearDonante("Carlos");
    UUID entidadId = crearEntidad("Hogar de Dia");

    // 2 donaciones con bienes aislados (5 unidades cada una)
    String bien1 = TestIdGenerator.uniqueItemName("arroz complex");
    String bien2 = TestIdGenerator.uniqueItemName("fideos complex");
    UUID donacionId1 =
        donacionesClient.crearDonacionOk(
            DonacionTestDataBuilder.deAlimento(bien1, 5).conDonante(donante.donanteId()).build());
    UUID donacionId2 =
        donacionesClient.crearDonacionOk(
            DonacionTestDataBuilder.deAlimento(bien2, 5).conDonante(donante.donanteId()).build());
    PollingUtils.esperarDonacionSegmentada(donacionesClient, donacionId1);
    PollingUtils.esperarDonacionSegmentada(donacionesClient, donacionId2);

    // Una necesidad de 10 unidades se cubre con las 2 donaciones
    List<UUID> asignadas = asignarYAprobar(entidadId, "Necesidad de " + bien1 + " y " + bien2, 10);
    assertEquals(
        2, asignadas.size(), "Deberían haber 2 fragmentaciones para completar las 10 unidades");

    entregarPorRest(asignadas);

    PollingUtils.esperarTotalDonacionesExitosas(incentivosClient, donante.donanteId(), 1);
    PollingUtils.esperarMinimoNotificaciones(notificacionesClient, donante.personaId(), 5);
  }

  private static UUID crearDonanteEnIncentivos(String nombre) {
    UUID donanteId = crearDonante(nombre).donanteId();
    PollingUtils.esperarDonanteEnIncentivos(incentivosClient, donanteId);
    return donanteId;
  }

  /**
   * Atajo REST de Incentivos (DIT-01): una donación el día 1 de cada uno de los últimos 3 meses.
   */
  private static void enviarDonacionesEnTresMeses(UUID donanteId) {
    LocalDate now = LocalDate.now();
    for (int mesesAtras = 2; mesesAtras >= 0; mesesAtras--) {
      String fecha =
          now.minusMonths(mesesAtras).withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
      incentivosClient.enviarEventoDonacion(donanteId, fecha, 1).then().statusCode(200);
    }
  }
}
