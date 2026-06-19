package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.comunicaciones.NuevaDonacionRequest;
import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemanticoBien;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
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
  private IncentivosFeignClient incentivosFeignClient;

  private ProcesadorDeDonaciones procesador;

  @BeforeEach
  void setUp() {
    normalizadorMock = mock(NormalizadorSemanticoBien.class);
    segmentadorMock = mock(Segmentador.class);
    donacionRepositoryMock = mock(DonacionRepositoryEnMemoria.class);
    donacionesIndependientesRepositoryMock = mock(IDonacionesIndependientesRepository.class);
    incentivosFeignClient = mock(IncentivosFeignClient.class);

    procesador =
        new ProcesadorDeDonaciones(
            normalizadorMock,
            segmentadorMock,
            donacionRepositoryMock,
            donacionesIndependientesRepositoryMock,
            incentivosFeignClient);
  }

  @Test
  void procesar_deberiaNormalizarSegmentarYPersistirDonacion() {
    Humana humana =
        new Humana("Juan", "Perez", java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    Donante donante = new Donante(humana);
    Donacion donacion = new Donacion(donante);
    donacion.setFecha(java.time.LocalDateTime.now());

    grupo5.donaciones.models.entities.categorias.Categoria categoria =
        new grupo5.donaciones.models.entities.categorias.Categoria(
            "Ropa", false, false, grupo5.donaciones.models.entities.categorias.Unidad.UNIDADES);
    grupo5.donaciones.models.entities.categorias.Subcategoria subcategoria =
        new grupo5.donaciones.models.entities.categorias.Subcategoria(categoria, "Abrigos");
    grupo5.donaciones.models.entities.donaciones.Bien bien =
        new grupo5.donaciones.models.entities.donaciones.Bien("Abrigo", null, null, null);
    grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado bienNormalizado =
        new grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado(
            bien,
            subcategoria,
            1.0,
            grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion.ACEPTADO);

    grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente item =
        new grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente(
            bienNormalizado, 5);

    DonacionIndependiente donacionIndependiente =
        new DonacionIndependiente(donacion, List.of(item));

    List<ItemDonacionNormalizado> itemsNormalizados =
        Collections.singletonList(mock(ItemDonacionNormalizado.class));
    List<DonacionIndependiente> donacionesIndependientes =
        Collections.singletonList(donacionIndependiente);

    when(normalizadorMock.normalizar(donacion)).thenReturn(itemsNormalizados);
    when(segmentadorMock.segmentar(itemsNormalizados)).thenReturn(donacionesIndependientes);

    procesador.procesar(donacion);

    assertEquals(EstadoDonacion.SEGMENTADA, donacion.getEstadoActual());
    verify(normalizadorMock, times(1)).normalizar(donacion);
    verify(segmentadorMock, times(1)).segmentar(itemsNormalizados);

    // Debería guardarse tras normalizar y tras segmentar
    verify(donacionRepositoryMock, times(2)).save(donacion);
    verify(donacionesIndependientesRepositoryMock, times(1)).saveAll(donacionesIndependientes);
    verify(incentivosFeignClient, times(1)).procesarDonacion(any(NuevaDonacionRequest.class));
  }
}
