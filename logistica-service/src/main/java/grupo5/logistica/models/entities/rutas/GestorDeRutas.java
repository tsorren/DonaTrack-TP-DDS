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

  public static void iniciarRuta(
      Ruta ruta, Camion camion, Chofer chofer, List<Entrega> entregas, String actor) {
    validarInicioRuta(ruta, camion, chofer);
    if (entregas == null || entregas.isEmpty() || actor == null || actor.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (!ruta.getEntregaIds().equals(entregas.stream().map(Entrega::getId).toList())
        || entregas.stream().anyMatch(e -> e.getEstadoActual() != EstadoEntrega.PENDIENTE)) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }

    iniciarRuta(ruta, camion, chofer);
    entregas.forEach(entrega -> entrega.iniciarRuta(actor));
  }

  public static void iniciarRuta(Ruta ruta, Camion camion, Chofer chofer) {
    validarInicioRuta(ruta, camion, chofer);

    camion.asignarARuta(ruta.getId());
    chofer.asignarARuta(ruta.getId());
    ruta.iniciarRuta();
  }

  public static void completarRuta(Ruta ruta, Camion camion, Chofer chofer) {
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

  private static void validarInicioRuta(Ruta ruta, Camion camion, Chofer chofer) {
    validarColaboradores(ruta, chofer, camion);
    if (ruta.getEstado() != EstadoRuta.PENDIENTE
        || !chofer.estaDisponibleParaAsignar()
        || !camion.estaDisponibleParaAsignar()
        || !Objects.equals(ruta.getChoferId(), chofer.getId())
        || !Objects.equals(ruta.getCamionId(), camion.getId())) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }
  }
}
