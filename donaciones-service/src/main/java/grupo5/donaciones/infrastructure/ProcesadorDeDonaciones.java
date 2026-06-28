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
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// TODO: Testear Procesador

@Service
@RequiredArgsConstructor
public class ProcesadorDeDonaciones {

  private static final Logger log = LoggerFactory.getLogger(ProcesadorDeDonaciones.class);

  private final NormalizadorSemanticoBien normalizador;
  private final Segmentador segmentador;
  private final IDonacionesRepository donacionRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  private final IncentivosFeignClient incentivosFeignClient;

  @Async
  public void procesar(Donacion donacion) {
    List<ItemDonacionNormalizado> itemsNormalizados = normalizador.normalizar(donacion);
    for (ItemDonacionNormalizado item : itemsNormalizados) {
      log.info(
          "  Item: {}, Subcategoría asignada: {}, Confianza: {}, Estado: {}",
          item.getBien().getBienOriginal().getDescripcion(),
          item.getBien().getSubcategoria() != null
              ? item.getBien().getSubcategoria().getNombre()
              : "null",
          item.getBien().getConfianza(),
          item.getBien().getEstadoNormalizacion());
    }
    donacion.marcarNormalizada();
    donacionRepository.save(donacion);

    List<DonacionIndependiente> donacionesIndependientes = segmentador.segmentar(itemsNormalizados);
    for (DonacionIndependiente di : donacionesIndependientes) {
      log.info(
          "  Donación Independiente ID: {}, Subcategoría: {}, Cantidad: {}, Estado: {}",
          di.getId(),
          di.getSubcategoria() != null ? di.getSubcategoria().getNombre() : "null",
          di.getCantidad(),
          di.getEstadoActual() != null ? di.getEstadoActual().getClass().getSimpleName() : "null");
    }
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
              di.getDonacionOriginal().getDonante().getId(),
              categorias,
              di.getCantidad(),
              di.getDonacionOriginal().getFecha().toLocalDate(),
              nombreDonante);

      log.info(
          "Registrando donación independiente en motor de incentivos. Donante ID: {}, Cantidad: {}",
          di.getDonacionOriginal().getDonante().getId(),
          di.getCantidad());
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
