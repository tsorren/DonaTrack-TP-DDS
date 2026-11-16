package grupo5.logistica.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.FuentePosicionCamion;
import grupo5.logistica.services.IEntregasService;
import grupo5.logistica.testutils.CamionMother;
import grupo5.logistica.testutils.EntregaMother;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

class ActualizadorPosicionCamionesTest {

  private static final PosicionCamion POSICION =
      new PosicionCamion(-34.6, -58.4, Instant.parse("2026-11-08T12:00:30Z"));

  private IRutasRepository rutas;
  private ICamionRepository camiones;
  private FuentePosicionCamion fuente;
  private IEntregasRepository entregas;
  private IEntregasService entregasService;
  private ActualizadorPosicionCamiones actualizador;

  @BeforeEach
  void setUp() {
    rutas = mock(IRutasRepository.class);
    camiones = mock(ICamionRepository.class);
    fuente = mock(FuentePosicionCamion.class);
    entregas = mock(IEntregasRepository.class);
    entregasService = mock(IEntregasService.class);
    actualizador =
        new ActualizadorPosicionCamiones(rutas, camiones, fuente, entregas, entregasService);
  }

  @Test
  void guardaLaPosicionDelCamionDeCadaRutaEnTraslado() {
    Camion camion = CamionMother.disponible();
    Ruta ruta = rutaDe(camion.getId());
    when(rutas.findByEstado(EstadoRuta.EN_TRASLADO)).thenReturn(List.of(ruta));
    when(fuente.posicionActual(ruta)).thenReturn(Optional.of(POSICION));
    when(camiones.findById(camion.getId())).thenReturn(Optional.of(camion));

    actualizador.actualizar();

    assertEquals(POSICION, camion.getPosicion());
    verify(camiones).save(camion);
  }

  @Test
  void unaPosicionRepetidaNoSeVuelveAGuardar() {
    Camion camion = CamionMother.disponible();
    camion.registrarPosicion(POSICION);
    Ruta ruta = rutaDe(camion.getId());
    when(rutas.findByEstado(EstadoRuta.EN_TRASLADO)).thenReturn(List.of(ruta));
    when(fuente.posicionActual(ruta)).thenReturn(Optional.of(POSICION));
    when(camiones.findById(camion.getId())).thenReturn(Optional.of(camion));

    actualizador.actualizar();

    verify(camiones, never()).save(any());
  }

  @Test
  void unConflictoDeVersionSalteaEseCamionYSigueConElResto() {
    Camion enConflicto = CamionMother.disponible();
    Camion otro = CamionMother.disponible();
    Ruta r1 = rutaDe(enConflicto.getId());
    Ruta r2 = rutaDe(otro.getId());
    when(rutas.findByEstado(EstadoRuta.EN_TRASLADO)).thenReturn(List.of(r1, r2));
    when(fuente.posicionActual(any())).thenReturn(Optional.of(POSICION));
    when(camiones.findById(enConflicto.getId())).thenReturn(Optional.of(enConflicto));
    when(camiones.findById(otro.getId())).thenReturn(Optional.of(otro));
    when(camiones.save(enConflicto)).thenThrow(new OptimisticLockingFailureException("versión"));

    actualizador.actualizar();

    verify(camiones).save(otro);
  }

  @Test
  void sinPosicionDeLaFuente_noTocaElCamion() {
    Ruta ruta = rutaDe(UUID.randomUUID());
    when(rutas.findByEstado(EstadoRuta.EN_TRASLADO)).thenReturn(List.of(ruta));
    when(fuente.posicionActual(ruta)).thenReturn(Optional.empty());

    actualizador.actualizar();

    verify(camiones, never()).findById(any());
  }

  @Test
  void marcaEnPuertaLasEntregasAlcanzadasQueSiguenEnTraslado() {
    Ruta ruta = rutaDe(UUID.randomUUID());
    Entrega enViaje = enTraslado();
    Entrega yaEnPuerta = enTraslado();
    yaEnPuerta.llegarAPuerta("CHOFER");
    when(rutas.findByEstado(EstadoRuta.EN_TRASLADO)).thenReturn(List.of(ruta));
    when(fuente.posicionActual(ruta)).thenReturn(Optional.empty());
    when(fuente.entregasAlcanzadas(ruta)).thenReturn(List.of(enViaje.getId(), yaEnPuerta.getId()));
    when(entregas.findById(enViaje.getId())).thenReturn(Optional.of(enViaje));
    when(entregas.findById(yaEnPuerta.getId())).thenReturn(Optional.of(yaEnPuerta));

    actualizador.actualizar();

    verify(entregasService).marcarEnPuerta(enViaje.getId(), ActualizadorPosicionCamiones.ACTOR);
    verify(entregasService, never())
        .marcarEnPuerta(yaEnPuerta.getId(), ActualizadorPosicionCamiones.ACTOR);
  }

  @Test
  void unaConfirmacionQueGanaLaCarreraNoCortaElResto() {
    Ruta ruta = rutaDe(UUID.randomUUID());
    Entrega confirmada = enTraslado();
    Entrega otra = enTraslado();
    when(rutas.findByEstado(EstadoRuta.EN_TRASLADO)).thenReturn(List.of(ruta));
    when(fuente.posicionActual(ruta)).thenReturn(Optional.empty());
    when(fuente.entregasAlcanzadas(ruta)).thenReturn(List.of(confirmada.getId(), otra.getId()));
    when(entregas.findById(any())).thenReturn(Optional.of(confirmada), Optional.of(otra));
    when(entregasService.marcarEnPuerta(confirmada.getId(), ActualizadorPosicionCamiones.ACTOR))
        .thenThrow(new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA));

    actualizador.actualizar();

    verify(entregasService).marcarEnPuerta(otra.getId(), ActualizadorPosicionCamiones.ACTOR);
  }

  private static Entrega enTraslado() {
    Entrega entrega = EntregaMother.pendiente();
    entrega.asignarRuta(UUID.randomUUID());
    entrega.iniciarRuta("CHOFER");
    return entrega;
  }

  private static Ruta rutaDe(UUID camionId) {
    return new Ruta(LocalDate.of(2026, 11, 8), UUID.randomUUID(), camionId);
  }
}
