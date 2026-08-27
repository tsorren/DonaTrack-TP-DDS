package grupo5.incentivos.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.fixtures.IncentivosFixturesTest;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.services.IMisionesDonacionService;
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
class MisionesDonacionControllerTest {

  private MisionesDonacionController controller;

  @Mock private IMisionesDonacionService misionesDonacionService;

  @BeforeEach
  void setUp() {
    controller = new MisionesDonacionController(misionesDonacionService);
  }

  @Test
  void procesarDonacion_deberiaRetornarStatus200Ok() {
    NuevaDonacionRequest request = IncentivosFixturesTest.nuevaDonacion(UUID.randomUUID());

    ResponseEntity<Void> response = controller.procesarDonacion(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(misionesDonacionService, times(1)).procesarDonacion(request);
  }

  @Test
  void procesarDonacionExitosa_deberiaRetornarStatus200Ok() {
    DonacionExitosaRequest request = IncentivosFixturesTest.donacionExitosa(UUID.randomUUID());

    ResponseEntity<Void> response = controller.procesarDonacionExitosa(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(misionesDonacionService, times(1)).procesarDonacionExitosa(request);
  }

  @Test
  void obtenerMisiones_deberiaRetornarStatus200OkYMisiones() {
    UUID donanteId = UUID.randomUUID();
    List<MisionDTO> dtos =
        List.of(
            new MisionDTO(
                "Mision 1", "Desc", CategoriaDonante.COLABORADOR, 3, 1, 33, 2, false, null, null),
            new MisionDTO(
                "Mision 2", "Desc", CategoriaDonante.COLABORADOR, 5, 5, 100, 0, true, null, null));

    when(misionesDonacionService.obtenerMisiones(donanteId)).thenReturn(dtos);

    ResponseEntity<List<MisionDTO>> response = controller.obtenerMisiones(donanteId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(dtos, response.getBody());
    verify(misionesDonacionService, times(1)).obtenerMisiones(donanteId);
  }
}
