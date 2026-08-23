package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.camiones.GestorDeCamiones;
import grupo5.logistica.models.entities.camiones.SolicitudNuevoCamion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.entregas.ConfirmacionRecepcion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.GestorDeEntregas;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.GestorDeRutas;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GestoresDeDominioTest {

  @Test
  void gestorDeCamionesCreaYRegistraCadaCambioDeEstado() {
    Camion camion =
        GestorDeCamiones.procesarSolicitudNuevoCamion(
                new SolicitudNuevoCamion("AB123CD", 20f, 3f, 5000f, List.of()))
            .orElseThrow();

    GestorDeCamiones.cambiarEstado(camion, EstadoCamion.DESHABILITADO);
    GestorDeCamiones.cambiarEstado(camion, EstadoCamion.DISPONIBLE);

    assertEquals(EstadoCamion.DISPONIBLE, camion.getEstado());
    assertEquals(2, camion.getHistorialEstado().size());
    assertEquals(EstadoCamion.DESHABILITADO, camion.getHistorialEstado().getFirst().estadoNuevo());
  }

  @Test
  void gestorDeEntregasProcesaLaSolicitudSinConocerDTOs() {
    Entrega entrega = crearEntrega();
    entrega.iniciarRuta("chofer");

    GestorDeEntregas.cambiarEstado(
        new ConfirmacionRecepcion(entrega, "beneficiaria", "https://foto.test/recepcion.jpg"));

    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstadoActual());
    assertEquals("https://foto.test/recepcion.jpg", entrega.getFotoRecepcionUrl());
  }

  @Test
  void gestorDeRutasCoordinaLosCuatroAgregadosYRegistraHistorial() {
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");
    Entrega entrega = crearEntrega();
    Ruta ruta = new Ruta(LocalDate.now(), chofer.getId(), camion.getId());
    GestorDeRutas.agregarEntrega(ruta, entrega);

    GestorDeRutas.iniciarRuta(ruta, chofer, camion, List.of(entrega), "Ada Lovelace");

    assertEquals(EstadoRuta.EN_TRASLADO, ruta.getEstado());
    assertEquals(EstadoCamion.EN_RUTA, camion.getEstado());
    assertEquals(EstadoChofer.EN_RUTA, chofer.getEstado());
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstadoActual());
    assertEquals(1, ruta.getHistorialEstado().size());
    assertEquals(1, camion.getHistorialEstado().size());
    assertEquals(1, chofer.getHistorialEstados().size());
  }

  @Test
  void gestorDeRutasValidaAntesDeModificarLosAgregados() {
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");
    Entrega entrega = crearEntrega();
    Ruta ruta = new Ruta(LocalDate.now(), chofer.getId(), camion.getId());
    GestorDeRutas.agregarEntrega(ruta, entrega);

    assertThrows(
        ValidationException.class,
        () ->
            GestorDeRutas.iniciarRuta(
                ruta, chofer, camion, List.of(crearEntrega()), "Ada Lovelace"));

    assertEquals(EstadoRuta.PENDIENTE, ruta.getEstado());
    assertEquals(EstadoCamion.DISPONIBLE, camion.getEstado());
    assertEquals(EstadoChofer.DISPONIBLE, chofer.getEstado());
    assertNull(camion.getRutaId());
  }

  private static Entrega crearEntrega() {
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("CABA", provincia);
    Direccion direccion = new Direccion("Calle", 123, null, null, "1000", localidad);
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), direccion, 10f, 2f);
  }
}
