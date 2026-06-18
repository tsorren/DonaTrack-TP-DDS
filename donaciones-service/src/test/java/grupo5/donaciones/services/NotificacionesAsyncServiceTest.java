package grupo5.donaciones.services;

import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import java.util.UUID;

import grupo5.donaciones.services.impl.NotificacionesAsyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionesAsyncServiceTest {

  @Mock private NotificacionesFeignClient client;

  @InjectMocks private NotificacionesAsyncService service;

  @Test
  void sincronizarPersona_deberiaInvocarCliente_CuandoNoHayErrores() {
    PersonaReplicaDTO dto = mock(PersonaReplicaDTO.class);
    when(dto.id()).thenReturn(UUID.randomUUID());

    service.sincronizarPersona(dto);

    verify(client, times(1)).sincronizarPersona(dto);
  }

  @Test
  void sincronizarPersona_deberiaCapturarExcepcionYNoPropagarla_CuandoClienteFalla() {
    PersonaReplicaDTO dto = mock(PersonaReplicaDTO.class);
    when(dto.id()).thenReturn(UUID.randomUUID());
    doThrow(new RuntimeException("Error de conexión")).when(client).sincronizarPersona(dto);

    // No debe lanzar excepción
    service.sincronizarPersona(dto);

    verify(client, times(1)).sincronizarPersona(dto);
  }

  @Test
  void anonimizarPersona_deberiaInvocarCliente_CuandoNoHayErrores() {
    UUID id = UUID.randomUUID();

    service.anonimizarPersona(id);

    verify(client, times(1)).anonimizarPersona(id);
  }

  @Test
  void anonimizarPersona_deberiaCapturarExcepcionYNoPropagarla_CuandoClienteFalla() {
    UUID id = UUID.randomUUID();
    doThrow(new RuntimeException("Error de conexión")).when(client).anonimizarPersona(id);

    // No debe lanzar excepción
    service.anonimizarPersona(id);

    verify(client, times(1)).anonimizarPersona(id);
  }
}
