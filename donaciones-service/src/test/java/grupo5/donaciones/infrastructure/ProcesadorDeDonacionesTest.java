package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import grupo5.donaciones.models.entities.donaciones.events.DonacionNormalizada;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ProcesadorDeDonacionesTest {

  private IDonacionesRepository donacionRepositoryMock;
  private IItemDonacionNormalizadoRepository itemNormalizadoRepositoryMock;
  private ISubcategoriasRepository subcategoriasRepositoryMock;
  private ICategoriasRepository categoriasRepositoryMock;
  private ApplicationEventPublisher eventPublisherMock;

  private ProcesadorDeDonaciones procesador;

  @BeforeEach
  void setUp() {
    donacionRepositoryMock = mock(IDonacionesRepository.class);
    itemNormalizadoRepositoryMock = mock(IItemDonacionNormalizadoRepository.class);
    subcategoriasRepositoryMock = mock(ISubcategoriasRepository.class);
    categoriasRepositoryMock = mock(ICategoriasRepository.class);
    eventPublisherMock = mock(ApplicationEventPublisher.class);

    procesador =
        new ProcesadorDeDonaciones(
            donacionRepositoryMock,
            itemNormalizadoRepositoryMock,
            subcategoriasRepositoryMock,
            categoriasRepositoryMock,
            eventPublisherMock,
            0.6);
  }

  @Test
  void procesar_conTodosAceptados_deberiaNormalizarYPublicarEvento() {
    Humana humana = new Humana("Juan", "Perez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana.getId());
    Donacion donacion = new Donacion(donante.getId());

    Categoria categoria = new Categoria("Ropa", false, false, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Abrigos");
    subcategoria.agregarAlias("campera de abrigo");

    Bien bien = new Bien("campera de abrigo", null, null, null, 1.0, 1.0);
    donacion.agregarItem(new ItemDonacion(bien, 5));

    when(subcategoriasRepositoryMock.findAll()).thenReturn(List.of(subcategoria));
    when(categoriasRepositoryMock.findAll()).thenReturn(List.of(categoria));

    procesador.procesar(donacion);

    assertEquals(EstadoDonacion.NORMALIZADA, donacion.getEstadoActual());
    verify(itemNormalizadoRepositoryMock, times(1)).saveAll(any());
    verify(donacionRepositoryMock, times(1)).save(donacion);
    verify(eventPublisherMock, times(1)).publishEvent(any(DonacionNormalizada.class));
  }

  @Test
  void procesar_conPendientesDeRevision_deberiaGuardarNormalizacionesYQuedarEnCargada() {
    Humana humana = new Humana("Juan", "Perez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana.getId());
    Donacion donacion = new Donacion(donante.getId());

    Categoria categoria = new Categoria("Ropa", false, false, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria.getId(), "Abrigos");
    subcategoria.agregarAlias("campera impermeable muy abrigada");

    // Coincidencia parcial con confianza < 0.6
    Bien bien = new Bien("campera", null, null, null, 1.0, 1.0);
    donacion.agregarItem(new ItemDonacion(bien, 5));

    when(subcategoriasRepositoryMock.findAll()).thenReturn(List.of(subcategoria));
    when(categoriasRepositoryMock.findAll()).thenReturn(List.of(categoria));

    procesador.procesar(donacion);

    assertEquals(EstadoDonacion.CARGADA, donacion.getEstadoActual());
    verify(itemNormalizadoRepositoryMock, times(1)).saveAll(any());
    verify(donacionRepositoryMock, never()).save(donacion);
    verify(eventPublisherMock, never()).publishEvent(any(DonacionNormalizada.class));
  }
}
