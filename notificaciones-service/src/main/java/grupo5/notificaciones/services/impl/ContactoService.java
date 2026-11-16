package grupo5.notificaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.config.AdminConstantes;
import grupo5.notificaciones.dto.contacto.MensajeContactoDTO;
import grupo5.notificaciones.dto.contacto.MensajeContactoInputDTO;
import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import grupo5.notificaciones.models.entities.contacto.MensajeContacto;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.IMensajeContactoRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Mensajes del formulario de contacto (HB-21, ADR proposed 20261110). Orden de los chequeos: la
 * clave de envío, después los límites y recién entonces se guarda. Sin {@code @Transactional}: si
 * dos pedidos con la misma clave se cruzan, el segundo choca con el UNIQUE y relee el primero.
 */
@Service
public class ContactoService {
  static final int TAMANIO_PAGINA = 20;
  private static final int LARGO_MAXIMO_IP = 45;
  private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Argentina/Buenos_Aires");

  private final IMensajeContactoRepository repository;
  private final NotificacionService notificacionService;
  private final NotificacionSender sender;
  private final int limitePorHoraPorCorreo;
  private final int limitePorDiaPorIp;

  public ContactoService(
      IMensajeContactoRepository repository,
      NotificacionService notificacionService,
      NotificacionSender sender,
      @Value("${donatrack.contacto.limite-por-hora-por-correo:5}") int limitePorHoraPorCorreo,
      @Value("${donatrack.contacto.limite-por-dia-por-ip:20}") int limitePorDiaPorIp) {
    this.repository = repository;
    this.notificacionService = notificacionService;
    this.sender = sender;
    this.limitePorHoraPorCorreo = limitePorHoraPorCorreo;
    this.limitePorDiaPorIp = limitePorDiaPorIp;
  }

  /** {@code nuevo} es false cuando la clave ya estaba: el número es el del primer mensaje. */
  public record Recepcion(long numero, boolean nuevo) {}

  public record Pagina(List<MensajeContactoDTO> mensajes, long total) {}

  public Recepcion recibir(MensajeContactoInputDTO dto, @Nullable String xForwardedFor) {
    Optional<MensajeContacto> previo = repository.buscarPorClaveEnvio(dto.claveEnvio());
    if (previo.isPresent()) {
      return new Recepcion(previo.get().getNumero(), false);
    }
    String correo = dto.correo().trim().toLowerCase(Locale.ROOT);
    String ip = ipDeOrigen(xForwardedFor);
    LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);
    if (repository.contarPorCorreoDesde(correo, ahora.minusHours(1)) >= limitePorHoraPorCorreo
        || (ip != null
            && repository.contarPorIpDesde(ip, ahora.minusDays(1)) >= limitePorDiaPorIp)) {
      throw new BusinessStateException(ErrorCatalog.CONTACTO_LIMITE_EXCEDIDO);
    }
    MensajeContacto guardado;
    try {
      guardado =
          repository.guardar(
              MensajeContacto.recibido(
                  dto.claveEnvio(),
                  dto.tema(),
                  dto.texto(),
                  vacioANulo(dto.codigoError()),
                  correo,
                  ip,
                  ahora));
    } catch (DataIntegrityViolationException e) {
      return repository
          .buscarPorClaveEnvio(dto.claveEnvio())
          .map(m -> new Recepcion(m.getNumero(), false))
          .orElseThrow(() -> e);
    }
    // Solo el número y el tema: el correo y el texto no salen del mensaje.
    notificacionService.notificarPersona(
        AdminConstantes.ID_ADMIN,
        "Nuevo mensaje de contacto Nº "
            + guardado.getNumero()
            + ", sobre "
            + guardado.getTema().getDescripcion()
            + ".");
    return new Recepcion(guardado.getNumero(), true);
  }

  public Pagina listar(@Nullable EstadoMensajeContacto estado, int pagina) {
    if (pagina < 0) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    List<MensajeContactoDTO> mensajes =
        repository.listar(estado, pagina, TAMANIO_PAGINA).stream()
            .map(ContactoService::toDTO)
            .toList();
    return new Pagina(mensajes, repository.contar(estado));
  }

  public Optional<MensajeContactoDTO> obtener(long numero) {
    return repository.buscarPorNumero(numero).map(ContactoService::toDTO);
  }

  public Optional<MensajeContactoDTO> responder(long numero, String texto) {
    return repository
        .buscarPorNumero(numero)
        .map(
            mensaje -> {
              mensaje.responder(texto, sender);
              return toDTO(repository.guardar(mensaje));
            });
  }

  /**
   * La IP del navegador es la primera de {@code X-Forwarded-For}: el gateway suma la suya detrás.
   * El encabezado lo puede inventar quien llame directo, así que se recorta al largo de la columna.
   */
  @Nullable
  public static String ipDeOrigen(@Nullable String xForwardedFor) {
    if (xForwardedFor == null) {
      return null;
    }
    String primera = xForwardedFor.split(",", 2)[0].trim();
    if (primera.isEmpty()) {
      return null;
    }
    return primera.length() > LARGO_MAXIMO_IP ? primera.substring(0, LARGO_MAXIMO_IP) : primera;
  }

  @Nullable
  private static String vacioANulo(@Nullable String valor) {
    return valor == null || valor.isBlank() ? null : valor.trim();
  }

  private static MensajeContactoDTO toDTO(MensajeContacto m) {
    return new MensajeContactoDTO(
        m.getNumero(),
        m.getTema().name(),
        m.getTexto(),
        m.getCodigoError(),
        m.getCorreo(),
        m.getEstado().name(),
        m.getRecibidoEn(),
        m.getRespuesta(),
        m.getRespondidoEn());
  }
}
