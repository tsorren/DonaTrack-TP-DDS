package grupo5.common.events;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class EventoDeDominio {
  private final UUID id;
  private final LocalDateTime timestamp;

  protected EventoDeDominio() {
    this.id = UUID.randomUUID();
    this.timestamp = LocalDateTime.now(ZoneId.systemDefault());
  }

  protected EventoDeDominio(UUID id, LocalDateTime timestamp) {
    this.id = id != null ? id : UUID.randomUUID();
    this.timestamp = timestamp != null ? timestamp : LocalDateTime.now(ZoneId.systemDefault());
  }
}
