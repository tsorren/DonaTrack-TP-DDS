package grupo5.logistica.models.entities.rutas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeneradorDeRutas {

  public static final int MAX_ENTREGAS_POR_SOLICITUD = 100;

  private final GeneradorLotes generadorLotes;

  public GeneradorDeRutas(GeneradorLotes generadorLotes) {
    if (generadorLotes == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.generadorLotes = generadorLotes;
  }

  public List<PlanificacionSolicitada> planificar(
      List<Entrega> entregas,
      List<Camion> camiones,
      List<Chofer> choferes,
      LocalDate fecha,
      int maximoPorLote) {
    if (entregas == null) {
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_ENTREGAS_NULAS);
    }
    if (camiones == null || choferes == null || fecha == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    int limite = Math.min(maximoPorLote, MAX_ENTREGAS_POR_SOLICITUD);
    return generadorLotes.particionarEnLotes(entregas, limite).stream()
        .map(
            lote ->
                new PlanificacionSolicitada(
                    UUID.randomUUID(), fecha, limite, List.of(lote), camiones, choferes))
        .toList();
  }

  public List<Ruta> generarRutas(RespuestaPlanificacion respuesta) {
    if (respuesta == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    List<Ruta> rutas = new ArrayList<>();
    for (Map.Entry<Camion, List<Entrega>> asignacion : respuesta.datos().entrySet()) {
      Chofer chofer = respuesta.choferesPorCamion().get(asignacion.getKey());
      if (chofer == null || asignacion.getValue().isEmpty()) {
        continue;
      }

      Ruta ruta = new Ruta(respuesta.fecha(), chofer.getId(), asignacion.getKey().getId());
      asignacion.getValue().forEach(entrega -> GestorDeRutas.agregarEntrega(ruta, entrega));
      rutas.add(ruta);
    }
    return List.copyOf(rutas);
  }
}
