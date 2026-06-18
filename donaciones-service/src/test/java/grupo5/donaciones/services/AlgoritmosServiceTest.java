package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.infrastructure.analizadores.ComparadorTexto;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.impl.DonacionIndependienteRepository;
import grupo5.donaciones.models.repositories.impl.NecesidadRepository;
import grupo5.donaciones.models.repositories.impl.PropuestaRepository;
import grupo5.donaciones.services.impl.AlgoritmosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AlgoritmosServiceTest {

  private IDonacionesIndependientesRepository donacionRepositoryMock;
  private NecesidadRepository necesidadRepositoryMock;
  private PropuestaRepository propuestaRepositoryMock;
  private ComparadorTexto comparadorTextoMock;

  private AlgoritmosService service;

  @BeforeEach
  void setUp() {
    donacionRepositoryMock = mock(IDonacionesIndependientesRepository.class);
    necesidadRepositoryMock = mock(NecesidadRepository.class);
    propuestaRepositoryMock = mock(PropuestaRepository.class);
    comparadorTextoMock = mock(ComparadorTexto.class);

    service =
        new AlgoritmosService(
            donacionRepositoryMock,
            necesidadRepositoryMock,
            propuestaRepositoryMock,
            comparadorTextoMock);
  }

  @Test
  void ejecutar_deberiaBuscarDonacionesYNecesidadesYConsolidarResultados() {
    when(donacionRepositoryMock.findEnDeposito()).thenReturn(Collections.emptyList());
    when(necesidadRepositoryMock.findInsatisfechas()).thenReturn(Collections.emptyList());

    List<Propuesta> resultado = service.ejecutar();

    assertNotNull(resultado);
    verify(donacionRepositoryMock, times(1)).findEnDeposito();
    verify(necesidadRepositoryMock, times(1)).findInsatisfechas();
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
    when(propuestaRepositoryMock.findById(id)).thenReturn(Optional.of(propuestaMock));

    service.actualizarEstadoPropuesta(id, EstadoPropuesta.APROBADA);

    verify(propuestaMock, times(1)).confirmar();
    verify(propuestaRepositoryMock, times(1)).save(propuestaMock);
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
