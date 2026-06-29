package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import grupo5.donaciones.infrastructure.events.DonacionNormalizadaEvent;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.impl.ItemDonacionNormalizadoService;
import grupo5.donaciones.services.mappers.ItemDonacionNormalizadoMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ItemDonacionNormalizadoServiceTest {

  @Mock private IItemDonacionNormalizadoRepository itemNormalizadoRepository;
  @Mock private IDonacionesRepository donacionRepository;
  @Mock private ISubcategoriasRepository subcategoriasRepository;
  @Mock private ICategoriasRepository categoriasRepository;
  @Mock private ItemDonacionNormalizadoMapper mapper;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ItemDonacionNormalizadoService service;

  private Donacion donacion;
  private Subcategoria subcategoria;
  private ItemDonacionNormalizado itemNormalizado;

  @BeforeEach
  void setUp() {
    Humana humana =
        new Humana("Pedro", "Gomez", java.time.LocalDate.of(1985, java.time.Month.MAY, 15));
    Donante donante = new Donante(humana.getId());
    donacion = new Donacion(donante.getId());

    Categoria categoria =
        new Categoria(
            "Alimentos",
            false,
            false,
            grupo5.donaciones.models.entities.categorias.Unidad.KILOGRAMO);
    subcategoria = new Subcategoria(categoria.getId(), "Arroz");

    Bien bien = new Bien("Paquete de arroz", null, null, null);
    BienNormalizado bienNormalizado =
        new BienNormalizado(
            bien, subcategoria.getId(), 0.4, EstadoNormalizacion.PENDIENTE_REVISION, false, false);
    itemNormalizado = new ItemDonacionNormalizado(donacion.getId(), bienNormalizado, 10);
  }

  @Test
  void obtenerPendientes_deberiaRetornarSoloItemsPendientes() {
    when(itemNormalizadoRepository.findAll()).thenReturn(List.of(itemNormalizado));
    ItemDonacionNormalizadoOutputDTO outputDTO =
        new ItemDonacionNormalizadoOutputDTO(
            itemNormalizado.getId(),
            donacion.getId(),
            "Paquete de arroz",
            10,
            null,
            0.4,
            EstadoNormalizacion.PENDIENTE_REVISION,
            false);
    when(mapper.toOutputDTO(itemNormalizado)).thenReturn(outputDTO);

    List<ItemDonacionNormalizadoOutputDTO> result = service.obtenerPendientes();

    assertEquals(1, result.size());
    assertEquals(EstadoNormalizacion.PENDIENTE_REVISION, result.getFirst().estadoNormalizacion());
  }

  @Test
  void actualizarEstado_cuandoItemNoExiste_deberiaLanzarExcepcion() {
    UUID randomId = UUID.randomUUID();
    when(itemNormalizadoRepository.findById(randomId)).thenReturn(Optional.empty());

    ItemDonacionNormalizadoPatchDTO patchDTO =
        new ItemDonacionNormalizadoPatchDTO(EstadoNormalizacion.ACEPTADO, null);

    assertThrows(
        RecursoNoEncontradoException.class, () -> service.actualizarEstado(randomId, patchDTO));
  }

  @Test
  void
      actualizarEstado_cuandoAceptaYEsElUltimoPendiente_deberiaNormalizarDonacionYPublicarEvento() {
    when(itemNormalizadoRepository.findById(itemNormalizado.getId()))
        .thenReturn(Optional.of(itemNormalizado));
    when(subcategoriasRepository.findById(subcategoria.getId()))
        .thenReturn(Optional.of(subcategoria));
    when(donacionRepository.findById(donacion.getId())).thenReturn(Optional.of(donacion));
    when(itemNormalizadoRepository.findAll()).thenReturn(List.of(itemNormalizado));

    ItemDonacionNormalizadoPatchDTO patchDTO =
        new ItemDonacionNormalizadoPatchDTO(EstadoNormalizacion.ACEPTADO, null);

    service.actualizarEstado(itemNormalizado.getId(), patchDTO);

    assertEquals(EstadoNormalizacion.ACEPTADO, itemNormalizado.getBien().estadoNormalizacion());
    assertEquals(EstadoDonacion.NORMALIZADA, donacion.getEstadoActual());
    verify(itemNormalizadoRepository, times(1)).save(itemNormalizado);
    verify(donacionRepository, times(1)).save(donacion);
    verify(eventPublisher, times(1)).publishEvent(any(DonacionNormalizadaEvent.class));
  }

  @Test
  void
      actualizarEstado_cuandoReclasificaManualmenteAlRechazar_deberiaGuardarConNuevaSubcategoriaYAceptado() {
    UUID newSubId = UUID.randomUUID();
    Subcategoria newSub = mock(Subcategoria.class);
    when(newSub.getNombre()).thenReturn("Fideos");
    when(newSub.getId()).thenReturn(newSubId);

    when(itemNormalizadoRepository.findById(itemNormalizado.getId()))
        .thenReturn(Optional.of(itemNormalizado));
    when(subcategoriasRepository.findById(newSubId)).thenReturn(Optional.of(newSub));
    when(donacionRepository.findById(donacion.getId())).thenReturn(Optional.of(donacion));
    when(itemNormalizadoRepository.findAll()).thenReturn(List.of(itemNormalizado));

    ItemDonacionNormalizadoPatchDTO patchDTO =
        new ItemDonacionNormalizadoPatchDTO(EstadoNormalizacion.RECHAZADO, newSubId);

    service.actualizarEstado(itemNormalizado.getId(), patchDTO);

    assertEquals(
        EstadoNormalizacion.ACEPTADO,
        itemNormalizado.getBien().estadoNormalizacion()); // Aceptado porque fue manual
    assertEquals(newSubId, itemNormalizado.getBien().subcategoriaId());
    assertEquals(1.0, itemNormalizado.getBien().confianza());
    assertEquals(EstadoDonacion.NORMALIZADA, donacion.getEstadoActual());
    verify(eventPublisher, times(1)).publishEvent(any(DonacionNormalizadaEvent.class));
  }
}
