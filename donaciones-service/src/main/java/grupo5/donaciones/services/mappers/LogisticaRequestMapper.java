package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.comunicaciones.NuevaEntregaRequest;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.personas.Persona;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogisticaRequestMapper {

  private final DireccionMapper direccionMapper;

  public NuevaEntregaRequest toRequest(
      DonacionIndependiente donacion, EntidadBeneficiaria entidad, Persona persona) {
    return new NuevaEntregaRequest(
        donacion.getId(),
        entidad.getId(),
        direccionMapper.toOutputDTO(persona.getDireccion()),
        donacion.getPesoTotal(),
        donacion.getVolumenTotal());
  }
}
