package grupo5.notificaciones.services.mappers;

import grupo5.notificaciones.dto.input.*;
import grupo5.notificaciones.models.entities.notificaciones.eventos.*;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EventoMapper {
  private final IPersonaRepository personaRepository;

  public EventoMapper(IPersonaRepository personaRepository) {
    this.personaRepository = personaRepository;
  }

  public EventoNotificable toEntity(EventoNotificableDTO dto) {
    Persona donante = buscarPersona(dto.idPersonaDonante());
    return switch (dto) {
      case EventoDonacionAsignadaDTO don -> {
        Persona beneficiario = buscarPersona(don.idPersonaBeneficiaria());
        yield new DonacionAsignada(donante, beneficiario, don.detalleDonacion(), don.fecha());
      }
      case EventoDonacionRecibidaDTO rec -> {
        Persona beneficiario = buscarPersona(rec.idPersonaBeneficiaria());
        yield new DonacionRecibida(
            donante, beneficiario, rec.detalleDonacion(), rec.patenteCamion(), rec.fecha());
      }
      case EventoDonanteRegistradoDTO reg -> new DonanteRegistrado(
          donante, reg.credencialesDeAcceso(), reg.fecha());
      case EventoDonanteInactivoDTO inac -> new DonanteInactivo(
          donante, inac.diasInactivo(), inac.fecha());
      case EventoMisionCumplidaDTO mis -> new MisionCumplida(
          donante, mis.nombreMision(), mis.recompensa(), mis.fecha());
      case EventoSubioCategoriaDTO cat -> new SubioCategoria(
          donante, cat.categoriaVieja(), cat.categoriaNueva(), cat.fecha());
      case EventoDonacionEnCaminoDTO dec -> {
        Persona beneficiario = buscarPersona(dec.idPersonaBeneficiaria());
        yield new DonacionEnCamino(
            donante, beneficiario, dec.detalleDonacion(), dec.enlaceSeguimiento(), dec.fecha());
      }
      case EventoEntregaFallidaDTO ef -> {
        Persona beneficiario = buscarPersona(ef.idPersonaBeneficiaria());
        Persona admin = buscarPersona(ef.idPersonaAdmin());
        yield new EntregaFallida(
            donante,
            beneficiario,
            admin,
            ef.detalleDonacion(),
            ef.motivo(),
            ef.replanificable(),
            ef.fecha());
      }
    };
  }

  private Persona buscarPersona(UUID id) {
    return personaRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada con ID: " + id));
  }
}
