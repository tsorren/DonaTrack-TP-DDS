package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.actividad.ActividadDTO;
import grupo5.donaciones.dto.actividad.AnotacionActividadDTO;
import grupo5.donaciones.dto.actividad.FiltroActividad;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.OrigenEntidad;
import grupo5.donaciones.dto.personas.HumanaOutputDTO;
import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.models.entities.actividad.Actividad;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.models.repositories.impl.ActividadRepositoryEnMemoria;
import grupo5.donaciones.services.impl.ActividadService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** R11-26: el registro de actividad (C1, C2, C4, C5 y C6 de la spec R11-p2-actividad). */
class ActividadServiceTest {

  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-12T21:20:00Z"), ARGENTINA);
  private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 12, 18, 20);

  private final ActividadRepositoryEnMemoria repositorio = new ActividadRepositoryEnMemoria();
  private final IPropuestasRepository propuestas = mock(IPropuestasRepository.class);
  private final INecesidadesRepository necesidades = mock(INecesidadesRepository.class);
  private final IPersonasRepository personas = mock(IPersonasRepository.class);
  private Optional<String> cuenta;
  private ActividadService servicio;

  @BeforeEach
  void armar() {
    cuenta = Optional.of("admin-a");
    servicio =
        new ActividadService(repositorio, () -> cuenta, RELOJ, propuestas, necesidades, personas);
  }

  private List<ActividadDTO> todas() {
    return servicio
        .listar(FiltroActividad.ninguno(), new ParametrosListado(0, 100, null, null))
        .elementos();
  }

  private ActividadDTO unica() {
    List<ActividadDTO> filas = todas();
    assertEquals(1, filas.size());
    return filas.get(0);
  }

  @Test
  void aprobarUnaPropuesta_quedaANombreDeQuienLaAprobo_conSuOrganizacion() {
    UUID propuestaId = UUID.randomUUID();
    UUID necesidadId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();
    Propuesta propuesta = mock(Propuesta.class);
    when(propuesta.getNecesidadQueSatisfaceId()).thenReturn(necesidadId);
    Necesidad necesidad = mock(Necesidad.class);
    when(necesidad.getEntidadId()).thenReturn(entidadId);
    Juridica juridica = mock(Juridica.class);
    when(juridica.getRazonSocial()).thenReturn("Comedor Barrio Ejemplo");
    when(propuestas.findById(propuestaId)).thenReturn(Optional.of(propuesta));
    when(necesidades.findById(necesidadId)).thenReturn(Optional.of(necesidad));
    when(personas.findById(entidadId)).thenReturn(Optional.of(juridica));

    servicio.propuestaDecidida(propuestaId, EstadoPropuesta.APROBADA);

    ActividadDTO fila = unica();
    assertEquals("admin-a", fila.autor());
    assertEquals(AccionDeAdministracion.PROPUESTA_APROBADA, fila.accion());
    assertEquals(propuestaId.toString(), fila.objetoId());
    assertEquals("Comedor Barrio Ejemplo", fila.detalle());
    assertEquals(AHORA, fila.ocurrioEn());
  }

  @Test
  void descartarUnaPropuestaQueYaNoSeEncuentra_quedaSinRotulo() {
    UUID propuestaId = UUID.randomUUID();
    when(propuestas.findById(propuestaId)).thenReturn(Optional.empty());

    servicio.propuestaDecidida(propuestaId, EstadoPropuesta.DESCARTADA);

    ActividadDTO fila = unica();
    assertEquals(AccionDeAdministracion.PROPUESTA_DESCARTADA, fila.accion());
    assertNull(fila.detalle());
  }

  @Test
  void sinTokenDeAdmin_ningunaAccionDejaFila() {
    cuenta = Optional.empty();

    servicio.registrar(AccionDeAdministracion.DONACION_REGISTRADA, "d-1", null);
    servicio.propuestaDecidida(UUID.randomUUID(), EstadoPropuesta.APROBADA);
    servicio.donanteRegistrado(new DonanteOutputDTO(UUID.randomUUID(), humana(), true));

    assertTrue(todas().isEmpty());
  }

  @Test
  void registrarUnDonanteHumano_noGuardaSuNombre() {
    UUID id = UUID.randomUUID();

    servicio.donanteRegistrado(new DonanteOutputDTO(id, humana(), true));

    ActividadDTO fila = unica();
    assertEquals(AccionDeAdministracion.DONANTE_REGISTRADO, fila.accion());
    assertEquals(id.toString(), fila.objetoId());
    assertNull(fila.detalle());
  }

  @Test
  void registrarUnDonanteJuridico_guardaSuRazonSocial() {
    servicio.donanteRegistrado(
        new DonanteOutputDTO(UUID.randomUUID(), juridica("Almacén Ejemplo SRL"), true));

    assertEquals("Almacén Ejemplo SRL", unica().detalle());
  }

  @Test
  void unaOrganizacionDeUnaSolicitud_esAprobada_ySinOrigen_esRegistrada() {
    UUID id = UUID.randomUUID();
    EntidadBeneficiariaOutputDTO entidad =
        new EntidadBeneficiariaOutputDTO(id, juridica("Comedor Ejemplo"), true);

    servicio.organizacionCreada(entidad, OrigenEntidad.SOLICITUD);
    servicio.organizacionCreada(entidad, OrigenEntidad.ADMIN);

    List<AccionDeAdministracion> acciones = todas().stream().map(ActividadDTO::accion).toList();
    assertTrue(acciones.contains(AccionDeAdministracion.ORGANIZACION_APROBADA));
    assertTrue(acciones.contains(AccionDeAdministracion.ORGANIZACION_REGISTRADA));
    assertTrue(todas().stream().allMatch(f -> "Comedor Ejemplo".equals(f.detalle())));
  }

  @Test
  void elDetalleSeRecortaA200() {
    servicio.registrar(AccionDeAdministracion.IMPORTACION_SUBIDA, "a-1", "x".repeat(300));

    assertEquals(Actividad.LARGO_DETALLE, unica().detalle().length());
  }

  @Test
  void sinParametros_traeLas20MasRecientesYElTotal() {
    IntStream.range(0, 25)
        .forEach(
            i ->
                repositorio.guardar(
                    new Actividad(
                        UUID.randomUUID(),
                        AHORA.minusMinutes(i),
                        "admin-a",
                        AccionDeAdministracion.DONACION_VENCIDA,
                        "d-" + i,
                        null)));

    Pagina<ActividadDTO> pagina =
        servicio.listar(FiltroActividad.ninguno(), ParametrosListado.primeraPagina());

    assertEquals(25, pagina.total());
    assertEquals(20, pagina.elementos().size());
    assertEquals("d-0", pagina.elementos().get(0).objetoId());
    assertEquals("d-19", pagina.elementos().get(19).objetoId());
  }

  @Test
  void unaPaginaFueraDeRango_vieneVaciaConElTotalReal() {
    servicio.registrar(AccionDeAdministracion.DONACION_VENCIDA, "d-1", null);

    Pagina<ActividadDTO> pagina =
        servicio.listar(FiltroActividad.ninguno(), new ParametrosListado(5, 20, null, null));

    assertTrue(pagina.elementos().isEmpty());
    assertEquals(1, pagina.total());
  }

  @Test
  void filtraPorQuienAccionYFecha() {
    guardar("admin-a", AccionDeAdministracion.DONACION_VENCIDA, LocalDate.of(2026, 11, 11));
    guardar("admin-a", AccionDeAdministracion.DONACION_VENCIDA, LocalDate.of(2026, 11, 12));
    guardar("admin-a", AccionDeAdministracion.DONACION_VENCIDA, LocalDate.of(2026, 11, 9));
    guardar("admin-a", AccionDeAdministracion.CAMION_AGREGADO, LocalDate.of(2026, 11, 11));
    guardar("admin-b", AccionDeAdministracion.DONACION_VENCIDA, LocalDate.of(2026, 11, 11));

    Pagina<ActividadDTO> pagina =
        servicio.listar(
            new FiltroActividad(
                List.of("admin-a"),
                List.of("DONACION_VENCIDA"),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12)),
            new ParametrosListado(0, 1, null, null));

    assertEquals(2, pagina.total());
    assertEquals(1, pagina.elementos().size());
    ActividadDTO fila = pagina.elementos().get(0);
    assertEquals("admin-a", fila.autor());
    assertEquals(LocalDate.of(2026, 11, 12), fila.ocurrioEn().toLocalDate());
  }

  @Test
  void parametrosInvalidos_dan400() {
    ParametrosListado primera = ParametrosListado.primeraPagina();
    List<String> veintiuno = new ArrayList<>();
    IntStream.range(0, 21).forEach(i -> veintiuno.add("admin-" + i));
    List<FiltroActividad> invalidos =
        List.of(
            new FiltroActividad(null, List.of("INVENTADA"), null, null),
            new FiltroActividad(veintiuno, null, null, null),
            new FiltroActividad(List.of("x".repeat(65)), null, null, null),
            new FiltroActividad(List.of(" "), null, null, null),
            new FiltroActividad(null, null, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 1)));

    invalidos.forEach(
        filtro -> assertThrows(ValidationException.class, () -> servicio.listar(filtro, primera)));
    ParametrosListado grande = new ParametrosListado(0, 101, null, null);
    FiltroActividad ninguno = FiltroActividad.ninguno();
    assertThrows(ValidationException.class, () -> servicio.listar(ninguno, grande));
  }

  @Test
  void anotar_unaAccionDelSitio_guardaConElAutorDelToken() {
    ActividadDTO anotada =
        servicio.anotar(new AnotacionActividadDTO("CAMION_AGREGADO", "camion-1", "AB123CD"));

    assertEquals("admin-a", anotada.autor());
    assertEquals(AHORA, anotada.ocurrioEn());
    assertEquals("AB123CD", unica().detalle());
  }

  @Test
  void anotar_rechazaLoQueNoEsDelSitio() {
    List<AnotacionActividadDTO> invalidas =
        List.of(
            new AnotacionActividadDTO("PROPUESTA_APROBADA", "p-1", null),
            new AnotacionActividadDTO("INVENTADA", "p-1", null),
            new AnotacionActividadDTO(null, "p-1", null),
            new AnotacionActividadDTO("CAMION_AGREGADO", " ", null),
            new AnotacionActividadDTO("CAMION_AGREGADO", "x".repeat(65), null),
            new AnotacionActividadDTO("CAMION_AGREGADO", "camion-1", "x".repeat(21)),
            new AnotacionActividadDTO("ADMINISTRADOR_DADO_DE_ALTA", "sub-nuevo", "Bruno Díaz"));

    invalidas.forEach(
        anotacion -> assertThrows(ValidationException.class, () -> servicio.anotar(anotacion)));
    assertTrue(todas().isEmpty());
  }

  @Test
  void anotar_sinTokenDeAdmin_noGuardaNada() {
    cuenta = Optional.empty();
    AnotacionActividadDTO alta =
        new AnotacionActividadDTO("ADMINISTRADOR_DADO_DE_ALTA", "sub-nuevo", null);

    assertThrows(ValidationException.class, () -> servicio.anotar(alta));
    assertTrue(todas().isEmpty());
  }

  private void guardar(String autor, AccionDeAdministracion accion, LocalDate dia) {
    repositorio.guardar(
        new Actividad(UUID.randomUUID(), dia.atTime(12, 0), autor, accion, "o", null));
  }

  private static HumanaOutputDTO humana() {
    return mock(HumanaOutputDTO.class);
  }

  private static JuridicaOutputDTO juridica(String razonSocial) {
    return new JuridicaOutputDTO(
        null,
        UUID.randomUUID(),
        null,
        null,
        null,
        Collections.emptyList(),
        razonSocial,
        null,
        null,
        Collections.emptyList());
  }
}
