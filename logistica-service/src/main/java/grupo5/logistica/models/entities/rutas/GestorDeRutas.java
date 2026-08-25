package grupo5.logistica.models.entities.rutas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.util.List;
import java.util.Objects;

public final class GestorDeRutas {

  private GestorDeRutas() {}

  public static void agregarEntrega(Ruta ruta, Entrega entrega) {
    if (ruta == null || entrega == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (ruta.getEstado() != EstadoRuta.PENDIENTE
        || entrega.getEstadoActual() != EstadoEntrega.PENDIENTE
        || entrega.getIdRuta() != null
        || ruta.getEntregaIds().contains(entrega.getId())) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }

    ruta.agregarEntrega(entrega.getId());
    entrega.asignarRuta(ruta.getId());
  }

  public static void cambiarEstado(
      Ruta ruta,
      Chofer chofer,
      Camion camion,
      List<Entrega> entregas,
      EstadoRuta estadoNuevo,
      String actor) {
    if (estadoNuevo == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    switch (estadoNuevo) {
      case EN_TRASLADO -> iniciarRuta(ruta, chofer, camion, entregas, actor);
      case COMPLETADA -> completarRuta(ruta, chofer, camion);
      case PENDIENTE -> throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }
  }

  public static void iniciarRuta(
      Ruta ruta, Chofer chofer, Camion camion, List<Entrega> entregas, String actor) {
    validarColaboradores(ruta, chofer, camion);
    if (entregas == null || entregas.isEmpty() || actor == null || actor.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (ruta.getEstado() != EstadoRuta.PENDIENTE
        || !chofer.estaDisponibleParaAsignar()
        || !camion.estaDisponibleParaAsignar()
        || !Objects.equals(ruta.getChoferId(), chofer.getId())
        || !Objects.equals(ruta.getCamionId(), camion.getId())
        || !ruta.getEntregaIds().equals(entregas.stream().map(Entrega::getId).toList())
        || entregas.stream().anyMatch(e -> e.getEstadoActual() != EstadoEntrega.PENDIENTE)) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }

    camion.asignarARuta(ruta.getId());
    chofer.asignarARuta(ruta.getId());
    ruta.iniciarRuta();
    entregas.forEach(entrega -> entrega.iniciarRuta(actor));
  }

  public static void completarRuta(Ruta ruta, Chofer chofer, Camion camion) {
    validarColaboradores(ruta, chofer, camion);
    if (ruta.getEstado() != EstadoRuta.EN_TRASLADO
        || !Objects.equals(ruta.getId(), chofer.getRutaId())
        || !Objects.equals(ruta.getId(), camion.getRutaId())) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }

    ruta.completarRuta();
    camion.completarRuta();
    chofer.completarRuta();
  }

  private static void validarColaboradores(Ruta ruta, Chofer chofer, Camion camion) {
    if (ruta == null || chofer == null || camion == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }
}
