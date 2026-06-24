package grupo5.common.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import java.util.UUID;

public class InstanceIdPropertyDefiner extends PropertyDefinerBase {
  private static final String INSTANCE_ID = UUID.randomUUID().toString().substring(0, 8);

  @Override
  public String getPropertyValue() {
    // Read from environment variable or system property first (ideal for container hostnames/IDs)
    String envId = System.getenv("INSTANCE_ID");
    if (envId != null && !envId.isEmpty()) {
      return envId;
    }
    String sysId = System.getProperty("instance.id");
    if (sysId != null && !sysId.isEmpty()) {
      return sysId;
    }
    // Fallback to static JVM startup random short UUID
    return INSTANCE_ID;
  }
}
