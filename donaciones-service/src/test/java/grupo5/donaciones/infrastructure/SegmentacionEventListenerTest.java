package grupo5.donaciones.infrastructure;

import static org.mockito.Mockito.*;

import grupo5.donaciones.infrastructure.events.SegmentacionEventListener;
import grupo5.donaciones.models.entities.donaciones.events.DonacionNormalizada;
import grupo5.donaciones.services.ISegmentacionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentacionEventListenerTest {

  @Mock private ISegmentacionService segmentacionService;

  @InjectMocks private SegmentacionEventListener listener;

  @Test
  void onDonacionNormalizada_deberiaDelegar_alServicio() {
    DonacionNormalizada event = new DonacionNormalizada(UUID.randomUUID(), UUID.randomUUID());

    listener.onDonacionNormalizada(event);

    verify(segmentacionService, times(1)).procesarDonacionNormalizada(event);
  }
}
