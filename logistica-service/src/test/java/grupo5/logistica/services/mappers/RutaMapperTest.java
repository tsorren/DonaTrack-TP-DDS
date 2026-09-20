package grupo5.logistica.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.logistica.infrastructure.GeneradorDeURLSeguimiento;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.testutils.EntregaMother;
import grupo5.logistica.testutils.RutaMother;
import java.util.List;
import org.junit.jupiter.api.Test;

class RutaMapperTest {
  private final RutaMapper mapper =
      new RutaMapper(
          new EntregaMapper(new DireccionMapper()),
          new GeneradorDeURLSeguimiento("http://tracking/"));

  @Test
  void rutaPendienteNoExponeUrlSeguimiento() {
    Ruta ruta = RutaMother.pendiente();
    var dto = mapper.toResponseDTO(ruta);
    assertEquals(ruta.getId(), dto.id());
    assertEquals(ruta.getEntregaIds(), dto.entregaIds());
    assertNull(dto.urlSeguimiento());
  }

  @Test
  void rutaIniciadaExponeUrlYPermiteIncluirEntregas() {
    Entrega entrega = EntregaMother.pendiente();
    Ruta ruta = RutaMother.pendienteCon(entrega);
    ruta.iniciarRuta();
    var dto = mapper.toResponseDTOConEntregas(ruta, List.of(entrega));
    assertEquals("http://tracking/" + ruta.getId(), dto.urlSeguimiento());
    assertEquals(entrega.getId(), dto.entregas().getFirst().id());
  }

  @Test
  void aceptaRutaNula() {
    assertNull(mapper.toResponseDTO(null));
    assertNull(mapper.toResponseDTOConEntregas(null, List.of()));
  }
}
