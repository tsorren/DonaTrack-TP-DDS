package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.camiones.GestorDeCamiones;
import grupo5.logistica.models.entities.camiones.SolicitudNuevoCamion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.entregas.*;
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
  void gestorDeCamionesNoCreaUnCamionCuandoLaPatenteYaExiste() {
    assertTrue(
        GestorDeCamiones.procesarSolicitudNuevoCamion(
                new SolicitudNuevoCamion("AB123CD", 20f, 3f, 5000f, List.of("AB123CD")))
            .isEmpty());
  }

  @Test
  void gestorDeCamionesNoRegistraHistorialCuandoLaTransicionEsInvalida() {
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);

    assertThrows(
        ValidationException.class,
        () -> GestorDeCamiones.cambiarEstado(camion, EstadoCamion.EN_RUTA));

    assertTrue(camion.getHistorialEstado().isEmpty());
  }

  @Test
  void choferCambiarEstadoDelegaYConservaElHistorial() {
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");

    chofer.cambiarEstado(EstadoChofer.DESHABILITADO);
    chofer.cambiarEstado(EstadoChofer.DISPONIBLE);

    assertEquals(EstadoChofer.DISPONIBLE, chofer.getEstado());
    assertEquals(2, chofer.getHistorialEstados().size());
    assertEquals(EstadoChofer.DESHABILITADO, chofer.getHistorialEstados().getFirst().estadoNuevo());
  }

  @Test
  void choferCambiarEstadoNoRegistraHistorialCuandoLaTransicionEsInvalida() {
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");

    assertThrows(ValidationException.class, () -> chofer.cambiarEstado(EstadoChofer.EN_RUTA));

    assertTrue(chofer.getHistorialEstados().isEmpty());
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
  void gestorDeEntregasResuelveTodasLasTransicionesDeLaInterfazSellada() {
    Entrega entrega = crearEntrega();
    entrega.iniciarRuta("chofer");

    GestorDeEntregas.cambiarEstado(
        new NoRecepcion(entrega, "beneficiaria", "domicilio cerrado", true));
    assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstadoActual());

    GestorDeEntregas.cambiarEstado(new RevisionEntrega(entrega, "administrador"));
    assertEquals(EstadoEntrega.REVISION, entrega.getEstadoActual());

    GestorDeEntregas.cambiarEstado(new RegresoDeposito(entrega, "administrador"));
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstadoActual());
    assertNull(entrega.getIdRuta());
  }

  @Test
  void gestorDeRutasCoordinaLosCuatroAgregadosYRegistraHistorial() {
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");
    Entrega entrega = crearEntrega();
    Ruta ruta = new Ruta(LocalDate.now(), chofer.getId(), camion.getId());
    GestorDeRutas.agregarEntrega(ruta, entrega);

    GestorDeRutas.iniciarRuta(ruta, camion, chofer, List.of(entrega), "Ada Lovelace");

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

    List<Entrega> entregas = List.of(crearEntrega());
    assertThrows(
        ValidationException.class,
        () -> GestorDeRutas.iniciarRuta(ruta, camion, chofer, entregas, "Ada Lovelace"));

    assertEquals(EstadoRuta.PENDIENTE, ruta.getEstado());
    assertEquals(EstadoCamion.DISPONIBLE, camion.getEstado());
    assertEquals(EstadoChofer.DISPONIBLE, chofer.getEstado());
    assertNull(camion.getRutaId());
  }

  @Test
  void gestorDeRutasCoordinaRutaCamionYChoferConElContratoMinimo() {
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");
    Ruta ruta = new Ruta(LocalDate.now(), chofer.getId(), camion.getId());
    ruta.agregarEntrega(UUID.randomUUID());

    GestorDeRutas.iniciarRuta(ruta, camion, chofer);

    assertEquals(EstadoRuta.EN_TRASLADO, ruta.getEstado());
    assertEquals(ruta.getId(), camion.getRutaId());
    assertEquals(ruta.getId(), chofer.getRutaId());
  }

  private static Entrega crearEntrega() {
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("CABA", provincia);
    Direccion direccion = new Direccion("Calle", 123, null, null, "1000", localidad);
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), direccion, 10f, 2f);
  }

  @Test
  void gestorDeRutasNoCompletaRutaSiHayEntregasEnTraslado() {
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");
    Entrega entrega = crearEntrega();
    Ruta ruta = new Ruta(LocalDate.now(), chofer.getId(), camion.getId());
    GestorDeRutas.agregarEntrega(ruta, entrega);
    GestorDeRutas.iniciarRuta(ruta, camion, chofer, List.of(entrega), "Ada Lovelace");

    // La entrega sigue EN_TRASLADO, intentar completar debe lanzar excepción
    assertThrows(
        ValidationException.class,
        () -> GestorDeRutas.completarRuta(ruta, camion, chofer, List.of(entrega)));

    assertEquals(EstadoRuta.EN_TRASLADO, ruta.getEstado());
  }

  @Test
  void gestorDeRutasCompletaRutaCuandoTodasLasEntregasEstanResueltas() {
    Camion camion = new Camion("AB123CD", 20f, 5000f, 3f);
    Chofer chofer = new Chofer("Ada", "Lovelace", "LIC-1", "1111");
    Entrega entrega = crearEntrega();
    Ruta ruta = new Ruta(LocalDate.now(), chofer.getId(), camion.getId());
    GestorDeRutas.agregarEntrega(ruta, entrega);
    GestorDeRutas.iniciarRuta(ruta, camion, chofer, List.of(entrega), "Ada Lovelace");

    // Resolvemos la entrega pasándola a ENTREGADA
    entrega.confirmarEntrega("Comedor");

    GestorDeRutas.completarRuta(ruta, camion, chofer, List.of(entrega));

    assertEquals(EstadoRuta.COMPLETADA, ruta.getEstado());
    assertEquals(EstadoCamion.DISPONIBLE, camion.getEstado());
    assertEquals(EstadoChofer.DISPONIBLE, chofer.getEstado());
    assertNull(camion.getRutaId());
    assertNull(chofer.getRutaId());
  }
}
