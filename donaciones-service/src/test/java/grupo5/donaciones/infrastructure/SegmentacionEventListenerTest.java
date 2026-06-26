package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.comunicaciones.NuevaDonacionRequest;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.events.DonacionNormalizadaEvent;
import grupo5.donaciones.infrastructure.events.SegmentacionEventListener;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.impl.DonacionRepositoryEnMemoria;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentacionEventListenerTest {

  @Mock private IItemDonacionNormalizadoRepository itemNormalizadoRepository;
  @Mock private DonacionRepositoryEnMemoria donacionRepository;
  @Mock private Segmentador segmentador;
  @Mock private IDonacionesIndependientesRepository donacionesIndependientesRepository;
  @Mock private IncentivosFeignClient incentivosFeignClient;

  @InjectMocks private SegmentacionEventListener listener;

  private Donacion donacion;
  private ItemDonacionNormalizado itemAceptado;
  private ItemDonacionNormalizado itemRechazado;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("Juan", "Perez", java.time.LocalDate.of(1990, 1, 1));
    Donante donante = new Donante(humana);
    donacion = new Donacion(donante);
    donacion.setFecha(java.time.LocalDateTime.now());
    donacion.marcarNormalizada();

    Categoria categoria =
        new Categoria(
            "Ropa", false, false, grupo5.donaciones.models.entities.categorias.Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria, "Abrigos");
    Bien bien1 = new Bien("Abrigo", null, null, null);
    BienNormalizado bnAceptado =
        new BienNormalizado(bien1, subcategoria, 1.0, EstadoNormalizacion.ACEPTADO);
    itemAceptado = new ItemDonacionNormalizado(donacion, bnAceptado, 5);

    Bien bien2 = new Bien("Basura", null, null, null);
    BienNormalizado bnRechazado =
        new BienNormalizado(bien2, subcategoria, 0.0, EstadoNormalizacion.RECHAZADO);
    itemRechazado = new ItemDonacionNormalizado(donacion, bnRechazado, 1);
  }

  @Test
  void onDonacionNormalizada_deberiaSegmentarSoloItemsAceptadosYProgresarDonacion() {
    when(donacionRepository.findById(donacion.getId())).thenReturn(Optional.of(donacion));
    when(itemNormalizadoRepository.findAll()).thenReturn(List.of(itemAceptado, itemRechazado));

    grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente itemIndep =
        new grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente(
            itemAceptado.getBien(), 5);
    DonacionIndependiente donacionIndependiente =
        new DonacionIndependiente(donacion, List.of(itemIndep));

    when(segmentador.segmentar(List.of(itemAceptado))).thenReturn(List.of(donacionIndependiente));

    listener.onDonacionNormalizada(new DonacionNormalizadaEvent(donacion.getId()));

    assertTrue(itemAceptado.isSegmentado());
    assertFalse(itemRechazado.isSegmentado());
    assertEquals(EstadoDonacion.SEGMENTADA, donacion.getEstadoActual());

    verify(segmentador, times(1)).segmentar(List.of(itemAceptado));
    verify(incentivosFeignClient, times(1)).procesarDonacion(any(NuevaDonacionRequest.class));
    verify(donacionesIndependientesRepository, times(1)).saveAll(List.of(donacionIndependiente));
    verify(itemNormalizadoRepository, times(1)).save(itemAceptado);
    verify(itemNormalizadoRepository, never()).save(itemRechazado);
    verify(donacionRepository, times(1)).save(donacion);
  }

  @Test
  void onDonacionNormalizada_cuandoNoHayAceptados_deberiaMarcarComoSegmentadaSinSegmentar() {
    when(donacionRepository.findById(donacion.getId())).thenReturn(Optional.of(donacion));
    when(itemNormalizadoRepository.findAll()).thenReturn(List.of(itemRechazado));

    listener.onDonacionNormalizada(new DonacionNormalizadaEvent(donacion.getId()));

    assertEquals(EstadoDonacion.SEGMENTADA, donacion.getEstadoActual());

    verify(segmentador, never()).segmentar(any());
    verify(incentivosFeignClient, never()).procesarDonacion(any());
    verify(donacionesIndependientesRepository, never()).saveAll(any());
    verify(donacionRepository, times(1)).save(donacion);
  }
}
