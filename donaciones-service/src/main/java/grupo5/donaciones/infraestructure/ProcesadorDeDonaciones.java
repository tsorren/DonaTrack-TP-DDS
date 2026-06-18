package grupo5.donaciones.infraestructure;

import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.DonacionRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcesadorDeDonaciones {

  private final Normalizador normalizador;
  private final Segmentador segmentador;
  private final DonacionRepository donacionRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;

  @Async
  public void procesar(Donacion donacion) {
    List<ItemDonacionNormalizado> itemsNormalizados = normalizador.normalizar(donacion.getItems());
    donacion.marcarNormalizada();
    donacionRepository.save(donacion);

    List<DonacionIndependiente> donacionesIndependientes = segmentador.segmentar(itemsNormalizados);
    donacion.marcarSegmentada();
    donacionRepository.save(donacion);

    donacionesIndependientesRepository.saveAll(donacionesIndependientes);
  }
}
