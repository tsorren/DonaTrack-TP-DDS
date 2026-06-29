package grupo5.donaciones.infrastructure.algoritmos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public abstract class AlgoritmoAsignacion {

  public List<Propuesta> ejecutar(
      List<Necesidad> necesidades, List<DonacionIndependiente> donaciones) {
    List<Propuesta> propuestas = new ArrayList<>();
    StockDeDonaciones stock = new StockDeDonaciones(donaciones);

    for (Necesidad necesidad : ordenarNecesidades(necesidades)) {
      if (propuestas.size() >= 10) break;

      List<DonacionIndependiente> candidatas = filtrarDonaciones(necesidad, stock.disponibles());
      if (!candidatas.isEmpty()) {
        Propuesta propuesta = crearPropuestaDesde(necesidad, candidatas, stock);
        stock.registrarReservas(propuesta);
        propuestas.add(propuesta);
      }
    }
    return propuestas;
  }

  private static Propuesta crearPropuestaDesde(
      Necesidad necesidad,
      List<DonacionIndependiente> donacionesOrdenadas,
      StockDeDonaciones stock) {

    Propuesta propuesta = new Propuesta();
    propuesta.setNecesidadQueSatisface(necesidad);
    propuesta.setEstado(EstadoPropuesta.PENDIENTE);
    propuesta.setFechaCreacion(LocalDateTime.now(ZoneId.systemDefault()));

    int cantidadRestante = necesidad.getCantidadNecesitada() - necesidad.cantidadAcumulada();

    for (int i = 0; i < donacionesOrdenadas.size() && cantidadRestante > 0; i++) {
      DonacionIndependiente donacion = donacionesOrdenadas.get(i);
      int disponible = stock.disponibleDe(donacion);
      if (disponible > 0) {
        int aAsignar = Math.min(disponible, cantidadRestante);
        propuesta.agregarFragmentacion(donacion, aAsignar);
        cantidadRestante -= aAsignar;
      }
    }

    return propuesta;
  }

  protected boolean mismaSubcategoria(DonacionIndependiente donacion, Necesidad necesidad) {
    return donacion.getSubcategoriaId() != null
        && necesidad.getSubcategoriaId() != null
        && donacion.getSubcategoriaId().equals(necesidad.getSubcategoriaId());
  }

  public List<Necesidad> ordenarNecesidades(List<Necesidad> necesidades) {
    if (necesidades == null)
      throw new ValidationException(ErrorCatalog.ALGORITMO_NECESIDADES_NULAS);
    return necesidades;
  }

  public abstract List<DonacionIndependiente> filtrarDonaciones(
      Necesidad necesidad, List<DonacionIndependiente> donaciones);
}
