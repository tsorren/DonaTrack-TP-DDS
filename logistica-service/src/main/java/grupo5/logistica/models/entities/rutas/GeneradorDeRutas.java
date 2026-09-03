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
    List<List<Entrega>> lotes = generadorLotes.particionarEnLotes(entregas, limite);
    int cantidadRecursos = Math.min(camiones.size(), choferes.size());
    int cantidadSolicitudes = Math.min(lotes.size(), cantidadRecursos);
    List<PlanificacionSolicitada> solicitudes = new ArrayList<>();
    int primerRecurso = 0;

    for (int indice = 0; indice < cantidadSolicitudes; indice++) {
      int recursosRestantes = cantidadRecursos - primerRecurso;
      int solicitudesRestantes = cantidadSolicitudes - indice;
      int recursosParaSolicitud =
          (int) Math.ceil((double) recursosRestantes / solicitudesRestantes);
      int ultimoRecurso = primerRecurso + recursosParaSolicitud;
      solicitudes.add(
          new PlanificacionSolicitada(
              UUID.randomUUID(),
              fecha,
              limite,
              List.of(lotes.get(indice)),
              camiones.subList(primerRecurso, ultimoRecurso),
              choferes.subList(primerRecurso, ultimoRecurso)));
      primerRecurso = ultimoRecurso;
    }

    return List.copyOf(solicitudes);
  }

  public List<Ruta> calcularRutas(RespuestaPlanificacion respuesta) {
    if (respuesta == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    List<Ruta> rutas = new ArrayList<>();
    for (Map.Entry<Camion, List<Entrega>> asignacion : respuesta.datos().entrySet()) {
      Chofer chofer = respuesta.choferesPorCamion().get(asignacion.getKey());
      if (chofer == null || asignacion.getValue().isEmpty()) {
        continue;
      }

      rutas.add(crearRuta(respuesta.fecha(), chofer, asignacion.getKey(), asignacion.getValue()));
    }
    return List.copyOf(rutas);
  }

  private static Ruta crearRuta(
      LocalDate fecha, Chofer chofer, Camion camion, List<Entrega> entregas) {
    Ruta ruta = new Ruta(fecha, chofer.getId(), camion.getId());
    entregas.forEach(entrega -> GestorDeRutas.agregarEntrega(ruta, entrega));
    return ruta;
  }
}
