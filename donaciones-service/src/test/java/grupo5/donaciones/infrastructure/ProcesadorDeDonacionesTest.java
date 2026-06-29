package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemanticoBien;
import grupo5.donaciones.infrastructure.events.DonacionNormalizadaEvent;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.impl.DonacionRepositoryEnMemoria;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ProcesadorDeDonacionesTest {

  private NormalizadorSemanticoBien normalizadorMock;
  private DonacionRepositoryEnMemoria donacionRepositoryMock;
  private IItemDonacionNormalizadoRepository itemNormalizadoRepositoryMock;
  private ISubcategoriasRepository subcategoriasRepositoryMock;
  private ApplicationEventPublisher eventPublisherMock;

  private ProcesadorDeDonaciones procesador;

  @BeforeEach
  void setUp() {
    normalizadorMock = mock(NormalizadorSemanticoBien.class);
    donacionRepositoryMock = mock(DonacionRepositoryEnMemoria.class);
    itemNormalizadoRepositoryMock = mock(IItemDonacionNormalizadoRepository.class);
    subcategoriasRepositoryMock = mock(ISubcategoriasRepository.class);
    eventPublisherMock = mock(ApplicationEventPublisher.class);

    procesador =
        new ProcesadorDeDonaciones(
            normalizadorMock,
            donacionRepositoryMock,
            itemNormalizadoRepositoryMock,
            subcategoriasRepositoryMock,
            eventPublisherMock);
  }

  @Test
  void procesar_conTodosAceptados_deberiaNormalizarYPublicarEvento() {
    Humana humana =
        new Humana("Juan", "Perez", java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    Donante donante = new Donante(humana.getId());
    Donacion donacion = new Donacion(donante.getId());

    Categoria categoria = new Categoria("Ropa", false, false, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Abrigos");
    Bien bien = new Bien("Abrigo", null, null, null);
    BienNormalizado bienNormalizado =
        new BienNormalizado(
            bien, subcategoria.getId(), 1.0, EstadoNormalizacion.ACEPTADO, false, false);

    ItemDonacionNormalizado itemNormalizado =
        new ItemDonacionNormalizado(donacion.getId(), bienNormalizado, 5);
    List<ItemDonacionNormalizado> itemsNormalizados = Collections.singletonList(itemNormalizado);

    when(normalizadorMock.normalizar(donacion)).thenReturn(itemsNormalizados);

    procesador.procesar(donacion);

    assertEquals(EstadoDonacion.NORMALIZADA, donacion.getEstadoActual());
    verify(normalizadorMock, times(1)).normalizar(donacion);
    verify(itemNormalizadoRepositoryMock, times(1)).saveAll(itemsNormalizados);
    verify(donacionRepositoryMock, times(1)).save(donacion);
    verify(eventPublisherMock, times(1)).publishEvent(any(DonacionNormalizadaEvent.class));
  }

  @Test
  void procesar_conPendientesDeRevision_deberiaGuardarNormalizacionesYQuedarEnCargada() {
    Humana humana =
        new Humana("Juan", "Perez", java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    Donante donante = new Donante(humana.getId());
    Donacion donacion = new Donacion(donante.getId());

    Categoria categoria = new Categoria("Ropa", false, false, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Abrigos");
    Bien bien = new Bien("Abrigo", null, null, null);
    BienNormalizado bienNormalizado =
        new BienNormalizado(
            bien,
            subcategoria.getId(),
            0.4, // Confianza baja
            EstadoNormalizacion.PENDIENTE_REVISION,
            false,
            false);

    ItemDonacionNormalizado itemNormalizado =
        new ItemDonacionNormalizado(donacion.getId(), bienNormalizado, 5);
    List<ItemDonacionNormalizado> itemsNormalizados = Collections.singletonList(itemNormalizado);

    when(normalizadorMock.normalizar(donacion)).thenReturn(itemsNormalizados);

    procesador.procesar(donacion);

    assertEquals(EstadoDonacion.CARGADA, donacion.getEstadoActual());
    verify(normalizadorMock, times(1)).normalizar(donacion);
    verify(itemNormalizadoRepositoryMock, times(1)).saveAll(itemsNormalizados);
    verify(donacionRepositoryMock, never()).save(donacion);
    verify(eventPublisherMock, never()).publishEvent(any(DonacionNormalizadaEvent.class));
  }
}
