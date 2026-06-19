package grupo5.donaciones.infrastructure;

import grupo5.donaciones.dto.comunicaciones.NuevaDonacionRequest;
import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemanticoBien;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
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
  private final IncentivosFeignClient incentivosFeignClient;

  @Async
  public void procesar(Donacion donacion) {
    List<ItemDonacionNormalizado> itemsNormalizados = normalizador.normalizar(donacion);
    donacion.marcarNormalizada();
    donacionRepository.save(donacion);

    List<DonacionIndependiente> donacionesIndependientes = segmentador.segmentar(itemsNormalizados);
    donacion.marcarSegmentada();
    donacionRepository.save(donacion);

    for (DonacionIndependiente di : donacionesIndependientes) {
      List<String> categorias =
          di.getItems().stream()
              .map(item -> item.getBien().getSubcategoria().getCategoria().getNombre())
              .distinct()
              .toList();

      Persona persona = di.getDonacionOriginal().getDonante().getPersona();
      String nombreDonante = obtenerNombrePersona(persona);

      NuevaDonacionRequest request =
          new NuevaDonacionRequest(
              persona.getId(),
              categorias,
              di.getCantidad(),
              di.getDonacionOriginal().getFecha().toLocalDate(),
              nombreDonante);

      incentivosFeignClient.procesarDonacion(request);
    }

    donacionesIndependientesRepository.saveAll(donacionesIndependientes);
  }

  private String obtenerNombrePersona(Persona persona) {
    if (persona instanceof Humana humana) {
      return humana.getNombre() + " " + humana.getApellido();
    } else if (persona instanceof Juridica juridica) {
      return juridica.getRazonSocial();
    }
    return "Anónimo";
  }
}
