package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.errors.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonacionesIndependientesServiceTest {

  private static final Long ID = 1L;
  private static final String ACTOR = "admin";
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  @Mock private IDonacionesIndependientesRepository repositorio;

  @InjectMocks private DonacionesIndependientesService service;

  private DonacionIndependiente donacion;

  @BeforeEach
  void setUp() {
    Humana humana = new Humana("Juan", "Pérez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana);
    Donacion donacionOriginal = new Donacion(donante);

    Categoria categoria = new Categoria("Ropa", false, true, Unidad.UNIDADES);
    Subcategoria subcategoria = new Subcategoria(categoria, "Ropa de Invierno");
    Bien bien = new Bien("Abrigo", "abrigo.png", TEST_DATE.plusMonths(6), Estado.NUEVO);
    BienNormalizado bienNormalizado =
        new BienNormalizado(bien, subcategoria, 1.0, EstadoNormalizacion.ACEPTADO);

    ItemDonacionNormalizado itemNormalizado =
        new ItemDonacionNormalizado(donacionOriginal, bienNormalizado, 5);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(itemNormalizado, 5);

    donacion = new DonacionIndependiente(donacionOriginal, List.of(item));
  }

  @Test
  void cambiarEstado_conEstadoAsignada_llamamosAsignarYGuardamos() {
    when(repositorio.findById(ID)).thenReturn(Optional.of(donacion));
    when(repositorio.save(eq(ID), any())).thenReturn(donacion);

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO("ASIGNADA", null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(ID, request, ACTOR);

    assertNotNull(response);
    verify(repositorio).save(eq(ID), eq(donacion));
  }

  @Test
  void cambiarEstado_conIdInexistente_lanzaRecursoNoEncontradoException() {
    when(repositorio.findById(ID)).thenReturn(Optional.empty());

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO("ASIGNADA", null, null);

    assertThrows(
        RecursoNoEncontradoException.class, () -> service.cambiarEstado(ID, request, ACTOR));
  }

  @Test
  void cambiarEstado_conEstadoInvalido_lanzaIllegalArgumentException() {
    when(repositorio.findById(ID)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO("ESTADO_INEXISTENTE", null, null);

    assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(ID, request, ACTOR));
  }

  @Test
  void cambiarEstado_conEstadoEntregaFallida_pasaJustificacionAlDominio() {
    when(repositorio.findById(ID)).thenReturn(Optional.of(donacion));
    when(repositorio.save(eq(ID), any())).thenReturn(donacion);

    donacion.asignar(ACTOR);
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO("ENTREGA_FALLIDA", "No había nadie", null);

    assertDoesNotThrow(() -> service.cambiarEstado(ID, request, ACTOR));
    verify(repositorio).save(eq(ID), eq(donacion));
  }

  @Test
  void cambiarEstado_responseDTO_contieneEstadoActual() {
    when(repositorio.findById(ID)).thenReturn(Optional.of(donacion));
    when(repositorio.save(eq(ID), any())).thenReturn(donacion);

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO("ASIGNADA", null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(ID, request, ACTOR);

    assertNotNull(response.estadoActual());
  }
}
