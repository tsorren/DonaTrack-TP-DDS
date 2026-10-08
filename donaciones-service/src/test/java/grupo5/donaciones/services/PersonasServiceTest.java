package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.HumanaOutputDTO;
import grupo5.donaciones.dto.personas.JuridicaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.impl.NotificacionesAsyncService;
import grupo5.donaciones.services.impl.PersonasService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonasServiceTest {

  @Mock private IPersonasRepository repository;
  @Mock private NotificacionesAsyncService notificacionesAsyncService;
  @Mock private IDonantesService donantesService;
  @Mock private IEntidadBeneficiariaService entidadBeneficiariaService;

  private PersonaMapper mapper;
  private PersonasService service;

  private Humana humana;
  private HumanaInputDTO inputDTO;

  @BeforeEach
  void setUp() {
    mapper = new PersonaMapper(new DireccionMapper(), new MedioDeContactoMapper());
    service =
        new PersonasService(
            repository,
            mapper,
            notificacionesAsyncService,
            donantesService,
            entidadBeneficiariaService);

    humana = new Humana("Juan", "Perez", java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    inputDTO =
        new HumanaInputDTO(
            grupo5.donaciones.models.entities.personas.TipoPersona.HUMANA,
            grupo5.donaciones.models.entities.personas.TipoDocumento.DNI,
            "12345678",
            null,
            java.util.Collections.emptyList(),
            "Juan",
            "Perez",
            grupo5.donaciones.models.entities.personas.Genero.HOMBRE,
            java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
  }

  @Test
  void crearPersona_deberiaPersistirYSincronizar() {
    when(repository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

    PersonaOutputDTO result = service.crearPersona(inputDTO);

    assertNotNull(result);
    assertInstanceOf(HumanaOutputDTO.class, result);
    assertEquals("Juan", ((HumanaOutputDTO) result).nombre());
    verify(repository).save(any(Persona.class));
    verify(notificacionesAsyncService).sincronizarPersona(any(EventoPersonaSincronizadaV1.class));
  }

  @Test
  void actualizarPersona_siExiste_deberiaModificarYSincronizar() {
    UUID id = humana.getId();
    when(repository.findById(id)).thenReturn(Optional.of(humana));
    when(repository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

    PersonaOutputDTO result = service.actualizarPersona(id, inputDTO);

    assertNotNull(result);
    assertInstanceOf(HumanaOutputDTO.class, result);
    assertEquals("Juan", ((HumanaOutputDTO) result).nombre());
    verify(repository).save(humana);
    verify(notificacionesAsyncService).sincronizarPersona(any(EventoPersonaSincronizadaV1.class));
  }

  @Test
  void actualizarPersona_siNoExiste_deberiaLanzarExcepcion() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.actualizarPersona(id, inputDTO));
    verify(repository, never()).save(any());
    verify(notificacionesAsyncService, never()).sincronizarPersona(any());
  }

  @Test
  void eliminarPersona_siExiste_deberiaAnonimizarYSincronizar() {
    UUID id = humana.getId();
    when(repository.findById(id)).thenReturn(Optional.of(humana));
    when(repository.save(humana)).thenReturn(humana);

    service.eliminarPersona(id);

    verify(repository).save(humana);
    verify(notificacionesAsyncService)
        .sincronizarPersona(
            argThat(
                (EventoPersonaSincronizadaV1 evento) ->
                    evento != null
                        && evento.personaId().equals(id)
                        && grupo5.donaciones.models.privacidad.Anonimizable.VALOR_STRING.equals(
                            evento.denominacion())));
    assertEquals(grupo5.donaciones.models.privacidad.Anonimizable.VALOR_STRING, humana.getNombre());
  }

  @Test
  void eliminarPersona_daDeBajaSusRolesDeDonanteYDeEntidad() {
    UUID id = humana.getId();
    when(repository.findById(id)).thenReturn(Optional.of(humana));
    when(repository.save(humana)).thenReturn(humana);

    service.eliminarPersona(id);

    verify(donantesService).darDeBajaSiExiste(id);
    verify(entidadBeneficiariaService).darDeBajaSiExiste(id);
  }

  @Test
  void eliminarPersona_siNoExiste_deberiaLanzarExcepcion() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.eliminarPersona(id));
    verify(repository, never()).save(any());
    verify(notificacionesAsyncService, never()).sincronizarPersona(any());
    verify(donantesService, never()).darDeBajaSiExiste(any());
    verify(entidadBeneficiariaService, never()).darDeBajaSiExiste(any());
  }

  @Test
  void actualizarPersona_aEmpresaConEntidadActiva_lanzaTipoInvalidoYNoMuta() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    when(repository.findById(ong.getId())).thenReturn(Optional.of(ong));
    when(entidadBeneficiariaService.esEntidadActiva(ong.getId())).thenReturn(true);

    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () ->
                service.actualizarPersona(ong.getId(), juridicaInput(TipoJuridico.EMPRESA, true)));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_TIPO_INVALIDO, ex.getError());
    assertEquals(TipoJuridico.ONG, ong.getTipo());
    verify(repository, never()).save(any());
  }

  @Test
  void actualizarPersona_sinDireccionConEntidadActiva_lanzaSinDireccionYNoMuta() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    when(repository.findById(ong.getId())).thenReturn(Optional.of(ong));
    when(entidadBeneficiariaService.esEntidadActiva(ong.getId())).thenReturn(true);

    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> service.actualizarPersona(ong.getId(), juridicaInput(TipoJuridico.ONG, false)));

    assertEquals(ErrorCatalog.ENTIDAD_BENEFICIARIA_SIN_DIRECCION, ex.getError());
    assertNotNull(ong.getDireccion());
    verify(repository, never()).save(any());
  }

  @Test
  void actualizarPersona_aEmpresaSinEntidadActiva_esValido() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    when(repository.findById(ong.getId())).thenReturn(Optional.of(ong));
    when(repository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
    when(entidadBeneficiariaService.esEntidadActiva(ong.getId())).thenReturn(false);

    service.actualizarPersona(ong.getId(), juridicaInput(TipoJuridico.EMPRESA, true));

    assertEquals(TipoJuridico.EMPRESA, ong.getTipo());
  }

  @Test
  void actualizarPersona_conCambioValidoSobreEntidadActiva_loAplica() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    when(repository.findById(ong.getId())).thenReturn(Optional.of(ong));
    when(repository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
    when(entidadBeneficiariaService.esEntidadActiva(ong.getId())).thenReturn(true);

    service.actualizarPersona(ong.getId(), juridicaInput(TipoJuridico.INSTITUCION, true));

    assertEquals(TipoJuridico.INSTITUCION, ong.getTipo());
    verify(repository).save(ong);
  }

  @Test
  void actualizarPersona_sinTipoJuridicoEnElPedidoSobreEntidadActiva_conservaElActual() {
    Juridica ong = PersonaMother.fundacionEsperanza();
    when(repository.findById(ong.getId())).thenReturn(Optional.of(ong));
    when(repository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
    when(entidadBeneficiariaService.esEntidadActiva(ong.getId())).thenReturn(true);

    service.actualizarPersona(ong.getId(), juridicaInput(null, true));

    assertEquals(TipoJuridico.ONG, ong.getTipo());
  }

  private static JuridicaInputDTO juridicaInput(TipoJuridico tipo, boolean conDireccion) {
    return new JuridicaInputDTO(
        TipoPersona.JURIDICA,
        TipoDocumento.CUIT,
        "30-87654321-9",
        conDireccion ? DTOFixtures.direccionInput() : null,
        java.util.Collections.emptyList(),
        "Fundación Esperanza",
        tipo,
        "Social",
        java.util.Collections.emptyList());
  }
}
