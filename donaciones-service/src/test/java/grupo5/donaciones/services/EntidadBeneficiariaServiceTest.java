package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.mappers.EntidadBeneficiariaMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EntidadBeneficiariaServiceTest {
  private IEntidadesBeneficiariasRepository repository;
  private IPersonasRepository personasRepository;
  private EntidadBeneficiariaMapper mapper;
  private EntidadBeneficiariaService service;

  @BeforeEach
  void setUp() {
    repository = mock(IEntidadesBeneficiariasRepository.class);
    personasRepository = mock(IPersonasRepository.class);
    mapper = mock(EntidadBeneficiariaMapper.class);

    service = new EntidadBeneficiariaService(repository, personasRepository, mapper);
  }

  @Test
  void crearEntidad_debeCrearYRetornarDTO() {
    UUID juridicaId = UUID.randomUUID();

    Juridica juridica = mock(Juridica.class);
    when(personasRepository.findById(juridicaId)).thenReturn(Optional.of(juridica));

    EntidadBeneficiaria entidad = new EntidadBeneficiaria(juridica);

    when(repository.save(any(EntidadBeneficiaria.class))).thenReturn(entidad);

    EntidadBeneficiariaOutputDTO dto =
        new EntidadBeneficiariaOutputDTO(entidad.getId(), mock(JuridicaOutputDTO.class));

    when(mapper.toOutputDTO(entidad)).thenReturn(dto);

    EntidadBeneficiariaOutputDTO resultado =
        service.crearEntidad(new EntidadBeneficiariaInputDTO(juridicaId));

    assertEquals(dto, resultado);

    verify(personasRepository).findById(juridicaId);
    verify(repository).save(any(EntidadBeneficiaria.class));
  }

  @Test
  void obtenerEntidad_debeRetornarDTO() {
    UUID id = UUID.randomUUID();

    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);

    when(repository.findById(id)).thenReturn(Optional.of(entidad));

    EntidadBeneficiariaOutputDTO dto = mock(EntidadBeneficiariaOutputDTO.class);

    when(mapper.toOutputDTO(entidad)).thenReturn(dto);

    EntidadBeneficiariaOutputDTO resultado = service.obtenerEntidad(id);

    assertEquals(dto, resultado);
  }
}
