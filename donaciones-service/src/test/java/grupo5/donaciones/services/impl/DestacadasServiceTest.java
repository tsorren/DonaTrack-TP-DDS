package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donaciones.PeriodoDestacadas;
import grupo5.donaciones.dto.donaciones.outputs.DonacionDestacadaDTO;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.SolicitudCambioEstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.impl.DonacionesIndependientesRepositoryEnMemoria;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HB-01: entregadas en el período, con foto, sin el donante (D28). */
class DestacadasServiceTest {

  private static final String ACTOR = "logistica-service";
  private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 8, 12, 0);
  private static final List<String> FOTOS = List.of("https://ejemplo.org/a.jpg");

  private final DonacionesIndependientesRepositoryEnMemoria repositorio =
      new DonacionesIndependientesRepositoryEnMemoria();
  private final ISubcategoriasRepository subcategorias = mock(ISubcategoriasRepository.class);
  private final ICategoriasRepository categorias = mock(ICategoriasRepository.class);
  private final IPersonasRepository personas = mock(IPersonasRepository.class);
  private final UUID subcategoriaId = UUID.randomUUID();
  private final UUID entidadId = UUID.randomUUID();

  private DestacadasService service;

  @BeforeEach
  void preparar() {
    service =
        new DestacadasService(
            repositorio,
            subcategorias,
            categorias,
            personas,
            Clock.fixed(Instant.from(AHORA.atOffset(ZoneOffset.UTC)), ZoneOffset.UTC));
  }

  private DonacionIndependiente entregada(LocalDateTime fecha, List<String> fotos) {
    DonacionIndependiente donacion =
        new DonacionIndependiente(
            UUID.randomUUID(),
            List.of(
                new ItemDonacionIndependiente(
                    UUID.randomUUID(),
                    BienMother.aceptado(BienMother.prendaRopa("Campera"), subcategoriaId),
                    6)));
    donacion.asignar(ACTOR, new DestinoAsignacion(UUID.randomUUID(), null, entidadId));
    donacion.planificarRuta(ACTOR);
    donacion.iniciarRecorrido(ACTOR);
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.ENTREGADA, null, null, null, "AB123CD", null, ACTOR));
    donacion.registrarFotosDeRecepcion(fecha, fotos);
    return repositorio.save(donacion);
  }

  @Test
  void listar_porDefectoElMes_masNuevasPrimeroYConLosNombresPublicos() {
    Subcategoria camperas = mock(Subcategoria.class);
    UUID categoriaId = UUID.randomUUID();
    when(camperas.getNombre()).thenReturn("Camperas");
    when(camperas.getCategoriaId()).thenReturn(categoriaId);
    when(subcategorias.findById(subcategoriaId)).thenReturn(Optional.of(camperas));
    Categoria ropa = mock(Categoria.class);
    when(ropa.getNombre()).thenReturn("Ropa");
    when(categorias.findById(categoriaId)).thenReturn(Optional.of(ropa));
    Juridica comedor = mock(Juridica.class);
    when(comedor.getRazonSocial()).thenReturn("Comedor Los Pibes");
    when(personas.findById(entidadId)).thenReturn(Optional.of(comedor));
    DonacionIndependiente vieja = entregada(AHORA.minusDays(20), FOTOS);
    DonacionIndependiente nueva = entregada(AHORA.minusDays(2), FOTOS);
    entregada(AHORA.minusDays(31), FOTOS);

    Pagina<DonacionDestacadaDTO> pagina = service.listar(null, null);

    assertEquals(2, pagina.total());
    assertEquals(List.of(nueva.getId(), vieja.getId()), ids(pagina));
    DonacionDestacadaDTO primera = pagina.elementos().getFirst();
    assertEquals("Ropa", primera.categoria());
    assertEquals("Camperas", primera.subcategoria());
    assertEquals(6, primera.cantidad());
    assertEquals(entidadId, primera.entidadId());
    assertEquals("Comedor Los Pibes", primera.entidad());
    assertEquals(AHORA.minusDays(2), primera.fechaEntrega());
    assertEquals(FOTOS, primera.fotos());
  }

  @Test
  void listar_semanaYAnio_cambianLaVentana() {
    entregada(AHORA.minusDays(3), FOTOS);
    entregada(AHORA.minusDays(100), FOTOS);

    assertEquals(1, service.listar(PeriodoDestacadas.SEMANA, null).total());
    assertEquals(2, service.listar(PeriodoDestacadas.ANIO, null).total());
  }

  @Test
  void listar_sinFotosNoEsDestacada() {
    DonacionIndependiente sinFotos = entregada(AHORA.minusDays(1), List.of());

    assertNull(sinFotos.getFechaEntrega());
    assertTrue(service.listar(null, null).elementos().isEmpty());
  }

  @Test
  void listar_elLimiteCortaLaListaPeroNoElTotal() {
    entregada(AHORA.minusDays(1), FOTOS);
    entregada(AHORA.minusDays(2), FOTOS);
    entregada(AHORA.minusDays(3), FOTOS);

    Pagina<DonacionDestacadaDTO> pagina = service.listar(PeriodoDestacadas.MES, 2);

    assertEquals(2, pagina.elementos().size());
    assertEquals(3, pagina.total());
  }

  @Test
  void listar_limiteFueraDeRango_esInvalido() {
    assertThrows(ValidationException.class, () -> service.listar(null, 0));
    assertThrows(ValidationException.class, () -> service.listar(null, 51));
  }

  @Test
  void registrarFotos_sinFecha_esInvalido() {
    DonacionIndependiente donacion = entregada(AHORA.minusDays(1), List.of());

    assertThrows(ValidationException.class, () -> donacion.registrarFotosDeRecepcion(null, FOTOS));
  }

  private static List<UUID> ids(Pagina<DonacionDestacadaDTO> pagina) {
    return pagina.elementos().stream().map(DonacionDestacadaDTO::id).toList();
  }
}
