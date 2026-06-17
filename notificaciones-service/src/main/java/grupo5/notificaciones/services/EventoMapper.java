package grupo5.notificaciones.services;

import grupo5.notificaciones.dto.input.*;
import grupo5.notificaciones.models.entities.notificaciones.eventos.*;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.repositories.PersonaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EventoMapper {
  private final PersonaRepository personaRepository;

  public EventoMapper(PersonaRepository personaRepository) {
    this.personaRepository = personaRepository;
  }

  public List<EventoNotificable> toEntities(EventoNotificableDTO dto) {
    Persona donante = buscarPersona(dto.idPersonaDonante());
    return switch (dto) {
      case EventoDonacionAsignadaDTO don -> {
        Persona beneficiario = buscarPersona(don.idPersonaBeneficiaria());
        yield List.of(
            new DonacionAsignada(donante, beneficiario, don.detalleDonacion(), don.fecha()));
      }
      case EventoDonacionRecibidaDTO rec -> {
        Persona beneficiario = buscarPersona(rec.idPersonaBeneficiaria());
        yield List.of(
            new DonacionRecibida(donante, beneficiario, rec.detalleDonacion(), rec.fecha()));
      }
      case EventoDonanteRegistradoDTO reg -> List.of(
          new DonanteRegistrado(donante, reg.credencialesDeAcceso(), reg.fecha()));
      case EventoDonanteInactivoDTO inac -> List.of(
          new DonanteInactivo(donante, inac.diasInactivo(), inac.fecha()));
      case EventoMisionCumplidaDTO mis -> List.of(
          new MisionCumplida(donante, mis.nombreMision(), mis.recompensa(), mis.fecha()));
      case EventoSubioCategoriaDTO cat -> List.of(
          new SubioCategoria(donante, cat.categoriaVieja(), cat.categoriaNueva(), cat.fecha()));
    };
  }

  private Persona buscarPersona(UUID id) {
    return personaRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada con ID: " + id));
  }
}
