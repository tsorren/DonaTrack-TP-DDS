package grupo5.donaciones.infrastructure;

import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemanticoBien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.impl.DonacionRepositoryEnMemoria;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// TODO: Testear Procesador

@Component
@RequiredArgsConstructor
public class ProcesadorDeDonaciones {

  private final NormalizadorSemanticoBien normalizador;
  private final Segmentador segmentador;
  private final DonacionRepositoryEnMemoria donacionRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;

  @Async
  public void procesar(Donacion donacion) {
    List<ItemDonacionNormalizado> itemsNormalizados = normalizador.normalizar(donacion);
    donacion.marcarNormalizada();
    donacionRepository.save(donacion);

    List<DonacionIndependiente> donacionesIndependientes = segmentador.segmentar(itemsNormalizados);
    donacion.marcarSegmentada();
    donacionRepository.save(donacion);

    // TODO: Usar IncentivosFeignClient, forEach donacionIndependiente mandar DTO
    // NuevaDonacionRequest

    donacionesIndependientesRepository.saveAll(donacionesIndependientes);
  }
}
