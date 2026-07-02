package grupo5.notificaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import grupo5.notificaciones.dto.input.*;
import grupo5.notificaciones.models.entities.notificaciones.eventos.*;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventoMapperTest {

  private static final LocalDateTime TEST_DATE_TIME =
      LocalDateTime.of(2026, Month.JULY, 2, 12, 0, 0);

  @Mock private IPersonaRepository personaRepository;

  private EventoMapper mapper;

  private Persona donante;
  private Persona beneficiario;
  private Persona admin;

  @BeforeEach
  void setUp() {
    mapper = new EventoMapper(personaRepository);

    donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);
    admin = new Persona(UUID.randomUUID(), new ArrayList<>(), "Admin", TipoPersona.HUMANA);

    lenient().when(personaRepository.findById(donante.getId())).thenReturn(Optional.of(donante));
    lenient()
        .when(personaRepository.findById(beneficiario.getId()))
        .thenReturn(Optional.of(beneficiario));
    lenient().when(personaRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
  }

  @Test
  void toEntity_donacionAsignada_deberiaMapearDonanteYBeneficiarioCorrectamente() {
    EventoDonacionAsignadaDTO dto =
        new EventoDonacionAsignadaDTO(
            donante.getId(), TEST_DATE_TIME, beneficiario.getId(), "10kg de arroz");

    EventoNotificable evento = mapper.toEntity(dto);

    DonacionAsignada resultado = assertInstanceOf(DonacionAsignada.class, evento);
    assertEquals(donante.getId(), resultado.getPersona().getId());
    assertEquals(beneficiario.getId(), resultado.getEntidadBeneficiaria().getId());
    assertEquals("10kg de arroz", resultado.getDetalleDonacion());
  }

  @Test
  void toEntity_donacionRecibida_deberiaMapearPatenteDeCamion() {
    EventoDonacionRecibidaDTO dto =
        new EventoDonacionRecibidaDTO(
            donante.getId(), TEST_DATE_TIME, beneficiario.getId(), "ropa", "AB123CD");

    EventoNotificable evento = mapper.toEntity(dto);

    DonacionRecibida resultado = assertInstanceOf(DonacionRecibida.class, evento);
    assertEquals(donante.getId(), resultado.getPersona().getId());
    assertEquals(beneficiario.getId(), resultado.getEntidadBeneficiaria().getId());
    assertEquals("AB123CD", resultado.getPatenteCamion());
  }

  @Test
  void toEntity_donacionEnCamino_deberiaMapearEnlaceDeSeguimiento() {
    EventoDonacionEnCaminoDTO dto =
        new EventoDonacionEnCaminoDTO(
            donante.getId(),
            TEST_DATE_TIME,
            beneficiario.getId(),
            "ropa",
            "https://donatrack.app/mapa/123");

    EventoNotificable evento = mapper.toEntity(dto);

    DonacionEnCamino resultado = assertInstanceOf(DonacionEnCamino.class, evento);
    assertEquals(donante.getId(), resultado.getPersona().getId());
    assertEquals(beneficiario.getId(), resultado.getEntidadBeneficiaria().getId());
    assertEquals("https://donatrack.app/mapa/123", resultado.getEnlaceSeguimiento());
  }

  @Test
  void toEntity_entregaFallida_deberiaMapearDonanteBeneficiarioYAdminSinConfundirlos() {
    EventoEntregaFallidaDTO dto =
        new EventoEntregaFallidaDTO(
            donante.getId(),
            TEST_DATE_TIME,
            beneficiario.getId(),
            "ropa",
            admin.getId(),
            "Nadie respondió",
            true);

    EventoNotificable evento = mapper.toEntity(dto);

    EntregaFallida resultado = assertInstanceOf(EntregaFallida.class, evento);
    // Se verifica explícitamente que cada id fue a su campo correspondiente
    // y no se mezclaron beneficiario/admin en el switch del mapper.
    assertEquals(donante.getId(), resultado.getPersona().getId());
    assertEquals(beneficiario.getId(), resultado.getEntidadBeneficiaria().getId());
    assertEquals(admin.getId(), resultado.getAdministracion().getId());
    assertEquals("Nadie respondió", resultado.getMotivo());
    assertEquals(true, resultado.isReplanificable());
  }

  @Test
  void toEntity_donanteRegistrado_deberiaMapearCredenciales() {
    EventoDonanteRegistradoDTO dto =
        new EventoDonanteRegistradoDTO(donante.getId(), TEST_DATE_TIME, "usuario: Juan");

    EventoNotificable evento = mapper.toEntity(dto);

    DonanteRegistrado resultado = assertInstanceOf(DonanteRegistrado.class, evento);
    assertEquals(donante.getId(), resultado.getPersona().getId());
  }

  @Test
  void toEntity_donanteInactivo_deberiaMapearDiasInactivo() {
    EventoDonanteInactivoDTO dto =
        new EventoDonanteInactivoDTO(donante.getId(), TEST_DATE_TIME, 21);

    EventoNotificable evento = mapper.toEntity(dto);

    assertInstanceOf(DonanteInactivo.class, evento);
  }

  @Test
  void toEntity_misionCumplida_deberiaMapearNombreYRecompensa() {
    EventoMisionCumplidaDTO dto =
        new EventoMisionCumplidaDTO(donante.getId(), TEST_DATE_TIME, "Racha", "Insignia Oro");

    EventoNotificable evento = mapper.toEntity(dto);

    assertInstanceOf(MisionCumplida.class, evento);
  }

  @Test
  void toEntity_subioCategoria_deberiaMapearCategorias() {
    EventoSubioCategoriaDTO dto =
        new EventoSubioCategoriaDTO(donante.getId(), TEST_DATE_TIME, "Sostenedor", "Colaborador");

    EventoNotificable evento = mapper.toEntity(dto);

    assertInstanceOf(SubioCategoria.class, evento);
  }

  @Test
  void toEntity_personaNoEncontrada_deberiaLanzarExcepcion() {
    UUID idInexistente = UUID.randomUUID();
    when(personaRepository.findById(idInexistente)).thenReturn(Optional.empty());

    EventoDonanteInactivoDTO dto = new EventoDonanteInactivoDTO(idInexistente, TEST_DATE_TIME, 21);

    assertThrows(IllegalArgumentException.class, () -> mapper.toEntity(dto));
  }
}
