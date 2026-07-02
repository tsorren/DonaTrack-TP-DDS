package grupo5.logistica.services;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GeneradorDeRutas {
  private final AlgoritmoOrdenadorDeEntregas ordenadorEntregas;
  private final AlgoritmoAsignadorDeEntregas asignadorDeEntregas;

  public GeneradorDeRutas(
      AlgoritmoOrdenadorDeEntregas ordenadorEntregas,
      AlgoritmoAsignadorDeEntregas asignadorDeEntregas) {
    this.ordenadorEntregas = ordenadorEntregas;
    this.asignadorDeEntregas = asignadorDeEntregas;
  }

  public List<Ruta> generarRutas(
      List<Entrega> entregas, List<Camion> camiones, LocalDate fecha, UUID choferId) {
    List<Entrega> entregasOrdenadas = ordenadorEntregas.obtenerEntregasOrdenadas(entregas);
    Map<UUID, List<Entrega>> asignaciones =
        asignadorDeEntregas.asignar(entregasOrdenadas, camiones);

    return asignaciones.entrySet().stream()
        .filter(entry -> !entry.getValue().isEmpty())
        .map(entry -> construirRuta(fecha, choferId, entry.getKey(), entry.getValue()))
        .toList();
  }

  private Ruta construirRuta(
      LocalDate fecha, UUID choferId, UUID camionId, List<Entrega> entregasAsignadas) {
    Ruta ruta = new Ruta(fecha, choferId, camionId);
    entregasAsignadas.forEach(entrega -> ruta.agregarEntrega(entrega.getId()));
    return ruta;
  }
}
