package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
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
  void obtenerInsignias_deberiaRetornarListaVaciaSinInsignias() {
    UUID id = new UUID(0L, 51L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    repository.save(donante);

    List<InsigniaDTO> insignias = service.obtenerInsignias(id);

    assertNotNull(insignias);
    assertTrue(insignias.isEmpty());
  }

  @Test
  void configurarVisibilidadInsignia_deberiaOcultarInsignia() {
    UUID id = new UUID(0L, 30L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    donante.otorgarInsignia(new Insignia("Explorador", "desc", "/img.png"));
    repository.save(donante);

    service.configurarVisibilidadInsignia(id, "Explorador", false);

    InsigniaGanada insignia =
        repository.findById(id).orElseThrow().getInsignias().stream()
            .filter(i -> i.nombre().equals("Explorador"))
            .findFirst()
            .orElseThrow();
    assertFalse(insignia.visible());
  }

  @Test
  void configurarVisibilidadInsignia_deberiaLanzarExcepcionSiInsigniaNoExiste() {
    UUID id = new UUID(0L, 31L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    repository.save(donante);

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> service.configurarVisibilidadInsignia(id, "NoExiste", false));
    assertEquals(ErrorCatalog.INSIGNIA_NO_ENCONTRADA, ex.getError());
  }
}
