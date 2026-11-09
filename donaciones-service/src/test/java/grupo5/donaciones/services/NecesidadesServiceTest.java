package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.necesidades.DatosNecesidad;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.Periodicidad;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import grupo5.donaciones.models.entities.necesidades.ResultadoPeriodo;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.impl.AcumuladosDeAsignacion;
import grupo5.donaciones.services.impl.NecesidadesService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadesServiceTest {

  private INecesidadesRepository necesidadRepositoryMock;
  private IEntidadesBeneficiariasRepository entidadesBeneficiariasRepositoryMock;
  private ISubcategoriasRepository subcategoriaRepositoryMock;
  private IDonacionesIndependientesRepository donacionesIndependientesRepositoryMock;
  private ICategoriasRepository categoriasRepositoryMock;
  private NecesidadesService necesidadesService;

  @BeforeEach
  void setUp() {
    necesidadRepositoryMock = mock(INecesidadesRepository.class);
    entidadesBeneficiariasRepositoryMock = mock(IEntidadesBeneficiariasRepository.class);
    subcategoriaRepositoryMock = mock(ISubcategoriasRepository.class);
    donacionesIndependientesRepositoryMock = mock(IDonacionesIndependientesRepository.class);
    categoriasRepositoryMock = mock(ICategoriasRepository.class);

    necesidadesService =
        new NecesidadesService(
            necesidadRepositoryMock,
            entidadesBeneficiariasRepositoryMock,
            subcategoriaRepositoryMock,
            categoriasRepositoryMock,
            new AcumuladosDeAsignacion(donacionesIndependientesRepositoryMock));
  }

  @Test
  void guardar_DeberiaPersistirNecesidadRecurrenteCorrectamente() {
    UUID subcategoriaId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();

    NecesidadDTO inputDto =
        entrada("RECURRENTE", entidadId, subcategoriaId, null, Periodicidad.MENSUAL);

    Subcategoria subcategoriaMock = mock(Subcategoria.class);
    EntidadBeneficiaria entidadMock = mock(EntidadBeneficiaria.class);
    when(entidadMock.estaActivo()).thenReturn(true);

    when(subcategoriaRepositoryMock.findById(subcategoriaId))
        .thenReturn(Optional.of(subcategoriaMock));
    when(entidadesBeneficiariasRepositoryMock.findById(entidadId))
        .thenReturn(Optional.of(entidadMock));

    NecesidadDTO resultado = necesidadesService.guardar(inputDto);

    assertNotNull(resultado);
    assertEquals("RECURRENTE", resultado.tipo());
    assertEquals(Periodicidad.MENSUAL, resultado.periodicidad());
    assertEquals(EstadoNecesidad.VIGENTE, resultado.estado());
    verify(necesidadRepositoryMock, times(1)).save(any(Necesidad.class));
  }

  @Test
  void guardar_conEntidadDeBaja_lanzaEntidadInactivaYNoGuarda() {
    UUID entidadId = UUID.randomUUID();
    NecesidadDTO inputDto =
        new NecesidadDTO(
            UUID.randomUUID(),
            "EXTRAORDINARIA",
            entidadId,
            UUID.randomUUID(),
            5,
            "Mantas",
            false,
            null,
            null);
    EntidadBeneficiaria entidadDeBaja = mock(EntidadBeneficiaria.class);
    when(entidadDeBaja.estaActivo()).thenReturn(false);
    when(subcategoriaRepositoryMock.findById(inputDto.idSubcategoria()))
        .thenReturn(Optional.of(mock(Subcategoria.class)));
    when(entidadesBeneficiariasRepositoryMock.findById(entidadId))
        .thenReturn(Optional.of(entidadDeBaja));

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> necesidadesService.guardar(inputDto));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_INACTIVA, ex.getError());
    verify(necesidadRepositoryMock, never()).save(any(Necesidad.class));
  }

  @Test
  void guardar_DeberiaPersistirNecesidadExtraordinariaCorrectamente() {
    UUID subcategoriaId = UUID.randomUUID();
    UUID entidadId = UUID.randomUUID();

    NecesidadDTO inputDto =
        entrada("EXTRAORDINARIA", entidadId, subcategoriaId, NecesidadMother.FECHA_LIMITE, null);

    Subcategoria subcategoriaMock = mock(Subcategoria.class);
    EntidadBeneficiaria entidadMock = mock(EntidadBeneficiaria.class);
    when(entidadMock.estaActivo()).thenReturn(true);

    when(subcategoriaRepositoryMock.findById(subcategoriaId))
        .thenReturn(Optional.of(subcategoriaMock));
    when(entidadesBeneficiariasRepositoryMock.findById(entidadId))
        .thenReturn(Optional.of(entidadMock));

    NecesidadDTO resultado = necesidadesService.guardar(inputDto);

    assertNotNull(resultado);
    assertEquals("EXTRAORDINARIA", resultado.tipo());
    assertEquals(NecesidadMother.FECHA_LIMITE, resultado.fechaLimite());
    verify(necesidadRepositoryMock, times(1)).save(any(Necesidad.class));
  }

  @Test
  void obtenerPorId_DeberiaRetornarDto_CuandoElIdExiste() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(UUID.randomUUID(), 5);
    when(necesidadRepositoryMock.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));

    NecesidadDTO resultado = necesidadesService.obtenerPorId(necesidad.getId());

    assertNotNull(resultado);
    assertEquals(necesidad.getId(), resultado.id());
    assertEquals(necesidad.getDescripcion(), resultado.descripcion());
  }

  @Test
  void obtenerPorId_DeberiaLanzarExcepcion_CuandoElIdNoExiste() {
    UUID idInexistente = UUID.randomUUID();
    when(necesidadRepositoryMock.findById(idInexistente)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class,
        () -> {
          necesidadesService.obtenerPorId(idInexistente);
        });
  }

  // ===================== listarPublicas (HB-08) =====================

  @Test
  void listarPublicas_devuelveSoloLasActivasNoSatisfechasConElDtoPublico() {
    UUID entidadId = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();
    NecesidadExtraordinaria vigente = NecesidadMother.extraordinaria(entidadId, subcategoriaId, 10);
    NecesidadExtraordinaria satisfecha =
        NecesidadMother.extraordinaria(entidadId, subcategoriaId, 3);
    NecesidadExtraordinaria inactiva = NecesidadMother.extraordinaria(entidadId, subcategoriaId, 5);
    inactiva.desactivar();
    when(necesidadRepositoryMock.findAll()).thenReturn(List.of(vigente, satisfecha, inactiva));
    when(donacionesIndependientesRepositoryMock.sumarCantidadAsignada(anyMap()))
        .thenReturn(Map.of(vigente.getId(), 4, satisfecha.getId(), 3, inactiva.getId(), 0));

    List<NecesidadPublicaDTO> publicas = necesidadesService.listarPublicas(null);

    assertEquals(
        List.of(
            new NecesidadPublicaDTO(
                vigente.getId(),
                entidadId,
                subcategoriaId,
                vigente.getDescripcion(),
                10,
                "EXTRAORDINARIA",
                null)),
        publicas);
  }

  @Test
  void listarPublicas_porEntidad_filtraLasDeEsaEntidad() {
    UUID entidadId = UUID.randomUUID();
    NecesidadExtraordinaria propia =
        NecesidadMother.extraordinaria(entidadId, UUID.randomUUID(), 2);
    NecesidadExtraordinaria ajena =
        NecesidadMother.extraordinaria(UUID.randomUUID(), UUID.randomUUID(), 2);
    when(necesidadRepositoryMock.findAll()).thenReturn(List.of(propia, ajena));

    assertEquals(
        List.of(propia.getId()),
        necesidadesService.listarPublicas(entidadId).stream()
            .map(NecesidadPublicaDTO::id)
            .toList());
  }

  @Test
  void listarPublicas_recurrente_informaElFinDelPeriodoActual() {
    UUID entidadId = UUID.randomUUID();
    NecesidadRecurrente semanal =
        NecesidadMother.recurrenteSemanal(entidadId, UUID.randomUUID(), 4);
    when(necesidadRepositoryMock.findAll()).thenReturn(List.of(semanal));

    List<NecesidadPublicaDTO> publicas = necesidadesService.listarPublicas(entidadId);

    assertEquals(1, publicas.size());
    assertEquals("RECURRENTE", publicas.getFirst().tipo());
    assertEquals(semanal.obtenerPeriodoActual().fechaFin(), publicas.getFirst().fechaFin());
    assertNotNull(publicas.getFirst().fechaFin());
  }

  // ===================== Ciclo de vida (HB-18) =====================

  private static NecesidadDTO entrada(
      String tipo,
      UUID entidadId,
      UUID subcategoriaId,
      LocalDate fechaLimite,
      Periodicidad periodicidad) {
    return new NecesidadDTO(
        null,
        tipo,
        entidadId,
        subcategoriaId,
        10,
        "Fideos para el comedor",
        null,
        null,
        null,
        null,
        null,
        fechaLimite,
        periodicidad,
        null,
        null);
  }

  private void conEntidadYSubcategoriaValidas(NecesidadDTO dto) {
    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
    when(entidad.estaActivo()).thenReturn(true);
    when(subcategoriaRepositoryMock.findById(dto.idSubcategoria()))
        .thenReturn(Optional.of(mock(Subcategoria.class)));
    when(entidadesBeneficiariasRepositoryMock.findById(dto.idEntidad()))
        .thenReturn(Optional.of(entidad));
  }

  @Test
  void guardar_extraordinariaSinFechaLimite_respondeFechaLimiteNulaYNoGuarda() {
    NecesidadDTO dto = entrada("EXTRAORDINARIA", UUID.randomUUID(), UUID.randomUUID(), null, null);
    conEntidadYSubcategoriaValidas(dto);

    ValidationException ex =
        assertThrows(ValidationException.class, () -> necesidadesService.guardar(dto));

    assertEquals(ErrorCatalog.FECHA_LIMITE_NULA, ex.getError());
    verify(necesidadRepositoryMock, never()).save(any(Necesidad.class));
  }

  @Test
  void guardar_extraordinariaConFechaLimiteDeHoy_respondeFechaLimiteNoFutura() {
    NecesidadDTO dto =
        entrada(
            "EXTRAORDINARIA",
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDate.now(ZoneId.systemDefault()),
            null);
    conEntidadYSubcategoriaValidas(dto);

    ValidationException ex =
        assertThrows(ValidationException.class, () -> necesidadesService.guardar(dto));

    assertEquals(ErrorCatalog.FECHA_LIMITE_NO_FUTURA, ex.getError());
  }

  @Test
  void guardar_recurrenteSinPeriodicidad_respondeSinPeriodoAunqueTengaFechaFin() {
    NecesidadDTO dto =
        new NecesidadDTO(
            null,
            "RECURRENTE",
            UUID.randomUUID(),
            UUID.randomUUID(),
            10,
            "Leche",
            null,
            LocalDate.now(ZoneId.systemDefault()),
            LocalDate.now(ZoneId.systemDefault()).plusMonths(1));
    conEntidadYSubcategoriaValidas(dto);

    ValidationException ex =
        assertThrows(ValidationException.class, () -> necesidadesService.guardar(dto));

    assertEquals(ErrorCatalog.NECESIDAD_RECURRENTE_SIN_PERIODO, ex.getError());
    verify(necesidadRepositoryMock, never()).save(any(Necesidad.class));
  }

  @Test
  void guardar_recurrenteQuincenal_terminaSuPrimerPeriodoA15Dias() {
    NecesidadDTO dto =
        entrada("RECURRENTE", UUID.randomUUID(), UUID.randomUUID(), null, Periodicidad.QUINCENAL);
    conEntidadYSubcategoriaValidas(dto);

    NecesidadDTO resultado = necesidadesService.guardar(dto);

    assertEquals(Periodicidad.QUINCENAL, resultado.periodicidad());
    assertEquals(LocalDate.now(ZoneId.systemDefault()).plusDays(15), resultado.fechaFin());
    assertNull(resultado.fechaLimite());
  }

  @Test
  void listarConFiltros_calculaElEstadoYFiltraPorEl() {
    UUID subcategoriaId = UUID.randomUUID();
    NecesidadExtraordinaria vigente = NecesidadMother.extraordinaria(subcategoriaId, 10);
    NecesidadExtraordinaria cumplida = NecesidadMother.extraordinaria(subcategoriaId, 3);
    NecesidadExtraordinaria terminada = vencida(subcategoriaId);
    NecesidadExtraordinaria deBaja = NecesidadMother.extraordinaria(subcategoriaId, 5);
    deBaja.desactivar();
    when(necesidadRepositoryMock.findAll())
        .thenReturn(List.of(vigente, cumplida, terminada, deBaja));
    when(donacionesIndependientesRepositoryMock.sumarCantidadRecibida(anyMap()))
        .thenReturn(Map.of(cumplida.getId(), 3));

    Map<UUID, EstadoNecesidad> estados = new java.util.HashMap<>();
    necesidadesService
        .listarConFiltros(null, null, null)
        .forEach(dto -> estados.put(dto.id(), dto.estado()));

    assertEquals(
        Map.of(
            vigente.getId(), EstadoNecesidad.VIGENTE,
            cumplida.getId(), EstadoNecesidad.CUMPLIDA,
            terminada.getId(), EstadoNecesidad.TERMINADA,
            deBaja.getId(), EstadoNecesidad.DADA_DE_BAJA),
        estados);
    assertEquals(
        List.of(vigente.getId(), terminada.getId()),
        necesidadesService
            .listarConFiltros(
                null, null, List.of(EstadoNecesidad.VIGENTE, EstadoNecesidad.TERMINADA))
            .stream()
            .map(NecesidadDTO::id)
            .toList());
  }

  @Test
  void listarPublicas_noIncluyeLasVencidas() {
    NecesidadExtraordinaria terminada = vencida(UUID.randomUUID());
    when(necesidadRepositoryMock.findAll()).thenReturn(List.of(terminada));

    assertTrue(necesidadesService.listarPublicas(null).isEmpty());
  }

  /** Una extraordinaria guardada antes con una fecha límite que ya pasó. */
  private static NecesidadExtraordinaria vencida(UUID subcategoriaId) {
    return new NecesidadExtraordinaria(
        new DatosNecesidad(
            UUID.randomUUID(),
            subcategoriaId,
            8,
            "Vencida",
            LocalDate.now(ZoneId.systemDefault()).minusDays(20),
            null,
            0L,
            null,
            null),
        true,
        LocalDate.now(ZoneId.systemDefault()).minusDays(1));
  }

  @Test
  void obtenerPorId_devuelveLaUnidadPropiaOLaDeLaCategoria() {
    Categoria alimentos = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    Subcategoria aceite = new Subcategoria(alimentos.getId(), "Aceite");
    aceite.asignarUnidad(Unidad.LITROS);
    Subcategoria fideos = new Subcategoria(alimentos.getId(), "Fideos");
    when(categoriasRepositoryMock.findAll()).thenReturn(List.of(alimentos));
    when(subcategoriaRepositoryMock.findAll()).thenReturn(List.of(aceite, fideos));
    NecesidadExtraordinaria deAceite = NecesidadMother.extraordinaria(aceite.getId(), 4);
    NecesidadExtraordinaria deFideos = NecesidadMother.extraordinaria(fideos.getId(), 4);
    when(necesidadRepositoryMock.findById(deAceite.getId())).thenReturn(Optional.of(deAceite));
    when(necesidadRepositoryMock.findById(deFideos.getId())).thenReturn(Optional.of(deFideos));

    assertEquals(Unidad.LITROS, necesidadesService.obtenerPorId(deAceite.getId()).unidad());
    assertEquals(Unidad.KILOGRAMO, necesidadesService.obtenerPorId(deFideos.getId()).unidad());
  }

  @Test
  void actualizar_conOtraVersion_respondeModificadaConcurrentementeYNoGuarda() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(UUID.randomUUID(), 5);
    necesidad.asignarVersion(3L);
    when(necesidadRepositoryMock.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));
    NecesidadDTO cambio = conVersion(2L);
    UUID id = necesidad.getId();

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> necesidadesService.actualizar(id, cambio));

    assertEquals(ErrorCatalog.NECESIDAD_MODIFICADA_CONCURRENTEMENTE, ex.getError());
    assertEquals(5, necesidad.getCantidadNecesitada());
    verify(necesidadRepositoryMock, never()).save(any(Necesidad.class));
  }

  @Test
  void actualizar_conLaMismaVersionOSinVersion_guardaYDevuelveElUltimoCambio() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(UUID.randomUUID(), 5);
    necesidad.asignarVersion(3L);
    when(necesidadRepositoryMock.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));

    NecesidadDTO resultado = necesidadesService.actualizar(necesidad.getId(), conVersion(3L));

    assertEquals(9, resultado.cantidadNecesitada());
    assertEquals(
        new NecesidadDTO.UltimoCambio(5, LocalDate.now(ZoneId.systemDefault())),
        resultado.ultimoCambio());
    assertNotNull(necesidadesService.actualizar(necesidad.getId(), conVersion(null)));
    verify(necesidadRepositoryMock, times(2)).save(necesidad);
  }

  private static NecesidadDTO conVersion(Long version) {
    return new NecesidadDTO(
        null,
        "EXTRAORDINARIA",
        null,
        UUID.randomUUID(),
        9,
        "Mantas",
        null,
        null,
        null,
        null,
        version,
        null,
        null,
        null,
        null);
  }

  @Test
  void listarPeriodos_deUnaRecurrente_devuelveObjetivoAsignadoRecibidoYResultado() {
    NecesidadRecurrente semanal = NecesidadMother.recurrenteSemanal(UUID.randomUUID(), 4);
    semanal.generarNuevoPeriodo();
    List<PeriodoNecesidad> periodos = semanal.getPeriodos();
    UUID cerrado = periodos.get(0).id();
    UUID enCurso = periodos.get(1).id();
    when(necesidadRepositoryMock.findById(semanal.getId())).thenReturn(Optional.of(semanal));
    when(donacionesIndependientesRepositoryMock.sumarCantidadAsignada(semanal.getId(), cerrado))
        .thenReturn(4);
    when(donacionesIndependientesRepositoryMock.sumarCantidadRecibida(semanal.getId(), cerrado))
        .thenReturn(2);

    List<PeriodoNecesidadDTO> historia = necesidadesService.listarPeriodos(semanal.getId());

    assertEquals(
        List.of(
            new PeriodoNecesidadDTO(
                cerrado,
                semanal.getFechaInicio(),
                periodos.get(0).fechaFin(),
                4,
                4,
                2,
                ResultadoPeriodo.INCOMPLETO),
            new PeriodoNecesidadDTO(
                enCurso,
                periodos.get(0).fechaFin(),
                periodos.get(1).fechaFin(),
                4,
                0,
                0,
                ResultadoPeriodo.EN_CURSO)),
        historia);
  }

  @Test
  void listarPeriodos_deUnaExtraordinaria_devuelveVacio() {
    NecesidadExtraordinaria necesidad = NecesidadMother.extraordinaria(UUID.randomUUID(), 5);
    when(necesidadRepositoryMock.findById(necesidad.getId())).thenReturn(Optional.of(necesidad));

    assertTrue(necesidadesService.listarPeriodos(necesidad.getId()).isEmpty());
  }
}
