package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.dto.logistica.AvisoProveedorRequestDTO;
import grupo5.donaciones.dto.logistica.TipoAvisoProveedor;
import grupo5.donaciones.infrastructure.logistica.ProcesadorEventosLogistica;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.ISolicitudesEntregaRepository;
import grupo5.donaciones.services.impl.AvisosProveedorService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AvisosProveedorServiceTest {

  private static final String PROVEEDOR = "externo";
  private static final String ORIGEN = "http:externo";

  private ISolicitudesEntregaRepository solicitudesRepository;
  private ProcesadorEventosLogistica procesador;
  private AvisosProveedorService service;

  @BeforeEach
  void setUp() {
    solicitudesRepository = mock(ISolicitudesEntregaRepository.class);
    procesador = mock(ProcesadorEventosLogistica.class);
    service = new AvisosProveedorService(solicitudesRepository, procesador);
  }

  private UUID donacionAsignadaA(String proveedorId) {
    UUID donacionId = UUID.randomUUID();
    SolicitudEntrega solicitud = new SolicitudEntrega(donacionId, LocalDateTime.now());
    solicitud.asignarProveedor(proveedorId);
    when(solicitudesRepository.findActivaPorDonacion(donacionId))
        .thenReturn(Optional.of(solicitud));
    return donacionId;
  }

  private static AvisoProveedorRequestDTO aviso(
      TipoAvisoProveedor tipo,
      UUID rutaId,
      UUID entregaId,
      UUID donacionId,
      List<UUID> donaciones) {
    return new AvisoProveedorRequestDTO(
        tipo,
        rutaId,
        entregaId,
        donacionId,
        donaciones,
        "AB123CD",
        "http://mapa",
        "sin acceso",
        true);
  }

  @Test
  void rutaAsignada_deUnaDonacionDelProveedor_seProcesaConSuOrigen() {
    UUID donacionId = donacionAsignadaA(PROVEEDOR);
    UUID rutaId = UUID.randomUUID();

    service.registrarAviso(
        PROVEEDOR, aviso(TipoAvisoProveedor.RUTA_ASIGNADA, rutaId, null, donacionId, null));

    verify(procesador)
        .procesarRutaAsignada(
            argThat(
                (EventoRutaAsignada e) ->
                    e.rutaId().equals(rutaId) && e.donacionIndependienteId().equals(donacionId)),
            eq(ORIGEN));
  }

  @Test
  void rutaIniciada_conTodasLasDonacionesDelProveedor_seProcesa() {
    UUID d1 = donacionAsignadaA(PROVEEDOR);
    UUID d2 = donacionAsignadaA(PROVEEDOR);
    UUID rutaId = UUID.randomUUID();

    service.registrarAviso(
        PROVEEDOR, aviso(TipoAvisoProveedor.RUTA_INICIADA, rutaId, null, null, List.of(d1, d2)));

    verify(procesador)
        .procesarRutaIniciada(
            argThat(
                (EventoRutaIniciada e) ->
                    e.rutaId().equals(rutaId)
                        && e.donacionesIndependientesIds().equals(List.of(d1, d2))
                        && "http://mapa".equals(e.urlMapa())),
            eq(ORIGEN));
  }

  @Test
  void entregaExitosa_seProcesaConLaPatente() {
    UUID donacionId = donacionAsignadaA(PROVEEDOR);
    UUID entregaId = UUID.randomUUID();

    service.registrarAviso(
        PROVEEDOR, aviso(TipoAvisoProveedor.ENTREGA_EXITOSA, null, entregaId, donacionId, null));

    verify(procesador)
        .procesarEntregaExitosa(
            argThat(
                (EventoEntregaExitosa e) ->
                    e.entregaId().equals(entregaId)
                        && e.donacionIndependienteId().equals(donacionId)
                        && "AB123CD".equals(e.patenteCamion())),
            eq(ORIGEN));
  }

  @Test
  void entregaFallida_seProcesaConJustificacionYReplanificable() {
    UUID donacionId = donacionAsignadaA(PROVEEDOR);
    UUID entregaId = UUID.randomUUID();

    service.registrarAviso(
        PROVEEDOR, aviso(TipoAvisoProveedor.ENTREGA_FALLIDA, null, entregaId, donacionId, null));

    verify(procesador)
        .procesarEntregaFallida(
            argThat(
                (EventoEntregaFallida e) ->
                    e.entregaId().equals(entregaId)
                        && "sin acceso".equals(e.justificacion())
                        && e.replanificable()),
            eq(ORIGEN));
  }

  @Test
  void aviso_deUnProveedorSobreUnaDonacionDeOtro_seRechazaYNoSeProcesa() {
    UUID donacionId = donacionAsignadaA("donatrack");

    assertThrows(
        RecursoNoEncontradoException.class,
        () ->
            service.registrarAviso(
                PROVEEDOR,
                aviso(
                    TipoAvisoProveedor.ENTREGA_EXITOSA,
                    null,
                    UUID.randomUUID(),
                    donacionId,
                    null)));

    verify(procesador, never()).procesarEntregaExitosa(any(), any());
  }

  @Test
  void aviso_deUnaDonacionSinSolicitud_seRechaza() {
    UUID donacionId = UUID.randomUUID();
    when(solicitudesRepository.findActivaPorDonacion(donacionId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class,
        () ->
            service.registrarAviso(
                PROVEEDOR,
                aviso(
                    TipoAvisoProveedor.RUTA_ASIGNADA, UUID.randomUUID(), null, donacionId, null)));

    verify(procesador, never()).procesarRutaAsignada(any(), any());
  }

  @Test
  void rutaIniciada_conUnaDonacionAjena_noProcesaNinguna() {
    UUID propia = donacionAsignadaA(PROVEEDOR);
    UUID ajena = donacionAsignadaA("donatrack");

    assertThrows(
        RecursoNoEncontradoException.class,
        () ->
            service.registrarAviso(
                PROVEEDOR,
                aviso(
                    TipoAvisoProveedor.RUTA_INICIADA,
                    UUID.randomUUID(),
                    null,
                    null,
                    List.of(propia, ajena))));

    verify(procesador, never()).procesarRutaIniciada(any(), any());
  }

  @Test
  void aviso_sinIdentificadorDeEntrega_esInvalido() {
    UUID donacionId = donacionAsignadaA(PROVEEDOR);

    assertThrows(
        ValidationException.class,
        () ->
            service.registrarAviso(
                PROVEEDOR,
                aviso(TipoAvisoProveedor.ENTREGA_EXITOSA, null, null, donacionId, null)));

    verify(procesador, never()).procesarEntregaExitosa(any(), any());
  }

  @Test
  void rutaIniciada_sinDonaciones_esInvalido() {
    assertThrows(
        ValidationException.class,
        () ->
            service.registrarAviso(
                PROVEEDOR,
                aviso(TipoAvisoProveedor.RUTA_INICIADA, UUID.randomUUID(), null, null, List.of())));
  }

  @Test
  void aviso_sinTipoOSinProveedor_esInvalido() {
    assertThrows(ValidationException.class, () -> service.registrarAviso(PROVEEDOR, null));
    assertThrows(
        ValidationException.class,
        () ->
            service.registrarAviso(
                " ", aviso(TipoAvisoProveedor.RUTA_ASIGNADA, null, null, null, null)));
  }
}
