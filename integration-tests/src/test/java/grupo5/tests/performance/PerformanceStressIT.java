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
      String documento = "700" + String.format("%05d", i);
      String nombre = "PerfRaul_" + i;
      String email = "perf.raul." + i + "@example.com";

      long start = System.currentTimeMillis();
      try {
        String personaId = apiCrearPersonaHumana(documento, nombre, email);
        apiCrearDonante(personaId);
        apiCrearDonacion(personaId, "Donación stress " + i, "arroz", 1);
        long end = System.currentTimeMillis();
        latencies.add(end - start);
      } catch (Throwable t) {
        System.err.println("Error creating donor/donation at iteration " + i + ": " + t.getMessage());
        t.printStackTrace();
        errorCount++;
      }
    }

    long endTimeSuite = System.currentTimeMillis();
    long totalDuration = endTimeSuite - startTimeSuite;

    // Report
    printPerformanceReport("Donor and Donation Creation", totalRequests, latencies, errorCount, totalDuration);

    assertEquals(0, errorCount, "There should be no errors during sequential donor and donation creation performance test.");
    double average = calculateAverage(latencies);
    assertTrue(average < 500.0, "Average latency of donor + donation creation (" + average + " ms) should be below 500ms.");
  }

  @Test
  public void testDonationEventProcessingStress() {
    // 1. Pre-register a donor to run stress tests on
    String personaId = apiCrearPersonaHumana("79998888", "StressDonor", "stress.donor@example.com");
    String donorId = apiCrearDonante(personaId);

    // 2. Stress with 200 sequential calls to /api/incentivos/donaciones
    int totalRequests = 200;
    List<Long> latencies = new ArrayList<>();
    int errorCount = 0;

    long startTimeSuite = System.currentTimeMillis();

    for (int i = 0; i < totalRequests; i++) {
      long start = System.currentTimeMillis();
      try {
        apiEnviarEventoDonacionIncentivos(donorId, "2026-06-01", 1);
        long end = System.currentTimeMillis();
        latencies.add(end - start);
      } catch (Throwable t) {
        System.err.println("Error sending donation event at iteration " + i + ": " + t.getMessage());
        t.printStackTrace();
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
