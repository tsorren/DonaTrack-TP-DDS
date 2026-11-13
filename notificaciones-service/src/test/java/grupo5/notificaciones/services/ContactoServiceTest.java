package grupo5.notificaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.config.AdminConstantes;
import grupo5.notificaciones.dto.contacto.MensajeContactoDTO;
import grupo5.notificaciones.dto.contacto.MensajeContactoInputDTO;
import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import grupo5.notificaciones.models.entities.contacto.MensajeContacto;
import grupo5.notificaciones.models.entities.contacto.TemaContacto;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.IMensajeContactoRepository;
import grupo5.notificaciones.models.repositories.impl.MensajeContactoRepositoryEnMemoria;
import grupo5.notificaciones.services.impl.ContactoService;
import grupo5.notificaciones.services.impl.NotificacionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ContactoServiceTest {

  private static final String IP = "203.0.113.7";

  private MensajeContactoRepositoryEnMemoria repository;
  private NotificacionService notificacionService;
  private NotificacionSender sender;
  private ContactoService service;

  @BeforeEach
  void setUp() {
    repository = new MensajeContactoRepositoryEnMemoria();
    notificacionService = mock(NotificacionService.class);
    sender = mock(NotificacionSender.class);
    service = new ContactoService(repository, notificacionService, sender, 5, 20);
  }

  private static MensajeContactoInputDTO mensaje(String correo) {
    return mensaje(correo, UUID.randomUUID());
  }

  private static MensajeContactoInputDTO mensaje(String correo, UUID clave) {
    return new MensajeContactoInputDTO(
        TemaContacto.ENTREGA, "No llegó la entrega", "ERR-EST-804", correo, clave);
  }

  @Test
  void recibir_laMismaClaveDosVeces_devuelveElMismoNumeroYGuardaUnSoloMensaje() {
    UUID clave = UUID.randomUUID();

    ContactoService.Recepcion primera = service.recibir(mensaje("ana@ejemplo.org", clave), IP);
    ContactoService.Recepcion segunda = service.recibir(mensaje("ana@ejemplo.org", clave), IP);

    assertTrue(primera.nuevo());
    assertFalse(segunda.nuevo());
    assertEquals(primera.numero(), segunda.numero());
    assertEquals(1, repository.contar(null));
    verify(notificacionService, times(1)).notificarPersona(any(), anyString());
  }

  @Test
  void recibir_elSextoMensajeEnUnaHoraDelMismoCorreo_daLimiteExcedido() {
    for (int i = 0; i < 5; i++) {
      service.recibir(mensaje("ana@ejemplo.org"), IP);
    }

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class, () -> service.recibir(mensaje("ana@ejemplo.org"), IP));
    assertEquals(ErrorCatalog.CONTACTO_LIMITE_EXCEDIDO, ex.getError());
    assertEquals(5, repository.contar(null));
  }

  @Test
  void recibir_elLimitePorCorreoNoDistingueMayusculasNiEspacios() {
    for (int i = 0; i < 5; i++) {
      service.recibir(mensaje("ana@ejemplo.org"), IP);
    }

    assertThrows(
        BusinessStateException.class, () -> service.recibir(mensaje("  ANA@Ejemplo.org "), IP));
  }

  @Test
  void recibir_conElCorreoEnElLimite_unaClaveRepetidaDevuelveElNumeroDelPrimero() {
    UUID clave = UUID.randomUUID();
    long primero = service.recibir(mensaje("ana@ejemplo.org", clave), IP).numero();
    for (int i = 0; i < 4; i++) {
      service.recibir(mensaje("ana@ejemplo.org"), IP);
    }

    ContactoService.Recepcion repetida = service.recibir(mensaje("ana@ejemplo.org", clave), IP);

    assertFalse(repetida.nuevo());
    assertEquals(primero, repetida.numero());
  }

  @Test
  void recibir_elMensaje21DelDiaDeLaMismaIp_daLimiteExcedido() {
    for (int i = 0; i < 20; i++) {
      service.recibir(mensaje("persona" + i + "@ejemplo.org"), IP + ", 10.0.0.5");
    }

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class, () -> service.recibir(mensaje("otra@ejemplo.org"), IP));
    assertEquals(ErrorCatalog.CONTACTO_LIMITE_EXCEDIDO, ex.getError());
  }

  @Test
  void recibir_sinIp_soloCuentaElLimitePorCorreo() {
    for (int i = 0; i < 25; i++) {
      service.recibir(mensaje("persona" + i + "@ejemplo.org"), null);
    }

    assertEquals(25, repository.contar(null));
  }

  @Test
  void recibir_guardaElCorreoNormalizadoYLaPrimeraIpDeXForwardedFor() {
    long numero = service.recibir(mensaje(" Ana@Ejemplo.ORG"), " 203.0.113.7 , 10.0.0.5").numero();

    MensajeContacto guardado = repository.buscarPorNumero(numero).orElseThrow();
    assertEquals("ana@ejemplo.org", guardado.getCorreo());
    assertEquals("203.0.113.7", guardado.getIpOrigen());
    assertEquals(EstadoMensajeContacto.RECIBIDO, guardado.getEstado());
  }

  @Test
  void recibir_unMensajeNuevoAvisaALaAdministracionConElNumeroYElTema() {
    long numero = service.recibir(mensaje("ana@ejemplo.org"), IP).numero();

    verify(notificacionService)
        .notificarPersona(
            AdminConstantes.ID_ADMIN,
            "Nuevo mensaje de contacto Nº " + numero + ", sobre una entrega.");
  }

  @Test
  void recibir_siOtroPedidoGuardoLaMismaClaveAntes_devuelveElNumeroDeEse() {
    IMensajeContactoRepository repo = mock(IMensajeContactoRepository.class);
    UUID clave = UUID.randomUUID();
    MensajeContacto ganador =
        new MensajeContacto(
            42L,
            clave,
            TemaContacto.OTRO,
            "Hola",
            null,
            "ana@ejemplo.org",
            null,
            LocalDateTime.now(),
            EstadoMensajeContacto.RECIBIDO,
            null,
            null);
    when(repo.buscarPorClaveEnvio(clave)).thenReturn(Optional.empty(), Optional.of(ganador));
    when(repo.guardar(any())).thenThrow(new DataIntegrityViolationException("clave"));
    ContactoService conCarrera = new ContactoService(repo, notificacionService, sender, 5, 20);

    ContactoService.Recepcion recepcion = conCarrera.recibir(mensaje("ana@ejemplo.org", clave), IP);

    assertEquals(42L, recepcion.numero());
    assertFalse(recepcion.nuevo());
    verify(notificacionService, never()).notificarPersona(any(), anyString());
  }

  @Test
  void ipDeOrigen_tomaLaPrimeraRecortadaYAcotaElLargo() {
    assertNull(ContactoService.ipDeOrigen(null));
    assertNull(ContactoService.ipDeOrigen(" , 10.0.0.1"));
    assertEquals("203.0.113.7", ContactoService.ipDeOrigen("203.0.113.7,10.0.0.1"));
    assertEquals(45, ContactoService.ipDeOrigen("x".repeat(500)).length());
  }

  @Test
  void listar_devuelveDelMasNuevoAlMasViejoYFiltraPorEstado() {
    long primero = service.recibir(mensaje("a@ejemplo.org"), IP).numero();
    long segundo = service.recibir(mensaje("b@ejemplo.org"), IP).numero();
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(true);
    service.responder(primero, "Listo");

    ContactoService.Pagina todos = service.listar(null, 0);
    ContactoService.Pagina recibidos = service.listar(EstadoMensajeContacto.RECIBIDO, 0);

    assertEquals(
        List.of(segundo, primero), todos.mensajes().stream().map(m -> m.numero()).toList());
    assertEquals(2, todos.total());
    assertEquals(1, recibidos.total());
    assertEquals(segundo, recibidos.mensajes().getFirst().numero());
  }

  @Test
  void listar_conPaginaNegativa_daArgumentoInvalido() {
    assertThrows(ValidationException.class, () -> service.listar(null, -1));
  }

  @Test
  void responder_cambiaElEstadoARespondidoYMandaAlCorreoDelMensaje() {
    long numero = service.recibir(mensaje("ana@ejemplo.org"), IP).numero();
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(true);

    MensajeContactoDTO respondido = service.responder(numero, "Ya la reprogramamos.").orElseThrow();

    assertEquals("RESPONDIDO", respondido.estado());
    assertEquals("Ya la reprogramamos.", respondido.respuesta());
    verify(sender)
        .enviarA(
            argThat((Correo c) -> "ana@ejemplo.org".equals(c.getDireccionCorreo())),
            eq("Respuesta a tu mensaje Nº " + numero + "\n\nYa la reprogramamos."));
  }

  @Test
  void responder_siFallaElProveedor_quedaEnvioFallido() {
    long numero = service.recibir(mensaje("ana@ejemplo.org"), IP).numero();
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(false);

    MensajeContactoDTO respondido = service.responder(numero, "Respuesta").orElseThrow();

    assertEquals("ENVIO_FALLIDO", respondido.estado());
    assertEquals(
        EstadoMensajeContacto.ENVIO_FALLIDO,
        repository.buscarPorNumero(numero).orElseThrow().getEstado());
  }

  @Test
  void responder_yObtener_conNumeroInexistente_devuelvenVacio() {
    assertTrue(service.responder(999, "Hola").isEmpty());
    assertTrue(service.obtener(999).isEmpty());
  }
}
