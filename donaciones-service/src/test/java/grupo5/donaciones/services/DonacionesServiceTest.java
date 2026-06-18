package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.models.entities.donaciones.Deposito;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.*;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.impl.DonacionesService;
import grupo5.donaciones.services.mappers.DonacionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
  @Mock private DonacionMapper mapper;

  @InjectMocks private DonacionesService service;

  private Humana persona;
  private Donacion donacion;
  private DonacionInputDTO inputDTO;
  private DonacionOutputDTO outputDTO;

  @BeforeEach
  void setUp() {
    persona = new Humana("Juan", "Perez", LocalDate.of(1990, 1, 1));

    Donante donante = new Donante(persona);
    Pais pais = new Pais();
    pais.setNombre("Argentina");
    Provincia provincia = new Provincia();
    provincia.setNombre("Buenos Aires");
    provincia.setPais(pais);
    Localidad localidad = new Localidad();
    localidad.setNombre("CABA");
    localidad.setProvincia(provincia);
    Direccion direccion = new Direccion("Calle Falsa", 123, null, null, "1000", localidad);
    Deposito deposito = new Deposito("Deposito Test", direccion);
    donacion = new Donacion(donante, deposito);

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
            persona.getId(),
            List.of(),
            "desc",
            LocalDateTime.now(),
            dirOut,
            EstadoDonacion.CARGADA,
            List.of());
  }

  @Test
  void cargarDonacion_cuandoPersonaExiste_deberiaGuardarYRetornarDTO() {
    when(personasRepository.findById(persona.getId())).thenReturn(Optional.of(persona));
    when(mapper.toEntity(inputDTO, persona)).thenReturn(donacion);
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
