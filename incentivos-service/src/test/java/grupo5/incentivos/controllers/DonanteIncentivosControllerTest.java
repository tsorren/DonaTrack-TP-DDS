package grupo5.incentivos.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.incentivos.dto.CambioCategoriaDTO;
import grupo5.incentivos.dto.DonantePerfilDTO;
import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.fixtures.IncentivosFixtures;
import grupo5.incentivos.models.entities.donante.CambioCategoria;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.services.IGestionDonanteService;
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

  @Test
  void obtenerDonante_deberiaRetornarStatus200OkYPerfilMapeado() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId, "Carlos");

    when(gestionDonanteService.obtenerDonante(donanteId)).thenReturn(donante);

    ResponseEntity<DonantePerfilDTO> response = controller.obtenerDonante(donanteId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(donanteId, response.getBody().donanteId());
    assertEquals("Carlos", response.getBody().nombre());
    assertEquals(CategoriaDonante.COLABORADOR, response.getBody().categoria());
    verify(gestionDonanteService, times(1)).obtenerDonante(donanteId);
  }

  @Test
  void obtenerAscensos_cuandoTieneHistorial_deberiaRetornarListaMapeada() {
    UUID donanteId = UUID.randomUUID();
    CambioCategoria cambio =
        new CambioCategoria(CategoriaDonante.COLABORADOR, CategoriaDonante.SOSTENEDOR);
    DonanteIncentivos donante =
        new DonanteIncentivos(
            donanteId,
            donanteId,
            "Carlos",
            CategoriaDonante.SOSTENEDOR,
            LocalDate.now(),
            List.of(cambio),
            List.of(),
            List.of(),
            null);

    when(gestionDonanteService.obtenerDonante(donanteId)).thenReturn(donante);

    ResponseEntity<List<CambioCategoriaDTO>> response = controller.obtenerAscensos(donanteId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals(CategoriaDonante.COLABORADOR, response.getBody().get(0).categoriaAnterior());
    assertEquals(CategoriaDonante.SOSTENEDOR, response.getBody().get(0).categoriaNueva());
    verify(gestionDonanteService, times(1)).obtenerDonante(donanteId);
  }

  @Test
  void obtenerAscensos_sinHistorial_deberiaRetornarListaVacia() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId, "Carlos");

    when(gestionDonanteService.obtenerDonante(donanteId)).thenReturn(donante);

    ResponseEntity<List<CambioCategoriaDTO>> response = controller.obtenerAscensos(donanteId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(List.of(), response.getBody());
  }
}
