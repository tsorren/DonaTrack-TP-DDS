package grupo5.tests.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.tests.BaseIT;
import grupo5.tests.builders.DonacionTestDataBuilder;
import grupo5.tests.builders.PersonaTestDataBuilder;
import grupo5.tests.dto.DonacionTestDTO;
import grupo5.tests.dto.PersonaTestDTO;
import grupo5.tests.utils.PollingUtils;
import grupo5.tests.utils.TestIdGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("performance")
class PerformanceStressIT extends BaseIT {

  @Test
  void testDonorCreationPerformance() {
    int totalRequests = 100;
    List<Long> latencies = new ArrayList<>();
    int errorCount = 0;

    long startTimeSuite = System.currentTimeMillis();

    for (int i = 0; i < totalRequests; i++) {
      String documento = TestIdGenerator.randomDni();
      String nombre = "PerfRaul_" + i;
      String email = TestIdGenerator.randomEmail("perf" + i);

      long start = System.currentTimeMillis();
      try {
        PersonaTestDTO persona =
            PersonaTestDataBuilder.humana()
                .conNombre(nombre)
                .conDocumento(documento)
                .conEmail(email)
                .build();
        UUID personaId = donacionesClient.crearPersonaOk(persona);
        PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
        UUID donanteId = donacionesClient.crearDonanteOk(personaId);

        String itemStress = TestIdGenerator.uniqueItemName("stress_grain");
        DonacionTestDTO donacion =
            DonacionTestDataBuilder.deAlimento(itemStress, 1)
                .conDonante(donanteId)
                .conDescripcion("Donación stress " + i)
                .build();
        donacionesClient.crearDonacionOk(donacion);

        long end = System.currentTimeMillis();
        latencies.add(end - start);
      } catch (Throwable t) {
        System.err.println(
            "Error creando donante/donación en iteración " + i + ": " + t.getMessage());
        errorCount++;
      }
    }

    long endTimeSuite = System.currentTimeMillis();
    long totalDuration = endTimeSuite - startTimeSuite;

    printPerformanceReport(
        "Donor and Donation Creation", totalRequests, latencies, errorCount, totalDuration);

    assertEquals(
        0,
        errorCount,
        "No deberían haber errores durante la prueba secuencial de creación de donantes y donaciones.");
    double average = calculateAverage(latencies);
    double maxLatency = Double.parseDouble(System.getProperty("perf.max.donor.latency", "1500.0"));
    assertTrue(
        average < maxLatency,
        "La latencia promedio ("
            + average
            + " ms) debería ser inferior al umbral configurado de "
            + maxLatency
            + " ms.");
  }

  @Test
  void testDonationEventProcessingStress() {
    // 1. Registrar donante para la prueba de estrés
    PersonaTestDTO persona =
        PersonaTestDataBuilder.humana()
            .conNombre("StressDonor")
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail("stress"))
            .build();
    UUID personaId = donacionesClient.crearPersonaOk(persona);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
    UUID donanteId = donacionesClient.crearDonanteOk(personaId);

    // 2. 200 llamadas secuenciales a /api/incentivos/donaciones
    int totalRequests = 200;
    List<Long> latencies = new ArrayList<>();
    int errorCount = 0;

    long startTimeSuite = System.currentTimeMillis();

    for (int i = 0; i < totalRequests; i++) {
      long start = System.currentTimeMillis();
      try {
        incentivosClient.enviarEventoDonacion(donanteId, "2026-06-01", 1).then().statusCode(200);
        long end = System.currentTimeMillis();
        latencies.add(end - start);
      } catch (Throwable t) {
        System.err.println(
            "Error enviando evento de donación en iteración " + i + ": " + t.getMessage());
        errorCount++;
      }
    }

    long endTimeSuite = System.currentTimeMillis();
    long totalDuration = endTimeSuite - startTimeSuite;

    printPerformanceReport(
        "Donation Event Ingestion Stress", totalRequests, latencies, errorCount, totalDuration);

    assertEquals(
        0,
        errorCount,
        "No deberían haber errores durante la prueba de estrés de ingestión de eventos.");
    double average = calculateAverage(latencies);
    double maxLatency = Double.parseDouble(System.getProperty("perf.max.event.latency", "500.0"));
    assertTrue(
        average < maxLatency,
        "La latencia promedio (" + average + " ms) debería ser inferior a " + maxLatency + " ms.");
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
      String testName,
      int totalRequests,
      List<Long> latencies,
      int errorCount,
      long totalDurationMs) {
    long min = latencies.stream().mapToLong(Long::longValue).min().orElse(0);
    long max = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
    double avg = calculateAverage(latencies);
    long p95 = calculatePercentile(latencies, 95.0);
    double throughput = (double) latencies.size() / (totalDurationMs / 1000.0);

    System.out.println(
        "================================================================================");
    System.out.println("QA PERFORMANCE & STRESS TEST REPORT - " + testName.toUpperCase());
    System.out.println(
        "================================================================================");
    System.out.println(String.format("Total Requests Sent : %d", totalRequests));
    System.out.println(String.format("Successful Requests : %d", latencies.size()));
    System.out.println(String.format("Failed Requests     : %d", errorCount));
    System.out.println(String.format("Total Duration      : %d ms", totalDurationMs));
    System.out.println(String.format("Min Latency         : %d ms", min));
    System.out.println(String.format("Max Latency         : %d ms", max));
    System.out.println(String.format("Average Latency     : %.2f ms", avg));
    System.out.println(String.format("P95 Latency         : %d ms", p95));
    System.out.println(String.format("Throughput          : %.2f req/sec", throughput));
    System.out.println(
        "================================================================================");
    System.out.println();
  }
}
