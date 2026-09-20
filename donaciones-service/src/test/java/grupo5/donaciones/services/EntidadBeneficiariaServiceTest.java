package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
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
  private EntidadBeneficiariaMapper mapper;
  private EntidadBeneficiariaService service;

  @BeforeEach
  void setUp() {
    repository = mock(IEntidadesBeneficiariasRepository.class);
    personasRepository = mock(IPersonasRepository.class);
    mapper =
        new EntidadBeneficiariaMapper(
            new PersonaMapper(new DireccionMapper(), new MedioDeContactoMapper()),
            personasRepository);

    service = new EntidadBeneficiariaService(repository, personasRepository, mapper);
  }

  @Test
  void crearEntidad_debeCrearYRetornarDTO() {
    UUID juridicaId = UUID.randomUUID();

    Juridica juridica = PersonaMother.fundacionEsperanza();
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(juridica));

    when(repository.save(any(EntidadBeneficiaria.class))).thenAnswer(inv -> inv.getArgument(0));

    EntidadBeneficiariaOutputDTO resultado =
        service.crearEntidad(new EntidadBeneficiariaInputDTO(juridicaId));

    assertNotNull(resultado);
    assertNotNull(resultado.id());
    assertEquals("Fundación Esperanza", resultado.juridica().razonSocial());

    verify(personasRepository, times(2)).findById(juridicaId);
    verify(repository).save(any(EntidadBeneficiaria.class));
  }

  @Test
  void obtenerEntidad_debeRetornarDTO() {
    UUID id = UUID.randomUUID();
    UUID juridicaId = UUID.randomUUID();

    Juridica juridica = PersonaMother.fundacionEsperanza();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridicaId);

    when(repository.findById(id)).thenReturn(Optional.of(entidad));
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(juridica));

    EntidadBeneficiariaOutputDTO resultado = service.obtenerEntidad(id);

    assertNotNull(resultado);
    assertEquals("Fundación Esperanza", resultado.juridica().razonSocial());
  }

  @Test
  void obtenerTodas_debeRetornarListaDeDTOs() {
    UUID juridicaId = UUID.randomUUID();
    Juridica juridica = PersonaMother.fundacionEsperanza();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridicaId);

    when(repository.findAll()).thenReturn(List.of(entidad));
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(juridica));

    List<EntidadBeneficiariaOutputDTO> resultado = service.obtenerTodas();

    assertEquals(1, resultado.size());
    assertEquals("Fundación Esperanza", resultado.getFirst().juridica().razonSocial());
  }
}
