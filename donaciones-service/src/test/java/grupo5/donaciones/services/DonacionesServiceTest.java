package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.infrastructure.ProcesadorDeDonaciones;
import grupo5.donaciones.models.entities.donaciones.Deposito;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.entities.ubicaciones.Localidad;
import grupo5.donaciones.models.entities.ubicaciones.Pais;
import grupo5.donaciones.models.entities.ubicaciones.Provincia;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.impl.DonacionesService;
import grupo5.donaciones.services.mappers.DonacionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class DonacionesServiceTest {

  @Mock private IDonacionesRepository donacionesRepository;
  @Mock private IPersonasRepository personasRepository;
  @Mock private IDonantesRepository donantesRepository;
  @Mock private IDonantesService donantesService;
  @Mock private DonacionMapper mapper;
  @Mock private ProcesadorDeDonaciones procesadorDonaciones;

  @InjectMocks private DonacionesService service;

  private Humana persona;
  private Donante donante;
  private Donacion donacion;
  private DonacionInputDTO inputDTO;
  private DonacionOutputDTO outputDTO;

  @BeforeEach
  void setUp() {
    persona = new Humana("Juan", "Perez", LocalDate.of(1990, Month.JANUARY, 1));

    donante = new Donante(persona.getId());
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("CABA", provincia);
    Direccion direccion = new Direccion("Calle Falsa", 123, null, null, "1000", localidad);
    Deposito deposito = new Deposito("Deposito Test", direccion);
    donacion = new Donacion(donante.getId(), deposito);

    DireccionInputDTO dirDTO =
        new DireccionInputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    inputDTO = new DonacionInputDTO(persona.getId(), "desc", List.of(), "Deposito Test", dirDTO);

    DireccionOutputDTO dirOut =
        new DireccionOutputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    outputDTO =
        new DonacionOutputDTO(
            donacion.getId(),
            new grupo5.donaciones.dto.donaciones.outputs.DonanteResumenDTO(
                donante.getId(), persona.getId(), null),
            List.of(),
            "desc",
            LocalDateTime.of(2026, Month.JUNE, 18, 0, 0),
            dirOut,
            EstadoDonacion.CARGADA,
            List.of());
  }

  @Test
  void cargarDonacion_cuandoPersonaExiste_deberiaGuardarYRetornarDTO() {
    when(personasRepository.findById(persona.getId())).thenReturn(Optional.of(persona));
    when(donantesRepository.findAll()).thenReturn(List.of(donante));
    when(mapper.toEntity(inputDTO, donante)).thenReturn(donacion);
    when(donacionesRepository.save(donacion)).thenReturn(donacion);
    when(mapper.toOutputDTO(donacion)).thenReturn(outputDTO);

    DonacionOutputDTO result = service.cargarDonacion(inputDTO);

    assertNotNull(result);
    verify(donacionesRepository).save(donacion);
    verify(mapper).toOutputDTO(donacion);
  }

  @Test
  void cargarDonacion_cuandoPersonaNoExiste_deberiaLanzarExcepcion() {
    when(personasRepository.findById(persona.getId())).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.cargarDonacion(inputDTO));
    verify(donacionesRepository, never()).save(any());
    verify(mapper, never()).toOutputDTO(any());
  }
}
