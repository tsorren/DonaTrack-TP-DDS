package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.impl.EntidadBeneficiariaService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.EntidadBeneficiariaMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaServiceTest {
  private IEntidadesBeneficiariasRepository repository;
  private IPersonasRepository personasRepository;
  private INecesidadesRepository necesidadesRepository;
  private EntidadBeneficiariaMapper mapper;
  private EntidadBeneficiariaService service;

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
            repository, personasRepository, necesidadesRepository, mapper);
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
  void obtenerTodas_debeRetornarListaDeDTOs() {
    Juridica juridica = PersonaMother.fundacionEsperanza();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridica.getId());

    when(repository.findAll()).thenReturn(List.of(entidad));
    when(personasRepository.findById(juridica.getId())).thenReturn(Optional.of(juridica));

    List<EntidadBeneficiariaOutputDTO> resultado = service.obtenerTodas();

    assertEquals(1, resultado.size());
    assertEquals("Fundación Esperanza", resultado.getFirst().juridica().razonSocial());
    verify(personasRepository, times(1)).findById(juridica.getId());
  }

  @Test
  void eliminarEntidad_daDeBajaDesactivaSusNecesidadesYNoBorra() {
    UUID id = UUID.randomUUID();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(id);
    Necesidad n1 = mock(Necesidad.class);
    Necesidad n2 = mock(Necesidad.class);
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
}
