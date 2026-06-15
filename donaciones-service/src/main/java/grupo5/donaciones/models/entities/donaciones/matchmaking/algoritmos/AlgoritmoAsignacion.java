package grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class AlgoritmoAsignacion {

    public List<Propuesta> ejecutar(List<Necesidad> necesidades, List<DonacionIndependiente> donaciones) {
        if (necesidades == null) throw new ValidationException(ErrorCatalog.ALGORITMO_NECESIDADES_NULAS);
        if (donaciones == null) throw new ValidationException(ErrorCatalog.ALGORITMO_DONACIONES_NULAS);
        List<Propuesta> propuestas = new ArrayList<>();
        StockDeDonaciones stock = new StockDeDonaciones(donaciones);

        for (Necesidad necesidad : ordenarNecesidades(necesidades)) {
            if (propuestas.size() >= 10) break;

            List<DonacionIndependiente> candidatas = filtrarDonaciones(necesidad, stock.disponibles());
            if (candidatas.isEmpty()) continue;

            Propuesta propuesta = crearPropuestaDesde(necesidad, candidatas, stock);
            stock.registrarReservas(propuesta);
            propuestas.add(propuesta);
        }
        return propuestas;
    }

    private Propuesta crearPropuestaDesde(
            Necesidad necesidad,
            List<DonacionIndependiente> donacionesOrdenadas,
            StockDeDonaciones stock) {

        Propuesta propuesta = new Propuesta();
        propuesta.setNecesidadQueSatisface(necesidad);
        propuesta.setEstado(EstadoPropuesta.PENDIENTE);
        propuesta.setFechaCreacion(LocalDateTime.now());

        int cantidadRestante = necesidad.getCantidadNecesitada() - necesidad.cantidadAcumulada();

        for (DonacionIndependiente donacion : donacionesOrdenadas) {
            if (cantidadRestante <= 0) break;
            int disponible = stock.disponibleDe(donacion);
            if (disponible <= 0) continue;

            int aAsignar = Math.min(disponible, cantidadRestante);
            propuesta.agregarFragmentacion(donacion, aAsignar);
            cantidadRestante -= aAsignar;
        }

        return propuesta;
    }

    protected boolean mismaSubcategoria(DonacionIndependiente donacion, Necesidad necesidad) {
        return donacion.getSubCategoria() != null
            && donacion.getSubCategoria().equals(necesidad.getSubcategoria());
    }

    public List<Necesidad> ordenarNecesidades(List<Necesidad> necesidades) {
        if (necesidades == null) throw new ValidationException(ErrorCatalog.ALGORITMO_NECESIDADES_NULAS);
        return necesidades;
    }

    protected void validarParametrosFiltrado(Necesidad necesidad, List<DonacionIndependiente> donaciones) {
        if (necesidad == null) throw new ValidationException(ErrorCatalog.ALGORITMO_NECESIDAD_NULA);
        if (donaciones == null) throw new ValidationException(ErrorCatalog.ALGORITMO_DONACIONES_NULAS);
    }

    public abstract List<DonacionIndependiente> filtrarDonaciones(
            Necesidad necesidad, List<DonacionIndependiente> donaciones);
}
