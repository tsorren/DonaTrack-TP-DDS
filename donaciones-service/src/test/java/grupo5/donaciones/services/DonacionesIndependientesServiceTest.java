package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.*;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionesIndependientesServiceTest {

  private IDonacionesIndependientesRepository repositoryMock;
  private DonacionesIndependientesService service;

  private static final String ACTOR = "SISTEMA";
  private static final LocalDate TEST_DATE = LocalDate.of(2026, Month.JUNE, 9);

  @BeforeEach
  void setUp() {
    repositoryMock = mock(IDonacionesIndependientesRepository.class);
    service = new DonacionesIndependientesService(repositoryMock);
  }

  private DonacionIndependiente crearDonacionDePrueba() {
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
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(itemNormalizado.getBien(), 5);

    return new DonacionIndependiente(donacionOriginal, List.of(item));
  }

  @Test
  void cambiarEstado_DeberiaLanzarRecursoNoEncontradoException_CuandoNoExisteDonacion() {
    UUID id = UUID.randomUUID();
    when(repositoryMock.findById(id)).thenReturn(Optional.empty());

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, null);

    assertThrows(
        RecursoNoEncontradoException.class, () -> service.cambiarEstado(id, request, ACTOR));
    verify(repositoryMock, never()).save(any());
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAAsignacionRealizada_CuandoEstadoActualEsEnDeposito() {
    DonacionIndependiente donacion = crearDonacionDePrueba();
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(AsignacionRealizada.class, donacion.getEstadoActual());
    assertEquals(id, response.id());
    assertEquals("AsignacionRealizada", response.estadoActual());
    assertTrue(response.historialEstados().contains("AsignacionRealizada"));
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAVencida_CuandoEstadoActualEsEnDeposito() {
    DonacionIndependiente donacion = crearDonacionDePrueba();
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(TipoEstadoDonacion.VENCIDA, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(Vencida.class, donacion.getEstadoActual());
    assertEquals("Vencida", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void
      cambiarEstado_DeberiaTransicionarAListaParaEntregar_CuandoEstadoActualEsAsignacionRealizada() {
    DonacionIndependiente donacion = crearDonacionDePrueba();
    donacion.setEstadoActual(new AsignacionRealizada());
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(TipoEstadoDonacion.EN_TRASLADO, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(ListaParaEntregar.class, donacion.getEstadoActual());
    assertEquals("ListaParaEntregar", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAEnTraslado_CuandoEstadoActualEsListaParaEntregar() {
    DonacionIndependiente donacion = crearDonacionDePrueba();
    donacion.setEstadoActual(new ListaParaEntregar());
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.LISTA_PARA_ENTREGAR, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(EnTraslado.class, donacion.getEstadoActual());
    assertEquals("EnTraslado", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAEntregada_CuandoEstadoActualEsEnTraslado() {
    DonacionIndependiente donacion = crearDonacionDePrueba();
    donacion.setEstadoActual(new EnTraslado());
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(TipoEstadoDonacion.ENTREGADA, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(Entregada.class, donacion.getEstadoActual());
    assertEquals("Entregada", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void
      cambiarEstado_DeberiaTransicionarAEntregaFallida_CuandoEstadoActualEsEnTrasladoYJustificacionEsValida() {
    DonacionIndependiente donacion = crearDonacionDePrueba();
    donacion.setEstadoActual(new EnTraslado());
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA, "Dirección incorrecta", null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(EntregaFallida.class, donacion.getEstadoActual());
    assertEquals("EntregaFallida", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void
      cambiarEstado_DeberiaLanzarIllegalArgumentException_CuandoEstadoActualEsEnTrasladoYJustificacionEsInvalida() {
    DonacionIndependiente donacion = crearDonacionDePrueba();
    donacion.setEstadoActual(new EnTraslado());
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA, "", null);

    assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(id, request, ACTOR));
    verify(repositoryMock, never()).save(any());
  }

  @Test
  void cambiarEstado_DeberiaTransicionarAEnDeposito_CuandoEstadoActualEsEntregaFallida() {
    DonacionIndependiente donacion = crearDonacionDePrueba();
    donacion.setEstadoActual(new EntregaFallida());
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(TipoEstadoDonacion.EN_DEPOSITO, null, null);

    DonacionIndependienteResponseDTO response = service.cambiarEstado(id, request, ACTOR);

    assertInstanceOf(EnDeposito.class, donacion.getEstadoActual());
    assertEquals("EnDeposito", response.estadoActual());
    verify(repositoryMock, times(1)).save(donacion);
  }

  @Test
  void cambiarEstado_DeberiaLanzarBusinessStateException_CuandoTransicionEsInvalida() {
    DonacionIndependiente donacion = crearDonacionDePrueba();
    UUID id = donacion.getId();
    when(repositoryMock.findById(id)).thenReturn(Optional.of(donacion));

    // EnDeposito a ENTREGADA es una transición inválida
    CambioEstadoDonacionIndependienteRequestDTO request =
        new CambioEstadoDonacionIndependienteRequestDTO(TipoEstadoDonacion.ENTREGADA, null, null);

    assertThrows(BusinessStateException.class, () -> service.cambiarEstado(id, request, ACTOR));
    verify(repositoryMock, never()).save(any());
  }
}
