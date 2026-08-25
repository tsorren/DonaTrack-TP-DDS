package grupo5.donaciones.infrastructure.events;

import grupo5.donaciones.dto.comunicaciones.DonacionExitosaRequest;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaDTO;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallidaDTO;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciadaDTO;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionRecibida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoRutaIniciada;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.IPersonasService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DonacionIndependienteNotificacionesListener {

  private static final Logger log =
      LoggerFactory.getLogger(DonacionIndependienteNotificacionesListener.class);

  private final IncentivosFeignClient incentivosFeignClient;
  private final NotificacionesFeignClient notificacionesFeignClient;
  private final IDonacionesRepository donacionRepository;
  private final IDonantesRepository donantesRepository;
  private final IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private final INecesidadesRepository necesidadRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  private final IPersonasService personasService;

  @EventListener
  public void onEventoRutaIniciada(EventoRutaIniciada event) {
    log.info("Procesando EventoRutaIniciada para donación {}", event.getDonacionIndependienteId());
    try {
      UUID personaDonanteId = obtenerPersonaDonanteId(event.getDonacionOriginalId());
      UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(event.getIdNecesidad());
      String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

      notificacionesFeignClient.enviarEvento(
          new EventoRutaIniciadaDTO(
              personaDonanteId,
              event.getTimestamp() != null
                  ? event.getTimestamp()
                  : LocalDateTime.now(ZoneId.systemDefault()),
              idPersonaBeneficiaria,
              descripcion,
              event.getUrlMapa()));
    } catch (Exception e) {
      log.error("Error al notificar ruta iniciada: {}", e.getMessage(), e);
    }
  }

  @EventListener
  public void onEventoDonacionRecibida(EventoDonacionRecibida event) {
    log.info(
        "Procesando EventoDonacionRecibida para donación {}", event.getDonacionIndependienteId());
    try {
      UUID donanteId = obtenerDonanteId(event.getDonacionOriginalId());
      UUID personaDonanteId = obtenerPersonaDonanteId(event.getDonacionOriginalId());
      UUID organizacionId = obtenerOrganizacionId(event.getIdNecesidad());
      UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(event.getIdNecesidad());
      String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

      incentivosFeignClient.procesarDonacionExitosa(
          new DonacionExitosaRequest(donanteId, organizacionId));

      notificacionesFeignClient.enviarEvento(
          new EventoDonacionRecibidaDTO(
              personaDonanteId,
              event.getTimestamp() != null
                  ? event.getTimestamp()
                  : LocalDateTime.now(ZoneId.systemDefault()),
              idPersonaBeneficiaria,
              descripcion,
              event.getPatenteCamion()));
    } catch (Exception e) {
      log.error("Error al procesar donación recibida: {}", e.getMessage(), e);
    }
  }

  @EventListener
  public void onEventoDonacionFallida(EventoDonacionFallida event) {
    log.info(
        "Procesando EventoDonacionFallida para donación {}", event.getDonacionIndependienteId());
    try {
      UUID personaDonanteId = obtenerPersonaDonanteId(event.getDonacionOriginalId());
      UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(event.getIdNecesidad());
      UUID idPersonaAdmin = personasService.obtenerIdPersonaAdministradora();
      String descripcion = obtenerDescripcionDonacion(event.getDonacionIndependienteId());

      notificacionesFeignClient.enviarEvento(
          new EventoEntregaFallidaDTO(
              personaDonanteId,
              event.getTimestamp() != null
                  ? event.getTimestamp()
                  : LocalDateTime.now(ZoneId.systemDefault()),
              idPersonaBeneficiaria,
              descripcion,
              idPersonaAdmin,
              event.getJustificacion(),
              event.getReplanificable()));
    } catch (Exception e) {
      log.error("Error al notificar entrega fallida: {}", e.getMessage(), e);
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
