package grupo5.notificaciones.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.input.DestinoEventoDTO;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaDTO;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaV1;
import grupo5.notificaciones.dto.input.EventoDonacionEnCaminoDTO;
import grupo5.notificaciones.dto.input.EventoDonacionEnCaminoV1;
import grupo5.notificaciones.dto.input.EventoDonacionEntregaFallidaV1;
import grupo5.notificaciones.dto.input.EventoDonacionRecibidaDTO;
import grupo5.notificaciones.dto.input.EventoDonacionRecibidaV1;
import grupo5.notificaciones.dto.input.EventoDonacionVencidaDTO;
import grupo5.notificaciones.dto.input.EventoDonacionVencidaV1;
import grupo5.notificaciones.dto.input.EventoDonanteInactivoDTO;
import grupo5.notificaciones.dto.input.EventoDonanteRegistradoDTO;
import grupo5.notificaciones.dto.input.EventoDonanteRegistradoV1;
import grupo5.notificaciones.dto.input.EventoEntregaFallidaDTO;
import grupo5.notificaciones.dto.input.EventoIncentivoDonanteInactivoV1;
import grupo5.notificaciones.dto.input.EventoIncentivoMisionCumplidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoSubioCategoriaV1;
import grupo5.notificaciones.dto.input.EventoMisionCumplidaDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.dto.input.EventoSubioCategoriaDTO;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonacionAsignada;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonacionEnCamino;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonacionRecibida;
import grupo5.notificaciones.models.entities.notificaciones.eventos.DonacionVencida;
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

  public DonacionAsignada toEntity(EventoDonacionAsignadaV1 evento) {
    Persona donante = buscarPersona(evento.personaId());
    Persona beneficiario = buscarPersona(evento.personaBeneficiariaId());
    return new DonacionAsignada(
        donante, beneficiario, construirDetalleDonacion(evento), evento.fecha());
  }

  public DonanteRegistrado toEntity(EventoDonanteRegistradoV1 evento) {
    Persona donante = buscarPersona(evento.personaId());
    return new DonanteRegistrado(donante, evento.credencialesDeAcceso(), evento.fecha());
  }

  public DonacionEnCamino toEntity(EventoDonacionEnCaminoV1 evento) {
    Persona donante = buscarPersona(evento.personaId());
    Persona beneficiario = buscarPersona(evento.personaBeneficiariaId());
    return new DonacionEnCamino(
        donante, beneficiario, evento.descripcion(), evento.urlMapa(), evento.fecha());
  }

  public DonacionRecibida toEntity(EventoDonacionRecibidaV1 evento) {
    Persona donante = buscarPersona(evento.personaId());
    Persona beneficiario = buscarPersona(evento.personaBeneficiariaId());
    return new DonacionRecibida(
        donante, beneficiario, evento.descripcion(), evento.patenteCamion(), evento.fecha());
  }

  public EntregaFallida toEntity(EventoDonacionEntregaFallidaV1 evento) {
    Persona donante = buscarPersona(evento.personaId());
    Persona beneficiario = buscarPersona(evento.personaBeneficiariaId());
    Persona admin = buscarPersona(evento.personaAdminId());
    return new EntregaFallida(
        donante,
        beneficiario,
        admin,
        evento.descripcion(),
        evento.justificacion(),
        evento.replanificable(),
        evento.fecha());
  }

  public DonacionVencida toEntity(EventoDonacionVencidaV1 evento) {
    Persona donante = buscarPersona(evento.personaId());
    Persona admin = buscarPersona(evento.personaAdminId());
    return new DonacionVencida(
        donante, admin, evento.descripcion(), evento.motivo(), evento.fecha());
  }

  public DonanteInactivo toEntity(EventoIncentivoDonanteInactivoV1 evento) {
    Persona donante = buscarPersona(evento.personaDonanteId());
    return new DonanteInactivo(donante, evento.diasInactividad(), evento.fecha());
  }

  public MisionCumplida toEntity(EventoIncentivoMisionCumplidaV1 evento) {
    Persona donante = buscarPersona(evento.personaDonanteId());
    return new MisionCumplida(donante, evento.nombreMision(), evento.recompensa(), evento.fecha());
  }

  public SubioCategoria toEntity(EventoIncentivoSubioCategoriaV1 evento) {
    Persona donante = buscarPersona(evento.personaDonanteId());
    return new SubioCategoria(
        donante, evento.nombreViejaCategoria(), evento.nombreNuevaCategoria(), evento.fecha());
  }

  public String construirDetalleDonacion(EventoDonacionAsignadaV1 evento) {
    return "%s (ID: %s, Peso: %s kg, Volumen: %s m³, Destino: %s)"
        .formatted(
            evento.descripcion() != null ? evento.descripcion() : "Donación",
            evento.donacionIndependienteId(),
            evento.pesoTotalKG(),
            evento.volumenTotalM3(),
            formatearDestino(evento.destino()));
  }

  private static String formatearDestino(DestinoEventoDTO d) {
    if (d == null) {
      return "Sin destino especificado";
    }
    String pisoDpto =
        (d.piso() != null ? ", Piso " + d.piso() : "")
            + (d.departamento() != null && !d.departamento().isBlank()
                ? ", Dpto " + d.departamento()
                : "");

    return "%s %d%s, %s, %s (CP %s), %s"
        .formatted(
            d.calle(),
            d.altura(),
            pisoDpto,
            d.localidad(),
            d.provincia(),
            d.codigoPostal(),
            d.pais());
  }

  public EventoNotificable toEntity(EventoNotificableDTO dto) {
    Persona donante = buscarPersona(dto.personaId());
    return switch (dto) {
      case EventoDonacionAsignadaDTO don -> {
        Persona beneficiario = buscarPersona(don.personaBeneficiariaId());
        yield new DonacionAsignada(donante, beneficiario, don.descripcion(), don.fecha());
      }
      case EventoDonacionRecibidaDTO rec -> {
        Persona beneficiario = buscarPersona(rec.personaBeneficiariaId());
        yield new DonacionRecibida(
            donante, beneficiario, rec.descripcion(), rec.patenteCamion(), rec.fecha());
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
        Persona beneficiario = buscarPersona(dec.personaBeneficiariaId());
        yield new DonacionEnCamino(
            donante, beneficiario, dec.descripcion(), dec.urlMapa(), dec.fecha());
      }
      case EventoEntregaFallidaDTO ef -> {
        Persona beneficiario = buscarPersona(ef.personaBeneficiariaId());
        Persona admin = buscarPersona(ef.personaAdminId());
        yield new EntregaFallida(
            donante,
            beneficiario,
            admin,
            ef.descripcion(),
            ef.justificacion(),
            ef.replanificable(),
            ef.fecha());
      }
      case EventoDonacionVencidaDTO dv -> {
        Persona admin = buscarPersona(dv.personaAdminId());
        yield new DonacionVencida(donante, admin, dv.descripcion(), dv.motivo(), dv.fecha());
      }
    };
  }

  private Persona buscarPersona(UUID id) {
    if (id == null) {
      throw new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO);
    }
    return personaRepository
        .findById(id)
        .orElseThrow(() -> new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO));
  }
}
