package grupo5.incentivos.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.dto.events.EventoIncentivoDonanteInactivoV1;
import grupo5.incentivos.dto.events.EventoIncentivoMisionCumplidaV1;
import grupo5.incentivos.dto.events.EventoIncentivoSubioCategoriaV1;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.EventoDonanteIncentivos;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.outbox.EntradaOutbox;
import grupo5.incentivos.models.outbox.EstadoOutbox;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventosSalientesTest {

  private static final LocalDateTime AHORA = LocalDateTime.of(2026, Month.OCTOBER, 27, 12, 0, 0);

  @Test
  void aEntrada_conMisionCompletadaConInsignia_usaElNombreDeLaInsigniaComoRecompensa() {
    UUID persona = UUID.randomUUID();
    Insignia insignia = new Insignia("Gran Donador", "Por donar mucho", "/icon.png");
    EventoDonanteIncentivos evento =
        new MisionCompletada(UUID.randomUUID(), persona, "Carlos", "Misión 1", insignia);

    EntradaOutbox entrada = EventosSalientes.aEntrada(evento, AHORA, 7).orElseThrow();

    assertEquals(RabbitMQConfig.ROUTING_KEY_MISION_CUMPLIDA, entrada.routingKey());
    assertEquals(EstadoOutbox.PENDIENTE, entrada.estado());
    assertEquals(7, entrada.maxIntentos());
    EventoIncentivoMisionCumplidaV1 publicado =
        (EventoIncentivoMisionCumplidaV1) EventosSalientes.aEvento(entrada);
    assertEquals(
        new EventoIncentivoMisionCumplidaV1(persona, AHORA, "Misión 1", "Gran Donador"), publicado);
  }

  @Test
  void aEntrada_conMisionCompletadaSinInsignia_usaSinRecompensa() {
    EventoDonanteIncentivos evento =
        new MisionCompletada(UUID.randomUUID(), UUID.randomUUID(), "Carlos", "Misión", null);

    EntradaOutbox entrada = EventosSalientes.aEntrada(evento, AHORA, 3).orElseThrow();

    EventoIncentivoMisionCumplidaV1 publicado =
        (EventoIncentivoMisionCumplidaV1) EventosSalientes.aEvento(entrada);
    assertEquals("Sin recompensa", publicado.recompensa());
  }

  @Test
  void aEntrada_conAscenso_publicaLasCategoriasPorNombre() {
    UUID persona = UUID.randomUUID();
    EventoDonanteIncentivos evento =
        new AscensoDonante(
            UUID.randomUUID(), persona, CategoriaDonante.COLABORADOR, CategoriaDonante.SOSTENEDOR);

    EntradaOutbox entrada = EventosSalientes.aEntrada(evento, AHORA, 3).orElseThrow();

    assertEquals(RabbitMQConfig.ROUTING_KEY_SUBIO_CATEGORIA, entrada.routingKey());
    assertEquals(
        new EventoIncentivoSubioCategoriaV1(persona, AHORA, "SOSTENEDOR", "COLABORADOR"),
        EventosSalientes.aEvento(entrada));
  }

  @Test
  void aEntrada_conUnEventoSinAviso_devuelveVacio() {
    EventoDonanteIncentivos otro = mock(EventoDonanteIncentivos.class);

    Optional<EntradaOutbox> entrada = EventosSalientes.aEntrada(otro, AHORA, 3);

    assertTrue(entrada.isEmpty());
  }

  @Test
  void inactividad_vaYVuelveSinPerderDatos() {
    UUID persona = UUID.randomUUID();

    EntradaOutbox entrada = EventosSalientes.inactividad(persona, 45, AHORA, 3);

    assertEquals(RabbitMQConfig.ROUTING_KEY_DONANTE_INACTIVO, entrada.routingKey());
    assertEquals(
        new EventoIncentivoDonanteInactivoV1(persona, AHORA, 45),
        EventosSalientes.aEvento(entrada));
  }

  @Test
  void aEvento_conRoutingKeyDesconocida_falla() {
    EntradaOutbox entrada = EntradaOutbox.nueva("no.existe.v1", "{}", AHORA, 3);

    assertThrows(IllegalArgumentException.class, () -> EventosSalientes.aEvento(entrada));
  }
}
