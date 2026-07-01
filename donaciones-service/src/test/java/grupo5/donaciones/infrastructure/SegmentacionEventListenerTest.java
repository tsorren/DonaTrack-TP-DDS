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
import grupo5.donaciones.models.repositories.*;
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
  @Mock private IDonacionesRepository donacionRepository;
  @Mock private Segmentador segmentador;
  @Mock private IDonacionesIndependientesRepository donacionesIndependientesRepository;
  @Mock private IncentivosFeignClient incentivosFeignClient;
  @Mock private ICategoriasRepository categoriasRepository;
  @Mock private ISubcategoriasRepository subcategoriasRepository;
  @Mock private grupo5.donaciones.models.repositories.IPersonasRepository personasRepository;
  @Mock private IDonantesRepository donantesRepository;

  @InjectMocks private SegmentacionEventListener listener;

  private Donacion donacion;
  private Donante donante;
  private Categoria categoria;
  private Subcategoria subcategoria;
  private ItemDonacionNormalizado itemAceptado;
  private ItemDonacionNormalizado itemRechazado;
  private Humana humana;

  @BeforeEach
  void setUp() {
    humana = new Humana("Juan", "Perez", java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    donante = new Donante(humana.getId());
    donacion = new Donacion(donante.getId());
    donacion.marcarNormalizada();

    categoria =
        new Categoria(
            "Ropa", false, false, grupo5.donaciones.models.entities.categorias.Unidad.UNIDADES);
    subcategoria = new Subcategoria(categoria.getId(), "Abrigos");
    Bien bien1 = new Bien("Abrigo", null, null, null, 1.0, 1.0);
    BienNormalizado bnAceptado =
        new BienNormalizado(
            bien1, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, false, false);
    itemAceptado = new ItemDonacionNormalizado(donacion.getId(), bnAceptado, 5);

    Bien bien2 = new Bien("Basura", null, null, null, 1.0, 1.0);
    BienNormalizado bnRechazado =
        new BienNormalizado(
            bien2, subcategoria.getId(), 0.0, EstadoNormalizacion.RECHAZADO, false, false);
    itemRechazado = new ItemDonacionNormalizado(donacion.getId(), bnRechazado, 1);
  }

  @Test
  void onDonacionNormalizada_deberiaSegmentarSoloItemsAceptadosYProgresarDonacion() {
    when(donacionRepository.findById(donacion.getId())).thenReturn(Optional.of(donacion));
    when(itemNormalizadoRepository.findAll()).thenReturn(List.of(itemAceptado, itemRechazado));

    grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente itemIndep =
        new grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente(
            itemAceptado.getBien(), 5);
    DonacionIndependiente donacionIndependiente =
        new DonacionIndependiente(donacion.getId(), List.of(itemIndep));

    when(segmentador.segmentar(List.of(itemAceptado))).thenReturn(List.of(donacionIndependiente));
    when(subcategoriasRepository.findById(subcategoria.getId()))
        .thenReturn(Optional.of(subcategoria));
    when(categoriasRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
    when(donantesRepository.findById(donante.getId())).thenReturn(Optional.of(donante));
    when(personasRepository.findById(humana.getId())).thenReturn(Optional.of(humana));

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
