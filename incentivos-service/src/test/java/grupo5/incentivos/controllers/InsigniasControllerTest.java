package grupo5.incentivos.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.services.IInsigniasService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class InsigniasControllerTest {

  private InsigniasController controller;

  @Mock private IInsigniasService insigniasService;

  @BeforeEach
  void setUp() {
    controller = new InsigniasController(insigniasService);
  }

  @Test
  void obtenerInsignias_deberiaRetornarStatus200OkYListaInsignias() {
    UUID donanteId = UUID.randomUUID();
    List<InsigniaDTO> dtos =
        List.of(new InsigniaDTO("Insignia 1", "Desc", "/icon.png", true, LocalDate.now()));

    when(insigniasService.obtenerInsignias(donanteId)).thenReturn(dtos);

    ResponseEntity<List<InsigniaDTO>> response = controller.obtenerInsignias(donanteId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(dtos, response.getBody());
    verify(insigniasService, times(1)).obtenerInsignias(donanteId);
  }

  @Test
  void configurarVisibilidad_deberiaRetornarStatus200Ok() {
    UUID donanteId = UUID.randomUUID();

    ResponseEntity<Void> response =
        controller.configurarVisibilidad(donanteId, "Insignia 1", false);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(insigniasService, times(1))
        .configurarVisibilidadInsignia(donanteId, "Insignia 1", false);
  }
}
