package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.publico.EntidadPublicaDTO;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.impl.EntidadesBeneficiariasRepositoryEnMemoria;
import grupo5.donaciones.services.impl.CancelacionDeEntregas;
import grupo5.donaciones.services.impl.EntidadBeneficiariaService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.EntidadBeneficiariaMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaServiceTest {
  private IEntidadesBeneficiariasRepository repository;
  private IPersonasRepository personasRepository;
  private INecesidadesRepository necesidadesRepository;
  private EntidadBeneficiariaMapper mapper;
  private EntidadBeneficiariaService service;
  // R11-01: la búsqueda se resuelve en el repositorio; estas pruebas usan el de memoria
  private EntidadesBeneficiariasRepositoryEnMemoria enMemoria;
  private EntidadBeneficiariaService serviceConBusqueda;
  private final List<Persona> personasRegistradas = new ArrayList<>();

  private CancelacionDeEntregas cancelacionDeEntregas = mock(CancelacionDeEntregas.class);

  @BeforeEach
  void setUp() {
    repository = mock(IEntidadesBeneficiariasRepository.class);
    personasRepository = mock(IPersonasRepository.class);
    necesidadesRepository = mock(INecesidadesRepository.class);
    mapper =
        new EntidadBeneficiariaMapper(
            new PersonaMapper(new DireccionMapper(), new MedioDeContactoMapper()),
            personasRepository);

    service =
        new EntidadBeneficiariaService(
            repository, personasRepository, necesidadesRepository, mapper, cancelacionDeEntregas);

    when(personasRepository.findAll()).thenAnswer(inv -> List.copyOf(personasRegistradas));
    when(personasRepository.findAllById(any()))
        .thenAnswer(
            inv ->
                inv.<Collection<UUID>>getArgument(0).stream()
                    .map(personasRepository::findById)
                    .flatMap(o -> o == null ? Stream.empty() : o.stream())
                    .toList());
    enMemoria = new EntidadesBeneficiariasRepositoryEnMemoria(personasRepository);
    serviceConBusqueda =
        new EntidadBeneficiariaService(
            enMemoria, personasRepository, necesidadesRepository, mapper, cancelacionDeEntregas);
  }

  @Test
  void crearEntidad_debeCrearConElIdDeLaJuridicaYRetornarDTO() {
    Juridica juridica = PersonaMother.fundacionEsperanza();
    UUID juridicaId = juridica.getId();
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(juridica));
    when(repository.findById(juridicaId)).thenReturn(Optional.empty());
    when(repository.save(any(EntidadBeneficiaria.class))).thenAnswer(inv -> inv.getArgument(0));

    ResultadoRegistro<EntidadBeneficiariaOutputDTO> resultado =
        service.crearEntidad(new EntidadBeneficiariaInputDTO(juridicaId));

    assertTrue(resultado.creado());
    assertNotNull(resultado.recurso());
    assertEquals(juridicaId, resultado.recurso().id());
    assertTrue(resultado.recurso().activo());
    assertEquals("Fundación Esperanza", resultado.recurso().juridica().razonSocial());
    verify(repository).save(any(EntidadBeneficiaria.class));
  }

  @Test
  void crearEntidad_cuandoYaExisteActiva_noDuplica() {
    Juridica juridica = PersonaMother.fundacionEsperanza();
    UUID juridicaId = juridica.getId();
    EntidadBeneficiaria existente = new EntidadBeneficiaria(juridicaId);
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(juridica));
    when(repository.findById(juridicaId)).thenReturn(Optional.of(existente));

    ResultadoRegistro<EntidadBeneficiariaOutputDTO> resultado =
        service.crearEntidad(new EntidadBeneficiariaInputDTO(juridicaId));

    assertFalse(resultado.creado());
    assertEquals(juridicaId, resultado.recurso().id());
    verify(repository, never()).save(any(EntidadBeneficiaria.class));
  }

  @Test
  void crearEntidad_cuandoEstabaDeBaja_laReactiva() {
    Juridica juridica = PersonaMother.fundacionEsperanza();
    UUID juridicaId = juridica.getId();
    EntidadBeneficiaria dadaDeBaja = new EntidadBeneficiaria(juridicaId);
    dadaDeBaja.darDeBaja();
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(juridica));
    when(repository.findById(juridicaId)).thenReturn(Optional.of(dadaDeBaja));
    when(repository.save(any(EntidadBeneficiaria.class))).thenAnswer(inv -> inv.getArgument(0));

    ResultadoRegistro<EntidadBeneficiariaOutputDTO> resultado =
        service.crearEntidad(new EntidadBeneficiariaInputDTO(juridicaId));

    assertFalse(resultado.creado());
    assertTrue(dadaDeBaja.estaActivo());
    verify(repository).save(dadaDeBaja);
  }

  @Test
  void crearEntidad_conEmpresa_lanzaTipoInvalidoYNoGuarda() {
    Juridica empresa = PersonaMother.empresaSA();
    when(personasRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
    EntidadBeneficiariaInputDTO input = new EntidadBeneficiariaInputDTO(empresa.getId());

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.crearEntidad(input));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_TIPO_INVALIDO, ex.getError());
    verify(repository, never()).save(any(EntidadBeneficiaria.class));
  }

  @Test
  void crearEntidad_conJuridicaAnonimizada_lanzaPersonaAnonimizadaYNoReactiva() {
    Juridica anonimizada = PersonaMother.fundacionEsperanza();
    anonimizada.anonimizar();
    EntidadBeneficiaria deBaja = new EntidadBeneficiaria(anonimizada.getId());
    deBaja.darDeBaja();
    when(personasRepository.findById(anonimizada.getId())).thenReturn(Optional.of(anonimizada));
    when(repository.findById(anonimizada.getId())).thenReturn(Optional.of(deBaja));
    EntidadBeneficiariaInputDTO input = new EntidadBeneficiariaInputDTO(anonimizada.getId());

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> service.crearEntidad(input));

    assertEquals(ErrorCatalog.PERSONA_ANONIMIZADA_NO_OPERABLE, ex.getError());
    assertFalse(deBaja.estaActivo());
    verify(repository, never()).save(any(EntidadBeneficiaria.class));
  }

  @Test
  void crearEntidad_conPersonaHumana_lanzaSinPersonaJuridica() {
    var humana = PersonaMother.juanPerez();
    when(personasRepository.findById(humana.getId())).thenReturn(Optional.of(humana));
    EntidadBeneficiariaInputDTO input = new EntidadBeneficiariaInputDTO(humana.getId());

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.crearEntidad(input));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA, ex.getError());
  }

  @Test
  void crearEntidad_conPersonaInexistente_lanzaRecursoNoEncontrado() {
    UUID id = UUID.randomUUID();
    when(personasRepository.findById(id)).thenReturn(Optional.empty());
    EntidadBeneficiariaInputDTO input = new EntidadBeneficiariaInputDTO(id);

    assertThrows(RecursoNoEncontradoException.class, () -> service.crearEntidad(input));
  }

  @Test
  void actualizarEntidad_revalidaYNoCambiaElEstado() {
    Juridica juridica = PersonaMother.fundacionEsperanza();
    UUID id = juridica.getId();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(id);
    when(repository.findById(id)).thenReturn(Optional.of(entidad));
    when(personasRepository.findById(id)).thenReturn(Optional.of(juridica));

    EntidadBeneficiariaOutputDTO resultado =
        service.actualizarEntidad(id, new EntidadBeneficiariaInputDTO(id));

    assertEquals(id, resultado.id());
    assertTrue(entidad.estaActivo());
    verify(repository, never()).save(any(EntidadBeneficiaria.class));
  }

  @Test
  void actualizarEntidad_conIdDistintoAlDeLaJuridica_lanzaArgumentoInvalido() {
    UUID id = UUID.randomUUID();
    EntidadBeneficiariaInputDTO input = new EntidadBeneficiariaInputDTO(UUID.randomUUID());

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.actualizarEntidad(id, input));

    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
  }

  @Test
  void actualizarEntidad_inexistente_lanzaRecursoNoEncontrado() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());
    EntidadBeneficiariaInputDTO input = new EntidadBeneficiariaInputDTO(id);

    assertThrows(RecursoNoEncontradoException.class, () -> service.actualizarEntidad(id, input));
  }

  @Test
  void actualizarEntidad_deBaja_lanzaEntidadInactiva() {
    Juridica juridica = PersonaMother.fundacionEsperanza();
    UUID id = juridica.getId();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(id);
    entidad.darDeBaja();
    when(repository.findById(id)).thenReturn(Optional.of(entidad));
    when(personasRepository.findById(id)).thenReturn(Optional.of(juridica));
    EntidadBeneficiariaInputDTO input = new EntidadBeneficiariaInputDTO(id);

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> service.actualizarEntidad(id, input));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_INACTIVA, ex.getError());
  }

  @Test
  void actualizarEntidad_siLaJuridicaPasoAEmpresa_lanzaTipoInvalido() {
    Juridica empresa = PersonaMother.empresaSA();
    UUID id = empresa.getId();
    when(repository.findById(id)).thenReturn(Optional.of(new EntidadBeneficiaria(id)));
    when(personasRepository.findById(id)).thenReturn(Optional.of(empresa));
    EntidadBeneficiariaInputDTO input = new EntidadBeneficiariaInputDTO(id);

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.actualizarEntidad(id, input));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_TIPO_INVALIDO, ex.getError());
  }

  @Test
  void obtenerEntidad_debeRetornarDTO() {
    Juridica juridica = PersonaMother.fundacionEsperanza();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridica.getId());

    when(repository.findById(juridica.getId())).thenReturn(Optional.of(entidad));
    when(personasRepository.findById(juridica.getId())).thenReturn(Optional.of(juridica));

    EntidadBeneficiariaOutputDTO resultado = service.obtenerEntidad(juridica.getId());

    assertNotNull(resultado);
    assertEquals("Fundación Esperanza", resultado.juridica().razonSocial());
  }

  @Test
  void eliminarEntidad_daDeBajaDesactivaSusNecesidadesYNoBorra() {
    UUID id = UUID.randomUUID();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(id);
    Necesidad n1 = mock(Necesidad.class);
    Necesidad n2 = mock(Necesidad.class);
    UUID idN1 = UUID.randomUUID();
    UUID idN2 = UUID.randomUUID();
    when(n1.getId()).thenReturn(idN1);
    when(n2.getId()).thenReturn(idN2);
    when(repository.findById(id)).thenReturn(Optional.of(entidad));
    when(necesidadesRepository.buscarNecesidadesPorEntidad(id)).thenReturn(List.of(n1, n2));

    service.eliminarEntidad(id);

    assertFalse(entidad.estaActivo());
    verify(repository).save(entidad);
    verify(repository, never()).delete(any(EntidadBeneficiaria.class));
    verify(n1).desactivar();
    verify(n2).desactivar();
    verify(necesidadesRepository).save(n1);
    verify(necesidadesRepository).save(n2);
    // HB-19: la baja en cascada también cancela lo que no salió de cada necesidad
    verify(cancelacionDeEntregas).cancelarLasQueNoSalieron(idN1);
    verify(cancelacionDeEntregas).cancelarLasQueNoSalieron(idN2);
  }

  @Test
  void eliminarEntidad_yaDadaDeBaja_noRepiteLaCascada() {
    UUID id = UUID.randomUUID();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(id);
    entidad.darDeBaja();
    when(repository.findById(id)).thenReturn(Optional.of(entidad));

    service.eliminarEntidad(id);

    verify(repository, never()).save(any(EntidadBeneficiaria.class));
    verify(necesidadesRepository, never()).buscarNecesidadesPorEntidad(any());
  }

  @Test
  void eliminarEntidad_inexistente_lanzaRecursoNoEncontrado() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.eliminarEntidad(id));
  }

  @Test
  void darDeBajaSiExiste_conEntidadActiva_laDaDeBajaConCascada() {
    UUID id = UUID.randomUUID();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(id);
    Necesidad necesidad = mock(Necesidad.class);
    when(repository.findById(id)).thenReturn(Optional.of(entidad));
    when(necesidadesRepository.buscarNecesidadesPorEntidad(id)).thenReturn(List.of(necesidad));

    service.darDeBajaSiExiste(id);

    assertFalse(entidad.estaActivo());
    verify(necesidad).desactivar();
  }

  @Test
  void darDeBajaSiExiste_sinEntidad_noHaceNada() {
    UUID personaSinRol = UUID.randomUUID();
    when(repository.findById(personaSinRol)).thenReturn(Optional.empty());

    service.darDeBajaSiExiste(personaSinRol);

    verify(repository, never()).save(any(EntidadBeneficiaria.class));
    verify(necesidadesRepository, never()).buscarNecesidadesPorEntidad(any());
  }

  @Test
  void obtenerEntidad_deUnaEntidadDeBaja_laDevuelveMarcadaInactiva() {
    Juridica juridica = PersonaMother.fundacionEsperanza();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridica.getId());
    entidad.darDeBaja();
    when(repository.findById(juridica.getId())).thenReturn(Optional.of(entidad));
    when(personasRepository.findById(juridica.getId())).thenReturn(Optional.of(juridica));

    assertFalse(service.obtenerEntidad(juridica.getId()).activo());
  }

  @Test
  void esEntidadActiva_distingueActivaDeBajaEInexistente() {
    UUID activa = UUID.randomUUID();
    UUID deBaja = UUID.randomUUID();
    UUID inexistente = UUID.randomUUID();
    EntidadBeneficiaria entidadDeBaja = new EntidadBeneficiaria(deBaja);
    entidadDeBaja.darDeBaja();
    when(repository.findById(activa)).thenReturn(Optional.of(new EntidadBeneficiaria(activa)));
    when(repository.findById(deBaja)).thenReturn(Optional.of(entidadDeBaja));
    when(repository.findById(inexistente)).thenReturn(Optional.empty());

    assertTrue(service.esEntidadActiva(activa));
    assertFalse(service.esEntidadActiva(deBaja));
    assertFalse(service.esEntidadActiva(inexistente));
  }

  // ===================== buscar (HB-15) =====================

  private static final String COMEDOR_SUR = "Comedor Sur";
  private static final String COMEDOR_NORTE = "COMEDOR Norte";
  private static final String BANCO_DE_ROPA = "banco de Ropa";
  private static final String ABRIGO_YA = "Abrigo Ya";

  /** Registra una entidad cuya jurídica tiene esa razón social y la devuelve. */
  private EntidadBeneficiaria entidadLlamada(String razonSocial) {
    Juridica juridica =
        new Juridica(PersonaMother.juanPerez(), razonSocial, TipoJuridico.ONG, "Social");
    when(personasRepository.findById(juridica.getId())).thenReturn(Optional.of(juridica));
    personasRegistradas.add(juridica);
    return enMemoria.save(new EntidadBeneficiaria(juridica.getId()));
  }

  /** Una entidad cuya jurídica no se encuentra: el DTO sale con {@code juridica} nula. */
  private EntidadBeneficiaria entidadSinJuridica() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(UUID.randomUUID());
    when(personasRepository.findById(entidad.juridicaId())).thenReturn(Optional.empty());
    return enMemoria.save(entidad);
  }

  /** Registra una entidad por cada razón social. */
  private void conEntidades(String... razonesSociales) {
    for (String razonSocial : razonesSociales) {
      entidadLlamada(razonSocial);
    }
  }

  private static List<String> nombres(Pagina<EntidadBeneficiariaOutputDTO> pagina) {
    return pagina.elementos().stream()
        .map(e -> e.juridica() == null ? null : e.juridica().razonSocial())
        .toList();
  }

  @Test
  void buscar_sinParametros_devuelveLaPrimeraPaginaOrdenadaPorNombreConElTotal_R1101() {
    conEntidades(COMEDOR_SUR, BANCO_DE_ROPA);

    Pagina<EntidadBeneficiariaOutputDTO> pagina = serviceConBusqueda.buscar(null, null, null);

    assertEquals(List.of(BANCO_DE_ROPA, COMEDOR_SUR), nombres(pagina));
    assertEquals(2, pagina.total());
  }

  @Test
  void buscar_sinParametros_nuncaDevuelveMasDeLaPaginaPorDefecto_R1101() {
    String[] razonesSociales =
        new String[EntidadBeneficiariaService.TAMANIO_PAGINA_POR_DEFECTO + 1];
    for (int i = 0; i < razonesSociales.length; i++) {
      razonesSociales[i] = String.format("Entidad %02d", i);
    }
    conEntidades(razonesSociales);

    Pagina<EntidadBeneficiariaOutputDTO> pagina = serviceConBusqueda.buscar(null, null, null);

    assertEquals(EntidadBeneficiariaService.TAMANIO_PAGINA_POR_DEFECTO, pagina.elementos().size());
    assertEquals(razonesSociales.length, pagina.total());
  }

  @Test
  void buscar_porNombre_filtraPorContieneSinDistinguirMayusculas() {
    conEntidades(COMEDOR_SUR, BANCO_DE_ROPA, COMEDOR_NORTE);

    Pagina<EntidadBeneficiariaOutputDTO> pagina = serviceConBusqueda.buscar("comedor", null, null);

    assertEquals(List.of(COMEDOR_NORTE, COMEDOR_SUR), nombres(pagina));
    assertEquals(2, pagina.total());
  }

  @Test
  void buscar_porNombre_ignoraLasTildesEnAmbosSentidos_R1101() {
    conEntidades("Asociación Gómez", "Comedor Gomez", BANCO_DE_ROPA);

    assertEquals(
        List.of("Asociación Gómez", "Comedor Gomez"),
        nombres(serviceConBusqueda.buscar("gomez", null, null)));
    assertEquals(
        List.of("Asociación Gómez", "Comedor Gomez"),
        nombres(serviceConBusqueda.buscar("GÓMEZ", null, null)));
  }

  @Test
  void buscar_porNombre_descartaEntidadesSinJuridica() {
    entidadSinJuridica();
    entidadLlamada(COMEDOR_SUR);

    assertEquals(List.of(COMEDOR_SUR), nombres(serviceConBusqueda.buscar("sur", null, null)));
  }

  @Test
  void buscar_paginado_ordenaPorNombreYRecortaConElTotalDeLasFiltradas() {
    conEntidades(COMEDOR_SUR, BANCO_DE_ROPA, ABRIGO_YA);

    Pagina<EntidadBeneficiariaOutputDTO> pagina = serviceConBusqueda.buscar(null, 1, 2);

    assertEquals(List.of(COMEDOR_SUR), nombres(pagina));
    assertEquals(3, pagina.total());
  }

  @Test
  void buscar_paginado_dejaAlFinalLasEntidadesSinJuridica() {
    entidadSinJuridica();
    entidadLlamada(ABRIGO_YA);

    assertEquals(Arrays.asList(ABRIGO_YA, null), nombres(serviceConBusqueda.buscar(null, 0, 2)));
  }

  @Test
  void buscar_paginado_conNombresIgualesDesempataPorId() {
    conEntidades(COMEDOR_SUR, COMEDOR_SUR);
    List<UUID> idsOrdenados =
        enMemoria.findAll().stream().map(EntidadBeneficiaria::getId).sorted().toList();

    List<UUID> ids =
        serviceConBusqueda.buscar(null, 0, 2).elementos().stream()
            .map(EntidadBeneficiariaOutputDTO::id)
            .toList();

    assertEquals(idsOrdenados, ids);
  }

  @Test
  void buscar_conSoloSize_empiezaPorLaPrimeraPagina() {
    conEntidades(COMEDOR_SUR, ABRIGO_YA);

    assertEquals(List.of(ABRIGO_YA), nombres(serviceConBusqueda.buscar(null, null, 1)));
  }

  @Test
  void buscar_conSoloPage_usaElTamanioPorDefecto() {
    String[] razonesSociales =
        new String[EntidadBeneficiariaService.TAMANIO_PAGINA_POR_DEFECTO + 1];
    for (int i = 0; i < razonesSociales.length; i++) {
      razonesSociales[i] = String.format("Entidad %02d", i);
    }
    conEntidades(razonesSociales);

    Pagina<EntidadBeneficiariaOutputDTO> pagina = serviceConBusqueda.buscar(null, 0, null);

    assertEquals(EntidadBeneficiariaService.TAMANIO_PAGINA_POR_DEFECTO, pagina.elementos().size());
    assertEquals(razonesSociales.length, pagina.total());
  }

  @Test
  void buscar_conElTamanioMaximo_loAcepta() {
    conEntidades(COMEDOR_SUR);

    assertEquals(
        List.of(COMEDOR_SUR),
        nombres(
            serviceConBusqueda.buscar(null, 0, EntidadBeneficiariaService.TAMANIO_PAGINA_MAXIMO)));
  }

  @Test
  void buscar_conPaginacionInvalida_lanzaValidationException() {
    int maximo = EntidadBeneficiariaService.TAMANIO_PAGINA_MAXIMO;
    for (Integer[] invalida : new Integer[][] {{-1, null}, {0, 0}, {0, maximo + 1}}) {
      ValidationException ex =
          assertThrows(
              ValidationException.class, () -> service.buscar(null, invalida[0], invalida[1]));
      assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
    }
    verify(repository, never()).buscar(any());
  }

  // ===================== buscarPublicas (HB-08) =====================

  @Test
  void buscarPublicas_devuelveSoloActivasConJuridicaYElDtoPublico() {
    Juridica juridica =
        new Juridica(PersonaMother.juanPerez(), COMEDOR_SUR, TipoJuridico.ONG, "Alimentos");
    when(personasRepository.findById(juridica.getId())).thenReturn(Optional.of(juridica));
    personasRegistradas.add(juridica);
    enMemoria.save(new EntidadBeneficiaria(juridica.getId()));
    EntidadBeneficiaria deBaja = entidadLlamada(ABRIGO_YA);
    deBaja.darDeBaja();
    enMemoria.save(deBaja);
    entidadSinJuridica();

    Pagina<EntidadPublicaDTO> pagina = serviceConBusqueda.buscarPublicas(null, null, null);

    assertEquals(
        List.of(new EntidadPublicaDTO(juridica.getId(), COMEDOR_SUR, "Alimentos")),
        pagina.elementos());
    assertEquals(1, pagina.total());
  }

  @Test
  void buscarPublicas_conNombreYPaginado_aplicaLaMismaBusqueda() {
    conEntidades(COMEDOR_SUR, COMEDOR_NORTE, BANCO_DE_ROPA);

    Pagina<EntidadPublicaDTO> pagina = serviceConBusqueda.buscarPublicas("comedor", 0, 1);

    assertEquals(
        List.of(COMEDOR_NORTE),
        pagina.elementos().stream().map(EntidadPublicaDTO::razonSocial).toList());
    assertEquals(2, pagina.total());
  }

  @Test
  void buscarPublicas_conPaginacionInvalida_lanzaValidationExceptionSinConsultar() {
    int maximo = EntidadBeneficiariaService.TAMANIO_PAGINA_MAXIMO;
    for (Integer[] invalida : new Integer[][] {{-1, null}, {null, 0}, {null, maximo + 1}}) {
      ValidationException ex =
          assertThrows(
              ValidationException.class,
              () -> service.buscarPublicas(null, invalida[0], invalida[1]));
      assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
    }
    verify(repository, never()).buscar(any());
  }
}
