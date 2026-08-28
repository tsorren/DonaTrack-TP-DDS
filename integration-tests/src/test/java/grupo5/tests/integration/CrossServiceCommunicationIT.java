package grupo5.tests.integration;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import grupo5.tests.BaseIT;
import grupo5.tests.builders.DonacionTestDataBuilder;
import grupo5.tests.builders.NecesidadTestDataBuilder;
import grupo5.tests.builders.PersonaTestDataBuilder;
import grupo5.tests.dto.DonacionTestDTO;
import grupo5.tests.dto.NecesidadTestDTO;
import grupo5.tests.dto.PersonaTestDTO;
import grupo5.tests.utils.PollingUtils;
import grupo5.tests.utils.TestIdGenerator;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
    // 1. Crear persona
    PersonaTestDTO persona =
        PersonaTestDataBuilder.humana()
            .conNombre("Maria")
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail("maria"))
            .build();
    UUID personaId = donacionesClient.crearPersonaOk(persona);

    // 2. Registrar persona como donante
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
    UUID donanteId = donacionesClient.crearDonanteOk(personaId);
    assertNotNull(donanteId);

    // 3. Verificar métricas del donante en incentivos-service
    incentivosClient
        .obtenerMetricas(donanteId)
        .then()
        .statusCode(200)
        .body("totalDonacionesExitosas", equalTo(0))
        .body("categoria", equalTo("COLABORADOR"));

    // 4. Verificar notificación de bienvenida en notificaciones-service
    PollingUtils.esperarMinimoNotificaciones(notificacionesClient, personaId, 1);
    notificacionesClient
        .obtenerNotificacionesPorPersona(personaId)
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1))
        .body("mensaje[0]", containsString("Usuario:"));
  }

  @Test
  void testDonanteBaja() {
    // 1. Crear persona y donante
    PersonaTestDTO persona =
        PersonaTestDataBuilder.humana()
            .conNombre("Pedro")
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail("pedro"))
            .build();
    UUID personaId = donacionesClient.crearPersonaOk(persona);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
    UUID donanteId = donacionesClient.crearDonanteOk(personaId);

    // Verificar perfil en incentivos
    incentivosClient.obtenerMetricas(donanteId).then().statusCode(200);

    // 2. Dar de baja donante en donaciones-service
    donacionesClient.eliminarDonante(donanteId).then().statusCode(204);

    // 3. Verificar que el perfil esté eliminado o no disponible en incentivos
    incentivosClient
        .obtenerMetricas(donanteId)
        .then()
        .statusCode(anyOf(equalTo(400), equalTo(404)));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testE2EDonationFlowAndSideEffects() {
    // 1. Crear Donante
    PersonaTestDTO personaDonante =
        PersonaTestDataBuilder.humana()
            .conNombre("Ana")
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail("ana"))
            .build();
    UUID donantePersonaId = donacionesClient.crearPersonaOk(personaDonante);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, donantePersonaId);
    UUID donanteId = donacionesClient.crearDonanteOk(donantePersonaId);

    // 2. Crear Entidad Beneficiaria
    PersonaTestDTO personaJuridica =
        PersonaTestDataBuilder.juridica()
            .conRazonSocial(TestIdGenerator.uniqueName("Comedor Solidario"))
            .build();
    UUID benefPersonaId = donacionesClient.crearPersonaOk(personaJuridica);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, benefPersonaId);
    UUID entidadId = donacionesClient.crearEntidadOk(benefPersonaId);

    // 3. Crear Donación con bien único aislado
    String nombreBien = TestIdGenerator.uniqueItemName("arroz flow");
    DonacionTestDTO donacion =
        DonacionTestDataBuilder.deAlimento(nombreBien, 10)
            .conDonante(donanteId)
            .conDescripcion("Donación de " + nombreBien)
            .build();
    UUID donacionId = donacionesClient.crearDonacionOk(donacion);
    assertNotNull(donacionId);

    // Esperar normalización y segmentación asíncrona
    PollingUtils.esperarDonacionSegmentada(donacionesClient, donacionId);

    // 4. Obtener subcategoría "No Perecederos"
    Response subcatResp = donacionesClient.obtenerSubcategorias();
    subcatResp.then().statusCode(200);
    String subcategoryIdStr = subcatResp.path("find { it.nombre == 'No Perecederos' }.id");
    assertNotNull(subcategoryIdStr);
    UUID subcategoryId = UUID.fromString(subcategoryIdStr);

    // 5. Crear Necesidad de 10 unidades
    String descNecesidad = "Necesidad de " + nombreBien;
    NecesidadTestDTO necesidad =
        NecesidadTestDataBuilder.extraordinaria(entidadId, subcategoryId, 10)
            .conDescripcion(descNecesidad)
            .build();
    donacionesClient.crearNecesidadOk(necesidad);

    // 6. Ejecutar Matching
    Response ejecucionResp = donacionesClient.ejecutarAsignacion();
    ejecucionResp.then().statusCode(201);

    List<Map<String, Object>> propuestas = JsonPath.from(ejecucionResp.asString()).get();
    assertFalse(propuestas.isEmpty());

    Map<String, Object> propuesta = null;
    for (Map<String, Object> p : propuestas) {
      Map<String, Object> nec = (Map<String, Object>) p.get("necesidad");
      if (nec != null && descNecesidad.equals(nec.get("descripcion"))) {
        propuesta = p;
        break;
      }
    }

    assertNotNull(propuesta, "Propuesta no encontrada para " + descNecesidad);
    UUID propuestaId = UUID.fromString((String) propuesta.get("id"));

    List<Map<String, Object>> frags = (List<Map<String, Object>>) propuesta.get("fragmentaciones");
    assertNotNull(frags);
    assertFalse(frags.isEmpty());
    // 7. Aprobar Propuesta
    donacionesClient.actualizarEstadoPropuesta(propuestaId, "APROBADA").then().statusCode(200);

    // 8. Transiciones de estado de DonacionIndependiente para todas las fragmentaciones
    for (Map<String, Object> frag : frags) {
      Map<String, Object> di = (Map<String, Object>) frag.get("donacionIndependiente");
      assertNotNull(di);
      UUID diId = UUID.fromString((String) di.get("id"));

      donacionesClient
          .cambiarEstadoDonacionIndependiente(diId, "LISTA_PARA_ENTREGAR", "TRANSPORTISTA")
          .then()
          .statusCode(200);

      donacionesClient
          .cambiarEstadoDonacionIndependiente(diId, "EN_TRASLADO", "TRANSPORTISTA")
          .then()
          .statusCode(200);

      donacionesClient
          .cambiarEstadoDonacionIndependiente(diId, "ENTREGADA", "TRANSPORTISTA")
          .then()
          .statusCode(200);
    }

    // 9. Verificar efectos secundarios en incentivos-service
    PollingUtils.esperarTotalDonacionesExitosas(incentivosClient, donanteId, 0);

    // 10. Verificar notificaciones generadas
    PollingUtils.esperarMinimoNotificaciones(notificacionesClient, donantePersonaId, 3);
  }

  @Test
  void testRankingMensualYPosicion() {
    // 1. Crear Persona y Donante
    PersonaTestDTO persona =
        PersonaTestDataBuilder.humana()
            .conNombre("Raul")
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail("raul"))
            .build();
    UUID personaId = donacionesClient.crearPersonaOk(persona);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
    UUID donanteId = donacionesClient.crearDonanteOk(personaId);

    // 2. Enviar 3 donaciones mensuales consecutivas con fechas dinámicas
    LocalDate now = LocalDate.now();
    String m1 = now.minusMonths(2).withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    String m2 = now.minusMonths(1).withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    String m3 = now.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    String periodoActual = YearMonth.now().toString();

    incentivosClient.enviarEventoDonacion(donanteId, m1, 1).then().statusCode(200);
    incentivosClient.enviarEventoDonacion(donanteId, m2, 1).then().statusCode(200);
    incentivosClient.enviarEventoDonacion(donanteId, m3, 1).then().statusCode(200);

    // 3. Calcular ranking para el período actual
    incentivosClient.calcularRanking(periodoActual).then().statusCode(200);

    // 4. Consultar último ranking
    Response rankingResp = incentivosClient.obtenerUltimoRanking();
    rankingResp.then().statusCode(200).body("periodo", equalTo(periodoActual));

    // 5. Consultar métricas del donante
    incentivosClient
        .obtenerMetricas(donanteId)
        .then()
        .statusCode(200)
        .body("misionesCompletadasTotal", equalTo(1));

    // 6. Consultar historial de ranking
    incentivosClient
        .obtenerHistorialRanking()
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1));
  }

  @Test
  void testInsigniaVisibilityFlow() {
    // 1. Crear Persona y Donante
    PersonaTestDTO persona =
        PersonaTestDataBuilder.humana()
            .conNombre("Laura")
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail("laura"))
            .build();
    UUID personaId = donacionesClient.crearPersonaOk(persona);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
    UUID donanteId = donacionesClient.crearDonanteOk(personaId);

    // 2. Enviar 3 eventos consecutivos para completar misión de racha
    LocalDate now = LocalDate.now();
    String m1 = now.minusMonths(2).withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    String m2 = now.minusMonths(1).withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    String m3 = now.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

    incentivosClient.enviarEventoDonacion(donanteId, m1, 1).then().statusCode(200);
    incentivosClient.enviarEventoDonacion(donanteId, m2, 1).then().statusCode(200);
    incentivosClient.enviarEventoDonacion(donanteId, m3, 1).then().statusCode(200);

    // 3. Verificar que la insignia "Racha Inicial" esté visible
    incentivosClient
        .obtenerInsignias(donanteId, null)
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].nombre", equalTo("Racha Inicial"))
        .body("[0].visible", equalTo(true));

    // 4. Cambiar visibilidad a false
    incentivosClient
        .cambiarVisibilidadInsignia(donanteId, "Racha Inicial", false)
        .then()
        .statusCode(200);

    // 5. Verificar insignia con visible=false y filtro soloVisibles=true
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

    // 6. Cambiar visibilidad a true
    incentivosClient
        .cambiarVisibilidadInsignia(donanteId, "Racha Inicial", true)
        .then()
        .statusCode(200);

    // 7. Verificar visible nuevamente
    incentivosClient
        .obtenerInsignias(donanteId, true)
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].visible", equalTo(true));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testComplexE2EMultipleDonationsFlow() {
    // 1. Crear Donante y Entidad Beneficiaria
    PersonaTestDTO personaDonante =
        PersonaTestDataBuilder.humana()
            .conNombre("Carlos")
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail("carlos"))
            .build();
    UUID donantePersonaId = donacionesClient.crearPersonaOk(personaDonante);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, donantePersonaId);
    UUID donanteId = donacionesClient.crearDonanteOk(donantePersonaId);

    PersonaTestDTO personaJuridica =
        PersonaTestDataBuilder.juridica()
            .conRazonSocial(TestIdGenerator.uniqueName("Hogar de Dia"))
            .build();
    UUID benefPersonaId = donacionesClient.crearPersonaOk(personaJuridica);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, benefPersonaId);
    UUID entidadId = donacionesClient.crearEntidadOk(benefPersonaId);

    // 2. Obtener subcategoría "No Perecederos"
    Response subcatResp = donacionesClient.obtenerSubcategorias();
    subcatResp.then().statusCode(200);
    UUID subcategoryId =
        UUID.fromString(subcatResp.path("find { it.nombre == 'No Perecederos' }.id"));

    // 3. Crear 2 donaciones con bienes aislados (5 unidades cada una)
    String bien1 = TestIdGenerator.uniqueItemName("arroz complex");
    String bien2 = TestIdGenerator.uniqueItemName("fideos complex");

    UUID donacionId1 =
        donacionesClient.crearDonacionOk(
            DonacionTestDataBuilder.deAlimento(bien1, 5).conDonante(donanteId).build());
    UUID donacionId2 =
        donacionesClient.crearDonacionOk(
            DonacionTestDataBuilder.deAlimento(bien2, 5).conDonante(donanteId).build());

    PollingUtils.esperarDonacionSegmentada(donacionesClient, donacionId1);
    PollingUtils.esperarDonacionSegmentada(donacionesClient, donacionId2);

    // 4. Crear Necesidad de 10 unidades
    String descNecesidad = "Necesidad de " + bien1 + " y " + bien2;
    NecesidadTestDTO necesidad =
        NecesidadTestDataBuilder.extraordinaria(entidadId, subcategoryId, 10)
            .conDescripcion(descNecesidad)
            .build();
    donacionesClient.crearNecesidadOk(necesidad);

    // 5. Ejecutar Matching
    Response ejecucionResp = donacionesClient.ejecutarAsignacion();
    ejecucionResp.then().statusCode(201);

    List<Map<String, Object>> propuestas = JsonPath.from(ejecucionResp.asString()).get();
    assertFalse(propuestas.isEmpty());

    Map<String, Object> propuesta = null;
    for (Map<String, Object> p : propuestas) {
      Map<String, Object> nec = (Map<String, Object>) p.get("necesidad");
      if (nec != null && descNecesidad.equals(nec.get("descripcion"))) {
        propuesta = p;
        break;
      }
    }

    assertNotNull(propuesta, "Propuesta no encontrada para " + descNecesidad);
    UUID propuestaId = UUID.fromString((String) propuesta.get("id"));

    // Aprobar propuesta
    donacionesClient.actualizarEstadoPropuesta(propuestaId, "APROBADA").then().statusCode(200);

    List<Map<String, Object>> frags = (List<Map<String, Object>>) propuesta.get("fragmentaciones");
    assertNotNull(frags);
    assertEquals(
        2, frags.size(), "Deberían haber 2 fragmentaciones para completar las 10 unidades");

    UUID diId1 =
        UUID.fromString(
            (String) ((Map<String, Object>) frags.get(0).get("donacionIndependiente")).get("id"));
    UUID diId2 =
        UUID.fromString(
            (String) ((Map<String, Object>) frags.get(1).get("donacionIndependiente")).get("id"));

    // 6. Transición de ambas donaciones independientes a ENTREGADA
    for (UUID diId : List.of(diId1, diId2)) {
      donacionesClient
          .cambiarEstadoDonacionIndependiente(diId, "LISTA_PARA_ENTREGAR", "TRANSPORTISTA")
          .then()
          .statusCode(200);
      donacionesClient
          .cambiarEstadoDonacionIndependiente(diId, "EN_TRASLADO", "TRANSPORTISTA")
          .then()
          .statusCode(200);
      donacionesClient
          .cambiarEstadoDonacionIndependiente(diId, "ENTREGADA", "TRANSPORTISTA")
          .then()
          .statusCode(200);
    }

    // 7. Verificar métricas en incentivos y notificaciones
    PollingUtils.esperarTotalDonacionesExitosas(incentivosClient, donanteId, 1);
    PollingUtils.esperarMinimoNotificaciones(notificacionesClient, donantePersonaId, 5);
  }
}
