package grupo5.common.events;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {
  UUID eventId();

  UUID aggregateId();

  LocalDateTime timestamp();
}
