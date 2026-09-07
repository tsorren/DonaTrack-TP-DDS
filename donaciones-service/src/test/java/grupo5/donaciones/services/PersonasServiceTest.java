package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.HumanaOutputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.impl.NotificacionesAsyncService;
import grupo5.donaciones.services.impl.PersonasService;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonasServiceTest {

  @Mock private IPersonasRepository repository;
  @Mock private PersonaMapper mapper;
  @Mock private NotificacionesAsyncService notificacionesAsyncService;

  @InjectMocks private PersonasService service;

  private Humana humana;
  private HumanaInputDTO inputDTO;
  private HumanaOutputDTO outputDTO;
  private PersonaReplicaDTO replicaDTO;

  @BeforeEach
  void setUp() {
    humana = new Humana("Juan", "Perez", java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    inputDTO =
        new HumanaInputDTO(
            grupo5.donaciones.models.entities.personas.TipoPersona.HUMANA,
            grupo5.donaciones.models.entities.personas.TipoDocumento.DNI,
            "12345678",
            null,
            java.util.Collections.emptyList(),
            "Juan",
            "Perez",
            grupo5.donaciones.models.entities.personas.Genero.HOMBRE,
            java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    outputDTO =
        new HumanaOutputDTO(
            grupo5.donaciones.models.entities.personas.TipoPersona.HUMANA,
            humana.getId(),
            grupo5.donaciones.models.entities.personas.TipoDocumento.DNI,
            "12345678",
            null,
            java.util.Collections.emptyList(),
            "Juan",
            "Perez",
            grupo5.donaciones.models.entities.personas.Genero.HOMBRE,
            java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    replicaDTO =
        new PersonaReplicaDTO(
            humana.getId(),
            "Juan Perez",
            grupo5.donaciones.models.entities.personas.TipoPersona.HUMANA,
            java.util.Collections.emptyList());
  }

  @Test
  void crearPersona_deberiaPersistirYSincronizar() {
    when(mapper.toEntity(inputDTO)).thenReturn(humana);
    when(repository.save(humana)).thenReturn(humana);
    when(mapper.toReplicaDTO(humana)).thenReturn(replicaDTO);
    when(mapper.toOutputDTO(humana)).thenReturn(outputDTO);

    PersonaOutputDTO result = service.crearPersona(inputDTO);

    assertNotNull(result);
    verify(repository).save(humana);
    verify(notificacionesAsyncService).sincronizarPersona(replicaDTO);
  }

  @Test
  void actualizarPersona_siExiste_deberiaModificarYSincronizar() {
    UUID id = humana.getId();
    when(repository.findById(id)).thenReturn(Optional.of(humana));
    doNothing().when(mapper).updateEntity(humana, inputDTO);
    when(repository.save(humana)).thenReturn(humana);
    when(mapper.toReplicaDTO(humana)).thenReturn(replicaDTO);
    when(mapper.toOutputDTO(humana)).thenReturn(outputDTO);

    PersonaOutputDTO result = service.actualizarPersona(id, inputDTO);

    assertNotNull(result);
    verify(mapper).updateEntity(humana, inputDTO);
    verify(repository).save(humana);
    verify(notificacionesAsyncService).sincronizarPersona(replicaDTO);
  }

  @Test
  void actualizarPersona_siNoExiste_deberiaLanzarExcepcion() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.actualizarPersona(id, inputDTO));
    verify(repository, never()).save(any());
    verify(notificacionesAsyncService, never()).sincronizarPersona(any());
  }

  @Test
  void eliminarPersona_siExiste_deberiaAnonimizarYSincronizar() {
    UUID id = humana.getId();
    when(repository.findById(id)).thenReturn(Optional.of(humana));
    when(repository.save(humana)).thenReturn(humana);
    when(mapper.toReplicaDTO(humana)).thenReturn(replicaDTO);

    service.eliminarPersona(id);

    verify(repository).save(humana);
    verify(notificacionesAsyncService).sincronizarPersona(replicaDTO);
    assertEquals(grupo5.donaciones.models.privacidad.Anonimizable.VALOR_STRING, humana.getNombre());
  }

  @Test
  void eliminarPersona_siNoExiste_deberiaLanzarExcepcion() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.eliminarPersona(id));
    verify(repository, never()).save(any());
    verify(notificacionesAsyncService, never()).sincronizarPersona(any());
  }
}
