package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.DonacionExitosaRequest;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaDTO;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionVencidaDTO;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallidaDTO;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciadaDTO;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
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

  private final IncentivosFeignClient incentivosFeignClient;
  private final NotificacionesFeignClient notificacionesFeignClient;
  private final IDonacionesRepository donacionRepository;
  private final IDonantesRepository donantesRepository;
  private final IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private final INecesidadesRepository necesidadRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  private final IPersonasService personasService;
  private final OutboxStore outboxStore;

  public DonacionesIndependientesNotificacionesService(
      IncentivosFeignClient incentivosFeignClient,
      NotificacionesFeignClient notificacionesFeignClient,
      IDonacionesRepository donacionRepository,
      IDonantesRepository donantesRepository,
      IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository,
      INecesidadesRepository necesidadRepository,
      IDonacionesIndependientesRepository donacionesIndependientesRepository,
      IPersonasService personasService,
      OutboxStore outboxStore) {
    this.incentivosFeignClient = incentivosFeignClient;
    this.notificacionesFeignClient = notificacionesFeignClient;
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

    UUID personaDonanteId = obtenerPersonaDonanteId(event.getDonacionOriginalId());
    UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(event.getIdNecesidad());
    String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

    var dto =
        new EventoRutaIniciadaDTO(
            personaDonanteId,
            event.getTimestamp() != null
                ? event.getTimestamp()
                : LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaBeneficiaria,
            descripcion,
            event.getUrlMapa());

    try {
      notificacionesFeignClient.enviarEvento(dto);
    } catch (Exception e) {
      log.warn("Fallo al notificar ruta iniciada, encolando para reintento: {}", e.getMessage());
      outboxStore.agregar(
          OutboxEntry.nuevo(
              "notificaciones.rutaIniciada[" + event.getDonacionIndependienteId() + "]",
              () -> notificacionesFeignClient.enviarEvento(dto)));
    }
  }

  @Override
  public void procesarDonacionRecibida(EventoDonacionRecibida event) {
    log.info(
        "Procesando EventoDonacionRecibida para donación {}", event.getDonacionIndependienteId());

    UUID donanteId = obtenerDonanteId(event.getDonacionOriginalId());
    UUID personaDonanteId = obtenerPersonaDonanteId(event.getDonacionOriginalId());
    UUID organizacionId = obtenerOrganizacionId(event.getIdNecesidad());
    UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(event.getIdNecesidad());
    String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

    var dtoIncentivos = new DonacionExitosaRequest(donanteId, organizacionId);
    try {
      incentivosFeignClient.procesarDonacionExitosa(dtoIncentivos);
    } catch (Exception e) {
      log.warn(
          "Fallo al registrar incentivos para donación recibida, encolando para reintento: {}",
          e.getMessage());
      outboxStore.agregar(
          OutboxEntry.nuevo(
              "incentivos.procesarDonacionExitosa[" + event.getDonacionIndependienteId() + "]",
              () -> incentivosFeignClient.procesarDonacionExitosa(dtoIncentivos)));
    }

    var dtoNotificaciones =
        new EventoDonacionRecibidaDTO(
            personaDonanteId,
            event.getTimestamp() != null
                ? event.getTimestamp()
                : LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaBeneficiaria,
            descripcion,
            event.getPatenteCamion());
    try {
      notificacionesFeignClient.enviarEvento(dtoNotificaciones);
    } catch (Exception e) {
      log.warn(
          "Fallo al notificar donación recibida, encolando para reintento: {}", e.getMessage());
      outboxStore.agregar(
          OutboxEntry.nuevo(
              "notificaciones.donacionRecibida[" + event.getDonacionIndependienteId() + "]",
              () -> notificacionesFeignClient.enviarEvento(dtoNotificaciones)));
    }
  }

  @Override
  public void procesarDonacionVencida(EventoDonacionVencida event) {
    log.info(
        "Procesando EventoDonacionVencida para donación {}", event.getDonacionIndependienteId());

    UUID personaDonanteId = obtenerPersonaDonanteId(event.getDonacionOriginalId());
    UUID idPersonaAdmin = personasService.obtenerIdPersonaAdministradora();
    String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

    var dto =
        new EventoDonacionVencidaDTO(
            personaDonanteId,
            event.getTimestamp() != null
                ? event.getTimestamp()
                : LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaAdmin,
            descripcion,
            event.getMotivo());

    try {
      notificacionesFeignClient.enviarEvento(dto);
    } catch (Exception e) {
      log.warn("Fallo al notificar donación vencida, encolando para reintento: {}", e.getMessage());
      outboxStore.agregar(
          OutboxEntry.nuevo(
              "notificaciones.donacionVencida[" + event.getDonacionIndependienteId() + "]",
              () -> notificacionesFeignClient.enviarEvento(dto)));
    }
  }

  @Override
  public void procesarDonacionFallida(EventoDonacionFallida event) {
    log.info(
        "Procesando EventoDonacionFallida para donación {}", event.getDonacionIndependienteId());

    UUID personaDonanteId = obtenerPersonaDonanteId(event.getDonacionOriginalId());
    UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(event.getIdNecesidad());
    UUID idPersonaAdmin = personasService.obtenerIdPersonaAdministradora();
    String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

    var dto =
        new EventoEntregaFallidaDTO(
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
      notificacionesFeignClient.enviarEvento(dto);
    } catch (Exception e) {
      log.warn("Fallo al notificar entrega fallida, encolando para reintento: {}", e.getMessage());
      outboxStore.agregar(
          OutboxEntry.nuevo(
              "notificaciones.donacionFallida[" + event.getDonacionIndependienteId() + "]",
              () -> notificacionesFeignClient.enviarEvento(dto)));
    }
  }

  private UUID obtenerDonanteId(UUID donacionOriginalId) {
    if (donacionOriginalId == null) return null;
    return donacionRepository.findById(donacionOriginalId).map(Donacion::getDonanteId).orElse(null);
  }

  private UUID obtenerPersonaDonanteId(UUID donacionOriginalId) {
    UUID donanteId = obtenerDonanteId(donacionOriginalId);
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
