package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemanticoBien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.impl.DonacionRepositoryEnMemoria;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcesadorDeDonacionesTest {

  private NormalizadorSemanticoBien normalizadorMock;
  private Segmentador segmentadorMock;
  private DonacionRepositoryEnMemoria donacionRepositoryMock;
  private IDonacionesIndependientesRepository donacionesIndependientesRepositoryMock;

  private ProcesadorDeDonaciones procesador;

  @BeforeEach
  void setUp() {
    normalizadorMock = mock(NormalizadorSemanticoBien.class);
    segmentadorMock = mock(Segmentador.class);
    donacionRepositoryMock = mock(DonacionRepositoryEnMemoria.class);
    donacionesIndependientesRepositoryMock = mock(IDonacionesIndependientesRepository.class);

    procesador =
        new ProcesadorDeDonaciones(
            normalizadorMock,
            segmentadorMock,
            donacionRepositoryMock,
            donacionesIndependientesRepositoryMock);
  }

  @Test
  void procesar_deberiaNormalizarSegmentarYPersistirDonacion() {
    Donante donanteMock = mock(Donante.class);
    Donacion donacion = new Donacion(donanteMock);

    List<ItemDonacionNormalizado> itemsNormalizados =
        Collections.singletonList(mock(ItemDonacionNormalizado.class));
    List<DonacionIndependiente> donacionesIndependientes =
        Collections.singletonList(mock(DonacionIndependiente.class));

    when(normalizadorMock.normalizar(donacion)).thenReturn(itemsNormalizados);
    when(segmentadorMock.segmentar(itemsNormalizados)).thenReturn(donacionesIndependientes);

    procesador.procesar(donacion);

    assertEquals(EstadoDonacion.SEGMENTADA, donacion.getEstadoActual());
    verify(normalizadorMock, times(1)).normalizar(donacion);
    verify(segmentadorMock, times(1)).segmentar(itemsNormalizados);

    // Debería guardarse tras normalizar y tras segmentar
    verify(donacionRepositoryMock, times(2)).save(donacion);
    verify(donacionesIndependientesRepositoryMock, times(1)).saveAll(donacionesIndependientes);
  }
}
