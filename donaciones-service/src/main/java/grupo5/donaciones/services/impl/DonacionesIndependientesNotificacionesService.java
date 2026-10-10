package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.EventoDonacionEnCaminoV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionEntregaFallidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionVencidaV1;
import grupo5.donaciones.infrastructure.outbox.OutboxEntry;
import grupo5.donaciones.infrastructure.outbox.OutboxStore;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionRecibida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionVencida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoRutaIniciada;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import grupo5.donaciones.services.IDonacionesIndependientesNotificacionesService;
import grupo5.donaciones.services.IPersonasService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DonacionesIndependientesNotificacionesService
    implements IDonacionesIndependientesNotificacionesService {

  private static final Logger log =
      LoggerFactory.getLogger(DonacionesIndependientesNotificacionesService.class);

  private final IDonacionesEventPublisher eventPublisher;
  private final IDonacionesRepository donacionRepository;
  private final IDonantesRepository donantesRepository;
  private final IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private final INecesidadesRepository necesidadRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  private final IPersonasService personasService;
  private final OutboxStore outboxStore;

  public DonacionesIndependientesNotificacionesService(
      IDonacionesEventPublisher eventPublisher,
      IDonacionesRepository donacionRepository,
      IDonantesRepository donantesRepository,
      IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository,
      INecesidadesRepository necesidadRepository,
      IDonacionesIndependientesRepository donacionesIndependientesRepository,
      IPersonasService personasService,
      OutboxStore outboxStore) {
    this.eventPublisher = eventPublisher;
    this.donacionRepository = donacionRepository;
    this.donantesRepository = donantesRepository;
    this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
    this.necesidadRepository = necesidadRepository;
    this.donacionesIndependientesRepository = donacionesIndependientesRepository;
    this.personasService = personasService;
    this.outboxStore = outboxStore;
  }

  @Override
  public void procesarRutaIniciada(EventoRutaIniciada event) {
    log.info("Procesando EventoRutaIniciada para donación {}", event.getDonacionIndependienteId());

    UUID donanteId = obtenerDonanteId(event.getDonacionOriginalId());
    UUID personaDonanteId = obtenerPersonaIdDelDonante(donanteId);
    UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(event.getIdNecesidad());
    String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

    var evento =
        new EventoDonacionEnCaminoV1(
            donanteId,
            personaDonanteId,
            event.getTimestamp() != null
                ? event.getTimestamp()
                : LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaBeneficiaria,
            descripcion,
            event.getUrlMapa());

    try {
      eventPublisher.publicarDonacionEnCamino(evento);
    } catch (Exception e) {
      log.warn(
          "Fallo al publicar donacion.en-camino.v1, encolando para reintento: {}", e.getMessage());
      outboxStore.agregar(
          OutboxEntry.nuevo(
              "donaciones.donacionEnCamino[" + event.getDonacionIndependienteId() + "]",
              () -> eventPublisher.publicarDonacionEnCamino(evento)));
    }
  }

  @Override
  public void procesarDonacionRecibida(EventoDonacionRecibida event) {
    log.info(
        "Procesando EventoDonacionRecibida para donación {}", event.getDonacionIndependienteId());

    UUID donanteId = obtenerDonanteId(event.getDonacionOriginalId());
    UUID personaDonanteId = obtenerPersonaIdDelDonante(donanteId);
    UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(event.getIdNecesidad());
    String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

    var evento =
        new EventoDonacionRecibidaV1(
            donanteId,
            personaDonanteId,
            event.getTimestamp() != null
                ? event.getTimestamp()
                : LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaBeneficiaria,
            descripcion,
            event.getPatenteCamion());

    try {
      eventPublisher.publicarDonacionRecibida(evento);
    } catch (Exception e) {
      log.warn(
          "Fallo al publicar donacion.recibida.v1, encolando para reintento: {}", e.getMessage());
      outboxStore.agregar(
          OutboxEntry.nuevo(
              "donaciones.donacionRecibida[" + event.getDonacionIndependienteId() + "]",
              () -> eventPublisher.publicarDonacionRecibida(evento)));
    }
  }

  @Override
  public void procesarDonacionVencida(EventoDonacionVencida event) {
    log.info(
        "Procesando EventoDonacionVencida para donación {}", event.getDonacionIndependienteId());

    UUID donanteId = obtenerDonanteId(event.getDonacionOriginalId());
    UUID personaDonanteId = obtenerPersonaIdDelDonante(donanteId);
    UUID idPersonaAdmin = personasService.obtenerIdPersonaAdministradora();
    String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

    var evento =
        new EventoDonacionVencidaV1(
            donanteId,
            personaDonanteId,
            event.getTimestamp() != null
                ? event.getTimestamp()
                : LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaAdmin,
            descripcion,
            event.getMotivo());

    try {
      eventPublisher.publicarDonacionVencida(evento);
    } catch (Exception e) {
      log.warn(
          "Fallo al publicar donacion.vencida.v1, encolando para reintento: {}", e.getMessage());
      outboxStore.agregar(
          OutboxEntry.nuevo(
              "donaciones.donacionVencida[" + event.getDonacionIndependienteId() + "]",
              () -> eventPublisher.publicarDonacionVencida(evento)));
    }
  }

  @Override
  public void procesarDonacionFallida(EventoDonacionFallida event) {
    log.info(
        "Procesando EventoDonacionFallida para donación {}", event.getDonacionIndependienteId());

    UUID donanteId = obtenerDonanteId(event.getDonacionOriginalId());
    UUID personaDonanteId = obtenerPersonaIdDelDonante(donanteId);
    UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(event.getIdNecesidad());
    UUID idPersonaAdmin = personasService.obtenerIdPersonaAdministradora();
    String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

    var evento =
        new EventoDonacionEntregaFallidaV1(
            donanteId,
            personaDonanteId,
            event.getTimestamp() != null
                ? event.getTimestamp()
                : LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaBeneficiaria,
            descripcion,
            idPersonaAdmin,
            event.getJustificacion(),
            event.getReplanificable());

    try {
      eventPublisher.publicarDonacionEntregaFallida(evento);
    } catch (Exception e) {
      log.warn(
          "Fallo al publicar donacion.entrega-fallida.v1, encolando para reintento: {}",
          e.getMessage());
      outboxStore.agregar(
          OutboxEntry.nuevo(
              "donaciones.donacionFallida[" + event.getDonacionIndependienteId() + "]",
              () -> eventPublisher.publicarDonacionEntregaFallida(evento)));
    }
  }

  private UUID obtenerDonanteId(UUID donacionOriginalId) {
    if (donacionOriginalId == null) return null;
    return donacionRepository.findById(donacionOriginalId).map(Donacion::getDonanteId).orElse(null);
  }

  private UUID obtenerPersonaIdDelDonante(UUID donanteId) {
    if (donanteId == null) return null;
    return donantesRepository.findById(donanteId).map(Donante::personaId).orElse(null);
  }

  private UUID obtenerOrganizacionId(UUID necesidadId) {
    if (necesidadId == null) return null;
    return necesidadRepository.findById(necesidadId).map(Necesidad::getEntidadId).orElse(null);
  }

  private UUID obtenerPersonaBeneficiariaId(UUID necesidadId) {
    UUID entidadId = obtenerOrganizacionId(necesidadId);
    if (entidadId == null) return null;
    return entidadesBeneficiariasRepository
        .findById(entidadId)
        .map(EntidadBeneficiaria::juridicaId)
        .orElse(null);
  }

  private String obtenerDescripcionDonacion(UUID donacionIndependienteId) {
    if (donacionIndependienteId == null) return "";
    return donacionesIndependientesRepository
        .findById(donacionIndependienteId)
        .map(DonacionIndependiente::getDescripcion)
        .orElse("");
  }
}
