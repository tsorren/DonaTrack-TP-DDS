package grupo5.incentivos.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.fixtures.IncentivosFixtures;
import grupo5.incentivos.services.IGestionDonanteService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class DonanteIncentivosControllerTest {

  private DonanteIncentivosController controller;

  @Mock private IGestionDonanteService gestionDonanteService;

  @BeforeEach
  void setUp() {
    controller = new DonanteIncentivosController(gestionDonanteService);
  }

  @Test
  void registrarDonante_deberiaRetornarStatus201CreatedYBody() {
    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    RegistrarDonanteRequest request =
        IncentivosFixtures.registrarDonante(donanteId, personaId, "Carlos");
    DonanteRegistradoDTO responseDto = new DonanteRegistradoDTO(donanteId, "COLABORADOR");

    when(gestionDonanteService.registrarDonante(any())).thenReturn(responseDto);

    ResponseEntity<DonanteRegistradoDTO> response = controller.registrarDonante(donanteId, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(responseDto, response.getBody());
    verify(gestionDonanteService, times(1)).registrarDonante(request);
  }

  @Test
  void darDeBaja_deberiaRetornarStatus204NoContent() {
    UUID donanteId = UUID.randomUUID();

    ResponseEntity<Void> response = controller.darDeBaja(donanteId);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(gestionDonanteService, times(1)).darDeBaja(donanteId);
  }

  @Test
  void modificarDonante_deberiaRetornarStatus200Ok() {
    UUID donanteId = UUID.randomUUID();
    ModificarDonanteRequest request = IncentivosFixtures.modificarDonante("Nuevo Nombre");

    ResponseEntity<Void> response = controller.modificarDonante(donanteId, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(gestionDonanteService, times(1)).modificarDonante(donanteId, request);
  }
}
