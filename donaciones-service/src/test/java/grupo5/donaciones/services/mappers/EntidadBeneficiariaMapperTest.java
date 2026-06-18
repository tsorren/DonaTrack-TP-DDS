package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntidadBeneficiariaMapperTest {

  @Mock private PersonaMapper personaMapper;

  @InjectMocks private EntidadBeneficiariaMapper mapper;

  @Test
  void toOutputDTO_debeMapearCorrectamente() {
    // Arrange
    UUID id = UUID.randomUUID();

    Juridica juridica = mock(Juridica.class);
    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);

    JuridicaOutputDTO juridicaDTO =
        new JuridicaOutputDTO(
            TipoPersona.JURIDICA,
            UUID.randomUUID(),
            TipoDocumento.DNI,
            "123",
            null,
            List.of(),
            "ONG",
            TipoJuridico.ONG,
            "IT",
            List.of());

    when(entidad.getId()).thenReturn(id);
    when(entidad.getJuridica()).thenReturn(juridica);
    when(personaMapper.toOutputDTO(juridica)).thenReturn(juridicaDTO);

    // Act
    EntidadBeneficiariaOutputDTO result = mapper.toOutputDTO(entidad);

    // Assert
    assertEquals(id, result.id());
    assertEquals(juridicaDTO, result.juridica());
  }
}
