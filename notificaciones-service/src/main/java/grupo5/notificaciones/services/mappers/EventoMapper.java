package grupo5.notificaciones.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaDTO;
import grupo5.notificaciones.dto.input.EventoDonacionEnCaminoDTO;
import grupo5.notificaciones.dto.input.EventoDonacionRecibidaDTO;
import grupo5.notificaciones.dto.input.EventoDonanteInactivoDTO;
import grupo5.notificaciones.dto.input.EventoDonanteRegistradoDTO;
import grupo5.notificaciones.dto.input.EventoEntregaFallidaDTO;
import grupo5.notificaciones.dto.input.EventoMisionCumplidaDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.dto.input.EventoSubioCategoriaDTO;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonacionAsignada;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonacionEnCamino;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonacionRecibida;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonanteInactivo;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonanteRegistrado;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EntregaFallida;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.entities.notificaciones.eventos.MisionCumplida;
import grupo5.notificaciones.models.entities.notificaciones.eventos.SubioCategoria;
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
      case EventoDonanteRegistradoDTO reg ->
          new DonanteRegistrado(donante, reg.credencialesDeAcceso(), reg.fecha());
      case EventoDonanteInactivoDTO inac ->
          new DonanteInactivo(donante, inac.diasInactivo(), inac.fecha());
      case EventoMisionCumplidaDTO mis ->
          new MisionCumplida(donante, mis.nombreMision(), mis.recompensa(), mis.fecha());
      case EventoSubioCategoriaDTO cat ->
          new SubioCategoria(donante, cat.categoriaVieja(), cat.categoriaNueva(), cat.fecha());
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
    // Mismo criterio que PersonasService.obtenerPersona/anonimizar: ValidationException +
    // RECURSO_NO_ENCONTRADO genérico, no RecursoNoEncontradoException — se mantiene consistente
    // con el resto del servicio en vez de introducir un segundo criterio para el mismo caso.
    return personaRepository
        .findById(id)
        .orElseThrow(() -> new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO));
  }
}
