package grupo5.donaciones.infrastructure.events;

import grupo5.donaciones.models.entities.donaciones.events.DonacionNormalizada;
import grupo5.donaciones.services.ISegmentacionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SegmentacionEventListener {

  private static final Logger log = LoggerFactory.getLogger(SegmentacionEventListener.class);

  private final ISegmentacionService segmentacionService;

  @EventListener
  public void onDonacionNormalizada(DonacionNormalizada event) {
    log.info("Capturando DonacionNormalizada para donación ID: {}", event.donacionId());
    segmentacionService.procesarDonacionNormalizada(event);
  }
}
