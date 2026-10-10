package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InsigniasServiceTest {

  private InsigniasService service;
  private DonanteIncentivosRepository repository;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    service = new InsigniasService(repository);
  }

  @Test
  void obtenerInsignias_cuandoDonanteNoTieneInsignias_deberiaRetornarListaVacia() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    repository.save(donante);

    List<InsigniaDTO> insignias = service.obtenerInsignias(donanteId);

    assertTrue(insignias.isEmpty());
  }

  @Test
  void obtenerInsignias_cuandoDonanteTieneInsignias_deberiaRetornarTodasConSuEstadoDeVisibilidad() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    Insignia ins1 = new Insignia("Insignia 1", "Desc", "url1");
    Insignia ins2 = new Insignia("Insignia 2", "Desc", "url2");
    donante.otorgarInsignia(ins1);
    donante.otorgarInsignia(ins2);
    donante.configurarVisibilidadInsignia("Insignia 2", false);
    repository.save(donante);

    List<InsigniaDTO> insignias = service.obtenerInsignias(donanteId);

    assertEquals(2, insignias.size());
    InsigniaDTO dto1 =
        insignias.stream().filter(i -> "Insignia 1".equals(i.nombre())).findFirst().orElseThrow();
    InsigniaDTO dto2 =
        insignias.stream().filter(i -> "Insignia 2".equals(i.nombre())).findFirst().orElseThrow();

    assertTrue(dto1.visible());
    assertFalse(dto2.visible());
  }

  @Test
  void obtenerInsignias_conSoloVisiblesTrue_deberiaRetornarSoloInsigniasVisibles() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    Insignia ins1 = new Insignia("Insignia 1", "Desc", "url1");
    Insignia ins2 = new Insignia("Insignia 2", "Desc", "url2");
    donante.otorgarInsignia(ins1);
    donante.otorgarInsignia(ins2);
    donante.configurarVisibilidadInsignia("Insignia 2", false);
    repository.save(donante);

    List<InsigniaDTO> visibles = service.obtenerInsignias(donanteId, true);

    assertEquals(1, visibles.size());
    assertEquals("Insignia 1", visibles.getFirst().nombre());
    assertTrue(visibles.getFirst().visible());
  }

  @Test
  void configurarVisibilidad_cuandoInsigniaExiste_deberiaActualizarVisibilidad() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    Insignia ins = new Insignia("Insignia 1", "Desc", "url1");
    donante.otorgarInsignia(ins);
    repository.save(donante);

    service.configurarVisibilidadInsignia(donanteId, "Insignia 1", false);

    DonanteIncentivos guardado = repository.findById(donanteId).orElseThrow();
    assertFalse(guardado.getInsignias().getFirst().visible());
  }

  @Test
  void configurarVisibilidad_cuandoDonanteNoExiste_deberiaLanzarExcepcion() {
    UUID donanteId = UUID.randomUUID();
    assertThrows(
        BusinessStateException.class,
        () -> service.configurarVisibilidadInsignia(donanteId, "Insignia 1", false));
  }

  @Test
  void configurarVisibilidad_cuandoInsigniaNoExiste_deberiaLanzarExcepcion() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    repository.save(donante);

    assertThrows(
        BusinessStateException.class,
        () -> service.configurarVisibilidadInsignia(donanteId, "Insignia Inexistente", false));
  }

  @Test
  void configurarVisibilidad_cuandoDonanteNoExiste_deberiaLanzarDonanteNoEncontrado() {
    UUID donanteId = UUID.randomUUID();

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> service.configurarVisibilidadInsignia(donanteId, "Insignia 1", false));

    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void
      configurarVisibilidad_cuandoDonanteNoExisteYNombreEsBlank_deberiaPriorizarDonanteNoEncontrado() {
    UUID donanteId = UUID.randomUUID();

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> service.configurarVisibilidadInsignia(donanteId, "   ", false));

    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void configurarVisibilidad_cuandoNombreEsBlank_deberiaLanzarInsigniaSinNombre() {
    UUID donanteId = UUID.randomUUID();
    repository.save(DonanteIncentivosMother.colaboradorSinMisiones(donanteId));

    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> service.configurarVisibilidadInsignia(donanteId, "   ", false));

    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, ex.getError());
  }

  @Test
  void configurarVisibilidad_cuandoNombreEsNulo_deberiaLanzarInsigniaSinNombre() {
    UUID donanteId = UUID.randomUUID();
    repository.save(DonanteIncentivosMother.colaboradorSinMisiones(donanteId));

    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> service.configurarVisibilidadInsignia(donanteId, null, false));

    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, ex.getError());
  }

  @Test
  void configurarVisibilidad_cuandoNombreEsBlankYLaInsigniaExiste_noDeberiaModificarNada() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    donante.otorgarInsignia(new Insignia("Insignia 1", "Desc", "url1"));
    repository.save(donante);

    assertThrows(
        ValidationException.class,
        () -> service.configurarVisibilidadInsignia(donanteId, "", false));

    assertTrue(repository.findById(donanteId).orElseThrow().getInsignias().getFirst().visible());
  }

  @Test
  void configurarVisibilidad_cuandoInsigniaNoExiste_deberiaLanzarInsigniaNoEncontrada() {
    UUID donanteId = UUID.randomUUID();
    repository.save(DonanteIncentivosMother.colaboradorSinMisiones(donanteId));

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> service.configurarVisibilidadInsignia(donanteId, "Insignia Inexistente", false));

    assertEquals(ErrorCatalog.INSIGNIA_NO_ENCONTRADA, ex.getError());
  }

  @Test
  void configurarVisibilidad_cuandoInsigniaExiste_deberiaPoderVolverAHacerseVisible() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    donante.otorgarInsignia(new Insignia("Insignia 1", "Desc", "url1"));
    repository.save(donante);

    service.configurarVisibilidadInsignia(donanteId, "Insignia 1", false);
    service.configurarVisibilidadInsignia(donanteId, "Insignia 1", true);

    assertTrue(repository.findById(donanteId).orElseThrow().getInsignias().getFirst().visible());
  }
}
