package grupo5.tests.utils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestIdGenerator {
  private static final AtomicInteger SEQ = new AtomicInteger(1000);

  private TestIdGenerator() {}

  public static String randomDni() {
    int randomPart = ThreadLocalRandom.current().nextInt(10000, 99999);
    return "7" + (SEQ.incrementAndGet() % 1000) + randomPart;
  }

  public static String randomCuit() {
    int randomPart = ThreadLocalRandom.current().nextInt(10000000, 99999999);
    return "30-" + randomPart + "-9";
  }

  public static String randomEmail(String prefix) {
    return prefix.toLowerCase()
        + "."
        + UUID.randomUUID().toString().substring(0, 8)
        + "@test.donatrack.org";
  }

  public static String uniqueName(String base) {
    return base + " " + UUID.randomUUID().toString().substring(0, 6);
  }

  public static String uniqueItemName(String base) {
    return base + " " + UUID.randomUUID().toString().substring(0, 8);
  }
}
