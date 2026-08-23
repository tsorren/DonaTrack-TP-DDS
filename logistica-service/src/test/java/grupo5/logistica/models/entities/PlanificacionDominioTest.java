package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.planificacion.AlgoritmoOrdenadorSimple;
import grupo5.logistica.models.entities.planificacion.AsignadorDeEntregasPorDimension;
import grupo5.logistica.models.entities.planificacion.PlanificadorDeRutas;
import grupo5.logistica.models.entities.rutas.GeneradorDeRutas;
import grupo5.logistica.models.entities.rutas.GeneradorLotesSimple;
import grupo5.logistica.models.entities.rutas.PlanificacionSolicitada;
import grupo5.logistica.models.entities.rutas.RespuestaPlanificacion;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PlanificacionDominioTest {

  @Test
  void planificarParteCientoUnaEntregasEnDosSolicitudes() {
    GeneradorDeRutas generador = new GeneradorDeRutas(new GeneradorLotesSimple());
    List<Entrega> entregas = IntStream.range(0, 101).mapToObj(i -> crearEntrega()).toList();

    List<PlanificacionSolicitada> solicitudes =
        generador.planificar(
            entregas,
            List.of(new Camion("AB123CD", 2000f, 10000f, 3f)),
            List.of(new Chofer("Ada", "Lovelace", "LIC-1", "1111")),
            LocalDate.now(),
            100);

    assertEquals(2, solicitudes.size());
    assertEquals(100, solicitudes.getFirst().entregas().size());
    assertEquals(1, solicitudes.getLast().entregas().size());
  }

  @Test
  void elPlanificadorNoAsignaLaRutaHastaProcesarLaRespuesta() {
    Entrega entrega = crearEntrega();
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");
    GeneradorDeRutas generador = new GeneradorDeRutas(new GeneradorLotesSimple());
    PlanificacionSolicitada solicitud =
        generador
            .planificar(List.of(entrega), List.of(camion), List.of(chofer), LocalDate.now(), 100)
            .getFirst();

    RespuestaPlanificacion respuesta =
        new PlanificadorDeRutas(
                new AlgoritmoOrdenadorSimple(), new AsignadorDeEntregasPorDimension())
            .procesarSolicitud(solicitud);

    assertNull(entrega.getIdRuta());

    List<Ruta> rutas = generador.generarRutas(respuesta);

    assertEquals(1, rutas.size());
    assertEquals(rutas.getFirst().getId(), entrega.getIdRuta());
  }

  private static Entrega crearEntrega() {
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("CABA", provincia);
    Direccion direccion = new Direccion("Calle", 123, null, null, "1000", localidad);
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), direccion, 10f, 2f);
  }
}
