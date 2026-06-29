package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.comunicaciones.EventoDonacionAsignadaDTO;
import grupo5.donaciones.infrastructure.analizadores.ComparadorTexto;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.impl.PropuestaRepository;
import grupo5.donaciones.services.impl.AlgoritmosService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AlgoritmosServiceTest {

  private IDonacionesIndependientesRepository donacionRepositoryMock;
  private INecesidadesRepository necesidadRepositoryMock;
  private PropuestaRepository propuestaRepositoryMock;
  private ComparadorTexto comparadorTextoMock;
  private NotificacionesFeignClient notificacionesFeignClientMock;
  private grupo5.donaciones.models.repositories.ISubcategoriasRepository
      subcategoriasRepositoryMock;
  private IDonacionesRepository donacionOriginalRepositoryMock;
  private IDonantesRepository donantesRepositoryMock;
  private grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository
      entidadesBeneficiariasRepositoryMock;
  private org.springframework.context.ApplicationEventPublisher eventPublisherMock;

  private AlgoritmosService service;

  @BeforeEach
  void setUp() {
    donacionRepositoryMock = mock(IDonacionesIndependientesRepository.class);
    necesidadRepositoryMock = mock(INecesidadesRepository.class);
    propuestaRepositoryMock = mock(PropuestaRepository.class);
    comparadorTextoMock = mock(ComparadorTexto.class);
    notificacionesFeignClientMock = mock(NotificacionesFeignClient.class);
    subcategoriasRepositoryMock =
        mock(grupo5.donaciones.models.repositories.ISubcategoriasRepository.class);
    donacionOriginalRepositoryMock = mock(IDonacionesRepository.class);
    donantesRepositoryMock = mock(IDonantesRepository.class);
    entidadesBeneficiariasRepositoryMock =
        mock(grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository.class);
    eventPublisherMock = mock(org.springframework.context.ApplicationEventPublisher.class);

    service =
        new AlgoritmosService(
            donacionRepositoryMock,
            necesidadRepositoryMock,
            propuestaRepositoryMock,
            comparadorTextoMock,
            notificacionesFeignClientMock,
            subcategoriasRepositoryMock,
            donacionOriginalRepositoryMock,
            donantesRepositoryMock,
            entidadesBeneficiariasRepositoryMock,
            eventPublisherMock);
  }

  @Test
  void ejecutar_deberiaBuscarDonacionesYNecesidadesYConsolidarResultados() {
    when(donacionRepositoryMock.findEnDeposito()).thenReturn(Collections.emptyList());
    when(necesidadRepositoryMock.findByEstaSatisfechaFalseActivaTrue())
        .thenReturn(Collections.emptyList());

    List<Propuesta> resultado = service.ejecutar();

    assertNotNull(resultado);
    verify(donacionRepositoryMock, times(1)).findEnDeposito();
    verify(necesidadRepositoryMock, times(1)).findByEstaSatisfechaFalseActivaTrue();
  }

  @Test
  void listarPropuestas_deberiaRetornarTodasLasPropuestasDelRepositorio() {
    Propuesta propuestaMock = mock(Propuesta.class);
    when(propuestaRepositoryMock.findAll()).thenReturn(List.of(propuestaMock));

    List<Propuesta> resultado = service.listarPropuestas();

    assertEquals(1, resultado.size());
    assertEquals(propuestaMock, resultado.getFirst());
    verify(propuestaRepositoryMock, times(1)).findAll();
  }

  @Test
  void actualizarEstadoPropuesta_deberiaAprobarPropuesta_CuandoEstadoEsAprobada() {
    UUID id = UUID.randomUUID();
    Propuesta propuestaMock = mock(Propuesta.class);
    when(propuestaMock.getDomainEvents()).thenReturn(List.of());
    when(propuestaRepositoryMock.findById(id)).thenReturn(Optional.of(propuestaMock));

    service.actualizarEstadoPropuesta(id, EstadoPropuesta.APROBADA);

    verify(propuestaMock, times(1)).confirmar();
    verify(propuestaRepositoryMock, times(1)).save(propuestaMock);
  }

  @Test
  void
      actualizarEstadoPropuesta_deberiaAprobarPropuesta_YEnviarNotificacion_CuandoTieneFragmentaciones() {
    UUID id = UUID.randomUUID();
    Propuesta propuesta = new Propuesta(id);

    // We need a Necesidad, Entidad, Juridica
    grupo5.donaciones.models.entities.necesidades.Necesidad necesidadMock =
        mock(grupo5.donaciones.models.entities.necesidades.Necesidad.class);
    grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria entidadMock =
        mock(grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria.class);
    UUID juridicaId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    UUID necesidadId = UUID.randomUUID();
    when(necesidadMock.getId()).thenReturn(necesidadId);
    when(entidadMock.juridicaId()).thenReturn(juridicaId);
    when(necesidadMock.getEntidadId()).thenReturn(entidadId);
    when(entidadesBeneficiariasRepositoryMock.findById(entidadId))
        .thenReturn(Optional.of(entidadMock));

    propuesta.asociarNecesidad(necesidadId);
    when(necesidadRepositoryMock.findById(necesidadId)).thenReturn(Optional.of(necesidadMock));

    // Build real DonacionIndependiente for fragmentation
    UUID donacionOriginalId = UUID.randomUUID();
    UUID donanteId = UUID.randomUUID();
    grupo5.donaciones.models.entities.donantes.Donante donanteMock =
        mock(grupo5.donaciones.models.entities.donantes.Donante.class);
    UUID donantePersonaId = UUID.randomUUID();
    when(donanteMock.personaId()).thenReturn(donantePersonaId);
    grupo5.donaciones.models.entities.donaciones.Donacion donacionOriginalMock =
        mock(grupo5.donaciones.models.entities.donaciones.Donacion.class);
    when(donacionOriginalMock.getDonanteId()).thenReturn(donanteId);
    when(donacionOriginalMock.getDescripcion()).thenReturn("Ropa de abrigo");

    // Build a real DonacionIndependiente with items
    grupo5.donaciones.models.entities.categorias.Categoria categoria =
        new grupo5.donaciones.models.entities.categorias.Categoria(
            "Ropa", false, true, grupo5.donaciones.models.entities.categorias.Unidad.UNIDADES);
    grupo5.donaciones.models.entities.categorias.Subcategoria subcategoria =
        new grupo5.donaciones.models.entities.categorias.Subcategoria(
            categoria.getId(), "Ropa Invierno");
    grupo5.donaciones.models.entities.donaciones.Bien bien =
        new grupo5.donaciones.models.entities.donaciones.Bien(
            "Campera",
            "img.png",
            java.time.LocalDate.now().plusYears(1),
            grupo5.donaciones.models.entities.donaciones.Estado.NUEVO);
    grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado bienNormalizado =
        new grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado(
            bien,
            subcategoria.getId(),
            1.0,
            grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion.ACEPTADO,
            true,
            false);
    grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente item =
        new grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente(
            bienNormalizado, 5);
    grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente donacion =
        new grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente(
            donacionOriginalId, java.util.List.of(item));

    propuesta.agregarFragmentacion(donacion, 5);

    // Service: donacionRepository.findById(f.getDonacionOriginalId()) → donacion
    when(donacionRepositoryMock.findById(donacion.getId())).thenReturn(Optional.of(donacion));
    // Service: donacionOriginalRepository.findById(di.getDonacionOriginalId()) → donacionOriginalMock
    when(donacionOriginalRepositoryMock.findById(donacionOriginalId))
        .thenReturn(Optional.of(donacionOriginalMock));
    when(donantesRepositoryMock.findById(donanteId)).thenReturn(Optional.of(donanteMock));

    when(propuestaRepositoryMock.findById(id)).thenReturn(Optional.of(propuesta));

    service.actualizarEstadoPropuesta(id, EstadoPropuesta.APROBADA);

    assertEquals(EstadoPropuesta.APROBADA, propuesta.getEstado());
    verify(propuestaRepositoryMock, times(1)).save(propuesta);
    verify(notificacionesFeignClientMock, times(1))
        .enviarEvento(any(EventoDonacionAsignadaDTO.class));
  }

  @Test
  void actualizarEstadoPropuesta_deberiaDescartarPropuesta_CuandoEstadoEsDescartada() {
    UUID id = UUID.randomUUID();
    Propuesta propuestaMock = mock(Propuesta.class);
    when(propuestaRepositoryMock.findById(id)).thenReturn(Optional.of(propuestaMock));

    service.actualizarEstadoPropuesta(id, EstadoPropuesta.DESCARTADA);

    verify(propuestaMock, times(1)).rechazar();
    verify(propuestaRepositoryMock, times(1)).save(propuestaMock);
  }

  @Test
  void actualizarEstadoPropuesta_deberiaLanzarNotFound_CuandoPropuestaNoExiste() {
    UUID id = UUID.randomUUID();
    when(propuestaRepositoryMock.findById(id)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.actualizarEstadoPropuesta(id, EstadoPropuesta.APROBADA));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    verify(propuestaRepositoryMock, never()).save(any());
  }

  @Test
  void actualizarEstadoPropuesta_deberiaLanzarBadRequest_CuandoEstadoNoEsAprobadaNiDescartada() {
    UUID id = UUID.randomUUID();
    Propuesta propuestaMock = mock(Propuesta.class);
    when(propuestaRepositoryMock.findById(id)).thenReturn(Optional.of(propuestaMock));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.actualizarEstadoPropuesta(id, EstadoPropuesta.PENDIENTE));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verify(propuestaRepositoryMock, never()).save(any());
  }
}
