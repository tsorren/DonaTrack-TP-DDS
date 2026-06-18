package grupo5.notificaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.IPersonasRepository;
import grupo5.notificaciones.services.mappers.PersonaMapper;
import java.util.ArrayList;
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

  @InjectMocks private PersonasService service;

  private Persona persona;
  private PersonaReplicaDTO replicaDTO;

  @BeforeEach
  void setUp() {
    persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Orginal Name", TipoPersona.HUMANA);
    replicaDTO = mock(PersonaReplicaDTO.class);
  }

  @Test
  void sincronizar_deberiaMapearYGuardar() {
    when(mapper.toEntity(replicaDTO)).thenReturn(persona);

    service.sincronizar(replicaDTO);

    verify(repository).save(persona);
  }

  @Test
  void anonimizar_siExiste_deberiaAnonimizarYGuardar() {
    UUID id = persona.getId();
    when(repository.findById(id)).thenReturn(Optional.of(persona));

    service.anonimizar(id);

    verify(repository).save(persona);
    assertEquals("ANONIMIZADO", persona.getDenominacion());
  }

  @Test
  void anonimizar_siNoExiste_deberiaLanzarExcepcion() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThrows(ValidationException.class, () -> service.anonimizar(id));
    verify(repository, never()).save(any());
  }
}
