package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.infrastructure.analizadores.NormalizadorSemanticoBien;
import grupo5.donaciones.infrastructure.events.DonacionNormalizadaEvent;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
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
  private ApplicationEventPublisher eventPublisherMock;

  private ProcesadorDeDonaciones procesador;

  @BeforeEach
  void setUp() {
    normalizadorMock = mock(NormalizadorSemanticoBien.class);
    donacionRepositoryMock = mock(DonacionRepositoryEnMemoria.class);
    itemNormalizadoRepositoryMock = mock(IItemDonacionNormalizadoRepository.class);
    eventPublisherMock = mock(ApplicationEventPublisher.class);

    procesador =
        new ProcesadorDeDonaciones(
            normalizadorMock,
            donacionRepositoryMock,
            itemNormalizadoRepositoryMock,
            eventPublisherMock);
  }

  @Test
  void procesar_conTodosAceptados_deberiaNormalizarYPublicarEvento() {
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
            bien, subcategoria, 1.0, EstadoNormalizacion.ACEPTADO);

    ItemDonacionNormalizado itemNormalizado =
        new ItemDonacionNormalizado(donacion, bienNormalizado, 5);
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
            0.4, // Confianza baja
            EstadoNormalizacion.PENDIENTE_REVISION);

    ItemDonacionNormalizado itemNormalizado =
        new ItemDonacionNormalizado(donacion, bienNormalizado, 5);
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
