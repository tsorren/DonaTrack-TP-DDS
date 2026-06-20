package grupo5.tests.performance;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import grupo5.tests.BaseIT;
import io.restassured.http.ContentType;
import java.util.*;
import org.junit.jupiter.api.Test;

public class PerformanceStressIT extends BaseIT {

  @Test
  public void testDonorCreationPerformance() {
    int totalRequests = 100;
    List<Long> latencies = new ArrayList<>();
    int errorCount = 0;

    long startTimeSuite = System.currentTimeMillis();

    for (int i = 0; i < totalRequests; i++) {
      Map<String, Object> personaPayload = new HashMap<>();
      personaPayload.put("tipo", "HUMANA");
      personaPayload.put("tipoDocumento", "DNI");
      // Use dynamic document number to avoid conflicts
      personaPayload.put("documento", "700" + String.format("%05d", i));
      personaPayload.put("nombre", "PerfRaul_" + i);
      personaPayload.put("apellido", "PerfGomez_" + i);
      personaPayload.put("genero", "HOMBRE");
      personaPayload.put("fechaNacimiento", "1990-01-01");

      Map<String, Object> medio = new HashMap<>();
      medio.put("tipo", "CORREO");
      medio.put("esPredeterminado", true);
      medio.put("direccionCorreo", "perf.raul." + i + "@example.com");
      personaPayload.put("mediosDeContacto", List.of(medio));

      long start = System.currentTimeMillis();
      try {
        String personaId =
            given()
                .contentType(ContentType.JSON)
                .body(personaPayload)
                .when()
                .post(DONACIONES_URL + "/api/personas")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Map<String, Object> donorPayload = new HashMap<>();
        donorPayload.put("idPersona", personaId);

        given()
            .contentType(ContentType.JSON)
            .body(donorPayload)
            .when()
            .post(DONACIONES_URL + "/api/donantes")
            .then()
            .statusCode(201);

        long end = System.currentTimeMillis();
        latencies.add(end - start);
      } catch (Exception e) {
        errorCount++;
      }
    }

    long endTimeSuite = System.currentTimeMillis();
    long totalDuration = endTimeSuite - startTimeSuite;

    // Report
    printPerformanceReport("Donor Creation (Persona + Donor)", totalRequests, latencies, errorCount, totalDuration);

    assertEquals(0, errorCount, "There should be no errors during sequential donor creation performance test.");
    double average = calculateAverage(latencies);
    assertTrue(average < 350.0, "Average latency of donor creation (" + average + " ms) should be below 350ms.");
  }

  @Test
  public void testDonationEventProcessingStress() {
    // 1. Pre-register a donor to run stress tests on
    Map<String, Object> personaPayload = new HashMap<>();
    personaPayload.put("tipo", "HUMANA");
    personaPayload.put("tipoDocumento", "DNI");
    personaPayload.put("documento", "79998888");
    personaPayload.put("nombre", "StressDonor");
    personaPayload.put("apellido", "StressLastName");
    personaPayload.put("genero", "HOMBRE");
    personaPayload.put("fechaNacimiento", "1995-01-01");

    Map<String, Object> medio = new HashMap<>();
    medio.put("tipo", "CORREO");
    medio.put("esPredeterminado", true);
    medio.put("direccionCorreo", "stress.donor@example.com");
    personaPayload.put("mediosDeContacto", List.of(medio));

    String personaId =
        given()
            .contentType(ContentType.JSON)
            .body(personaPayload)
            .when()
            .post(DONACIONES_URL + "/api/personas")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    Map<String, Object> donorPayload = new HashMap<>();
    donorPayload.put("idPersona", personaId);

    String donorId =
        given()
            .contentType(ContentType.JSON)
            .body(donorPayload)
            .when()
            .post(DONACIONES_URL + "/api/donantes")
            .then()
            .statusCode(201)
            .extract()
            .path("idDonante");

    // 2. Stress with 200 sequential calls to /api/incentivos/donaciones
    int totalRequests = 200;
    List<Long> latencies = new ArrayList<>();
    int errorCount = 0;

    long startTimeSuite = System.currentTimeMillis();

    for (int i = 0; i < totalRequests; i++) {
      Map<String, Object> event = new HashMap<>();
      event.put("donanteId", donorId);
      event.put("categorias", List.of("Alimentos"));
      event.put("cantidadBienes", 1);
      event.put("fecha", "2026-06-01");

      long start = System.currentTimeMillis();
      try {
        given()
            .contentType(ContentType.JSON)
            .body(event)
            .when()
            .post(INCENTIVOS_URL + "/api/incentivos/donaciones")
            .then()
            .statusCode(200);

        long end = System.currentTimeMillis();
        latencies.add(end - start);
      } catch (Exception e) {
        errorCount++;
      }
    }

    long endTimeSuite = System.currentTimeMillis();
    long totalDuration = endTimeSuite - startTimeSuite;

    // Report
    printPerformanceReport("Donation Event Ingestion Stress", totalRequests, latencies, errorCount, totalDuration);

    assertEquals(0, errorCount, "There should be no errors during sequential donation event processing stress test.");
    double average = calculateAverage(latencies);
    assertTrue(average < 150.0, "Average latency of event ingestion (" + average + " ms) should be below 150ms.");
  }

  private double calculateAverage(List<Long> values) {
    return values.stream().mapToLong(Long::longValue).average().orElse(0.0);
  }

  private long calculatePercentile(List<Long> values, double percentile) {
    if (values.isEmpty()) return 0;
    List<Long> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
    return sorted.get(Math.max(0, index));
  }

  private void printPerformanceReport(
      String testName, int totalRequests, List<Long> latencies, int errorCount, long totalDurationMs) {
    long min = latencies.stream().mapToLong(Long::longValue).min().orElse(0);
    long max = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
    double avg = calculateAverage(latencies);
    long p95 = calculatePercentile(latencies, 95.0);
    double throughput = (double) latencies.size() / (totalDurationMs / 1000.0);

    System.out.println("================================================================================");
    System.out.println("QA PERFORMANCE & STRESS TEST REPORT - " + testName.toUpperCase());
    System.out.println("================================================================================");
    System.out.println(String.format("Total Requests Sent : %d", totalRequests));
    System.out.println(String.format("Successful Requests : %d", latencies.size()));
    System.out.println(String.format("Failed Requests     : %d", errorCount));
    System.out.println(String.format("Total Duration      : %d ms", totalDurationMs));
    System.out.println(String.format("Min Latency         : %d ms", min));
    System.out.println(String.format("Max Latency         : %d ms", max));
    System.out.println(String.format("Average Latency     : %.2f ms", avg));
    System.out.println(String.format("P95 Latency         : %d ms", p95));
    System.out.println(String.format("Throughput          : %.2f req/sec", throughput));
    System.out.println("================================================================================");
    System.out.println();
  }
}
